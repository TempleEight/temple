package main

import (
	"encoding/json"
	"flag"
	"fmt"
	"log"
	"net/http"
	"time"

	"golang.org/x/crypto/bcrypt"

	"github.com/squat/and/dab/auth/comm"
	"github.com/squat/and/dab/auth/dao"
	"github.com/squat/and/dab/auth/util"
	valid "github.com/asaskevich/govalidator"
	"github.com/dgrijalva/jwt-go"
	"github.com/google/uuid"
	"github.com/gorilla/mux"
	"github.com/prometheus/client_golang/prometheus/promhttp"
)

// env defines the environment that requests should be executed within
type env struct {
	dao           dao.Datastore
	hook          Hook
	comm          comm.Comm
	jwtCredential *comm.JWTCredential
}

// registerAuthRequest contains the client-provided information required to create a single auth
type registerAuthRequest struct {
	Email    string `valid:"email,required"`
	Password string `valid:"type(string),required,stringlength(8|64)"`
}

// loginAuthRequest contains the client-provided information required to login an existing auth
type loginAuthRequest struct {
	Email    string `valid:"email,required"`
	Password string `valid:"type(string),required,stringlength(8|64)"`
}

// registerAuthResponse contains an access token
type registerAuthResponse struct {
	AccessToken string
}

// loginAuthResponse contains an access token
type loginAuthResponse struct {
	AccessToken string
}

// defaultRouter generates a router for this service
func defaultRouter(env *env) *mux.Router {
	r := mux.NewRouter()
	r.HandleFunc("/auth/register", env.registerAuthHandler).Methods(http.MethodPost)
	r.HandleFunc("/auth/login", env.loginAuthHandler).Methods(http.MethodPost)
	r.Use(jsonMiddleware)
	return r
}

func main() {
	configPtr := flag.String("config", "/etc/auth-service/config.json", "configuration filepath")
	flag.Parse()

	// Require all struct fields by default
	valid.SetFieldsRequiredByDefault(true)

	config, err := util.GetConfig(*configPtr)
	if err != nil {
		log.Fatal(err)
	}

	// Prometheus metrics
	promPort, ok := config.Ports["prometheus"]
	if !ok {
		log.Fatal("A port for the key prometheus was not found")
	}
	go func() {
		http.Handle("/metrics", promhttp.Handler())
		http.ListenAndServe(fmt.Sprintf(":%d", promPort), nil)
	}()

	d, err := dao.Init(config)
	if err != nil {
		log.Fatal(err)
	}

	c := comm.Init(config)

	jwtCredential, err := c.CreateJWTCredential()
	if err != nil {
		log.Fatal(err)
	}

	env := env{d, Hook{}, c, jwtCredential}

	// Call into non-generated entry-point
	router := defaultRouter(&env)
	env.setup(router)

	log.Fatal(http.ListenAndServe(":1024", router))
}

func jsonMiddleware(next http.Handler) http.Handler {
	return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		// All responses are JSON, set header accordingly
		w.Header().Set("Content-Type", "application/json")
		next.ServeHTTP(w, r)
	})
}

func (env *env) registerAuthHandler(w http.ResponseWriter, r *http.Request) {
	var req registerAuthRequest
	err := json.NewDecoder(r.Body).Decode(&req)
	if err != nil {
		errMsg := util.CreateErrorJSON(fmt.Sprintf("Invalid request parameters: %s", err.Error()))
		http.Error(w, errMsg, http.StatusBadRequest)
		return
	}

	_, err = valid.ValidateStruct(req)
	if err != nil {
		errMsg := util.CreateErrorJSON(fmt.Sprintf("Invalid request parameters: %s", err.Error()))
		http.Error(w, errMsg, http.StatusBadRequest)
		return
	}

	// Hash and salt the password before storing
	hashedPassword, err := bcrypt.GenerateFromPassword([]byte(req.Password), bcrypt.DefaultCost)
	if err != nil {
		errMsg := util.CreateErrorJSON(fmt.Sprintf("Could not hash password: %s", err.Error()))
		http.Error(w, errMsg, http.StatusInternalServerError)
		return
	}

	uuid, err := uuid.NewUUID()
	if err != nil {
		errMsg := util.CreateErrorJSON(fmt.Sprintf("Could not create UUID: %s", err.Error()))
		http.Error(w, errMsg, http.StatusInternalServerError)
		return
	}

	input := dao.CreateAuthInput{
		ID:       uuid,
		Email:    req.Email,
		Password: string(hashedPassword),
	}

	for _, hook := range env.hook.beforeRegisterHooks {
		err := (*hook)(env, req, &input)
		if err != nil {
			// TODO
			return
		}
	}

	auth, err := env.dao.CreateAuth(input)
	if err != nil {
		switch err {
		case dao.ErrDuplicateAuth:
			http.Error(w, util.CreateErrorJSON(err.Error()), http.StatusForbidden)
		default:
			errMsg := util.CreateErrorJSON(fmt.Sprintf("Something went wrong: %s", err.Error()))
			http.Error(w, errMsg, http.StatusInternalServerError)
		}
		return
	}

	accessToken, err := createToken(auth.ID, env.jwtCredential.Key, env.jwtCredential.Secret)
	if err != nil {
		errMsg := util.CreateErrorJSON(fmt.Sprintf("Could not create access token: %s", err.Error()))
		http.Error(w, errMsg, http.StatusInternalServerError)
		return
	}

	json.NewEncoder(w).Encode(registerAuthResponse{
		AccessToken: accessToken,
	})
}

func (env *env) loginAuthHandler(w http.ResponseWriter, r *http.Request) {
	var req loginAuthRequest
	err := json.NewDecoder(r.Body).Decode(&req)
	if err != nil {
		errMsg := util.CreateErrorJSON(fmt.Sprintf("Invalid request parameters: %s", err.Error()))
		http.Error(w, errMsg, http.StatusBadRequest)
		return
	}

	_, err = valid.ValidateStruct(req)
	if err != nil {
		errMsg := util.CreateErrorJSON(fmt.Sprintf("Invalid request parameters: %s", err.Error()))
		http.Error(w, errMsg, http.StatusBadRequest)
		return
	}

	input := dao.ReadAuthInput{
		Email: req.Email,
	}

	for _, hook := range env.hook.beforeLoginHooks {
		err := (*hook)(env, req, &input)
		if err != nil {
			// TODO
			return
		}
	}

	auth, err := env.dao.ReadAuth(input)
	if err != nil {
		switch err {
		case dao.ErrAuthNotFound:
			errMsg := util.CreateErrorJSON(fmt.Sprintf("Invalid email or password"))
			http.Error(w, errMsg, http.StatusUnauthorized)
		default:
			errMsg := util.CreateErrorJSON(fmt.Sprintf("Something went wrong: %s", err.Error()))
			http.Error(w, errMsg, http.StatusInternalServerError)
		}
		return
	}

	err = bcrypt.CompareHashAndPassword([]byte(auth.Password), []byte(req.Password))
	if err != nil {
		errMsg := util.CreateErrorJSON(fmt.Sprintf("Invalid email or password"))
		http.Error(w, errMsg, http.StatusUnauthorized)
		return
	}

	accessToken, err := createToken(auth.ID, env.jwtCredential.Key, env.jwtCredential.Secret)
	if err != nil {
		errMsg := util.CreateErrorJSON(fmt.Sprintf("Could not create access token: %s", err.Error()))
		http.Error(w, errMsg, http.StatusInternalServerError)
		return
	}

	json.NewEncoder(w).Encode(loginAuthResponse{
		AccessToken: accessToken,
	})
}

// Create an access token with a 24 hour lifetime
func createToken(id uuid.UUID, issuer string, secret string) (string, error) {
	token := jwt.NewWithClaims(jwt.SigningMethodHS256, jwt.MapClaims{
		"id":  id.String(),
		"iss": issuer,
		"exp": time.Now().Add(24 * time.Hour).Unix(),
	})

	return token.SignedString([]byte(secret))
}
