package main

import (
	"encoding/json"
	"flag"
	"fmt"
	"log"
	"net/http"
	"strconv"
	"time"

	"golang.org/x/crypto/bcrypt"

	"github.com/TempleEight/spec-golang/auth/comm"
	"github.com/TempleEight/spec-golang/auth/dao"
	"github.com/TempleEight/spec-golang/auth/metric"
	"github.com/TempleEight/spec-golang/auth/util"
	valid "github.com/go-playground/validator/v10"
	"github.com/dgrijalva/jwt-go"
	"github.com/google/uuid"
	"github.com/gorilla/mux"
	"github.com/prometheus/client_golang/prometheus"
	"github.com/prometheus/client_golang/prometheus/promhttp"
)

// env defines the environment that requests should be executed within
type env struct {
	dao           dao.Datastore
	hook          Hook
	valid	      *valid.Validate
	comm          comm.Comm
	jwtCredential *comm.JWTCredential
}

// registerAuthRequest contains the client-provided information required to create a single auth
type registerAuthRequest struct {
	Email    string `validate:"email,required"`
	Password string `validate:"required,gte=8,lte=64"`
}

// loginAuthRequest contains the client-provided information required to login an existing auth
type loginAuthRequest struct {
	Email    string `validate:"email,required"`
	Password string `validate:"required,gte=8,lte=64"`
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

	env := env{d, Hook{}, valid.New(), c, jwtCredential}

	// Call into non-generated entry-point
	router := defaultRouter(&env)
	env.setup(router)

	log.Fatal(http.ListenAndServe(":82", router))
}

func jsonMiddleware(next http.Handler) http.Handler {
	return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		// All responses are JSON, set header accordingly
		w.Header().Set("Content-Type", "application/json")
		next.ServeHTTP(w, r)
	})
}

// respondWithError responds to a HTTP request with a JSON error response
func respondWithError(w http.ResponseWriter, err string, statusCode int, requestType string) {
	w.WriteHeader(statusCode)
	fmt.Fprintln(w, util.CreateErrorJSON(err))
	metric.RequestFailure.WithLabelValues(requestType, strconv.Itoa(statusCode)).Inc()
}

func (env *env) registerAuthHandler(w http.ResponseWriter, r *http.Request) {
	var req registerAuthRequest
	err := json.NewDecoder(r.Body).Decode(&req)
	if err != nil {
		respondWithError(w, fmt.Sprintf("Invalid request parameters: %s", err.Error()), http.StatusBadRequest, metric.RequestRegister)
		return
	}

	err = env.valid.Struct(req)
	if err != nil {
		respondWithError(w, fmt.Sprintf("Invalid request parameters: %s", err.Error()), http.StatusBadRequest, metric.RequestRegister)
		return
	}

	// Hash and salt the password before storing
	hashedPassword, err := bcrypt.GenerateFromPassword([]byte(req.Password), bcrypt.DefaultCost)
	if err != nil {
		respondWithError(w, fmt.Sprintf("Could not hash password: %s", err.Error()), http.StatusInternalServerError, metric.RequestRegister)
		return
	}

	uuid, err := uuid.NewUUID()
	if err != nil {
		respondWithError(w, fmt.Sprintf("Could not create UUID: %s", err.Error()), http.StatusInternalServerError, metric.RequestRegister)
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
			respondWithError(w, err.Error(), err.statusCode, metric.RequestRegister)
			return
		}
	}

	timer := prometheus.NewTimer(metric.DatabaseRequestDuration.WithLabelValues(metric.RequestRegister))
	auth, err := env.dao.CreateAuth(input)
	timer.ObserveDuration()
	if err != nil {
		switch err {
		case dao.ErrDuplicateAuth:
			respondWithError(w, err.Error(), http.StatusForbidden, metric.RequestRegister)
		default:
			respondWithError(w, fmt.Sprintf("Something went wrong: %s", err.Error()), http.StatusInternalServerError, metric.RequestRegister)
		}
		return
	}

	accessToken, err := createToken(auth.ID, env.jwtCredential.Key, env.jwtCredential.Secret)
	if err != nil {
		respondWithError(w, fmt.Sprintf("Could not create access token: %s", err.Error()), http.StatusInternalServerError, metric.RequestRegister)
		return
	}

	for _, hook := range env.hook.afterRegisterHooks {
		err := (*hook)(env, auth, accessToken)
		if err != nil {
			respondWithError(w, err.Error(), err.statusCode, metric.RequestRegister)
			return
		}
	}

	json.NewEncoder(w).Encode(registerAuthResponse{
		AccessToken: accessToken,
	})

	metric.RequestSuccess.WithLabelValues(metric.RequestRegister).Inc()
}

func (env *env) loginAuthHandler(w http.ResponseWriter, r *http.Request) {
	var req loginAuthRequest
	err := json.NewDecoder(r.Body).Decode(&req)
	if err != nil {
		respondWithError(w, fmt.Sprintf("Invalid request parameters: %s", err.Error()), http.StatusBadRequest, metric.RequestLogin)
		return
	}

	err = env.valid.Struct(req)
	if err != nil {
		respondWithError(w, fmt.Sprintf("Invalid request parameters: %s", err.Error()), http.StatusBadRequest, metric.RequestLogin)
		return
	}

	input := dao.ReadAuthInput{
		Email: req.Email,
	}

	for _, hook := range env.hook.beforeLoginHooks {
		err := (*hook)(env, req, &input)
		if err != nil {
			respondWithError(w, err.Error(), err.statusCode, metric.RequestLogin)
			return
		}
	}

	timer := prometheus.NewTimer(metric.DatabaseRequestDuration.WithLabelValues(metric.RequestLogin))
	auth, err := env.dao.ReadAuth(input)
	timer.ObserveDuration()
	if err != nil {
		switch err {
		case dao.ErrAuthNotFound:
			respondWithError(w, fmt.Sprintf("Invalid email or password"), http.StatusUnauthorized, metric.RequestLogin)
		default:
			respondWithError(w, fmt.Sprintf("Something went wrong: %s", err.Error()), http.StatusInternalServerError, metric.RequestLogin)
		}
		return
	}

	err = bcrypt.CompareHashAndPassword([]byte(auth.Password), []byte(req.Password))
	if err != nil {
		respondWithError(w, fmt.Sprintf("Invalid email or password"), http.StatusUnauthorized, metric.RequestLogin)
		return
	}

	accessToken, err := createToken(auth.ID, env.jwtCredential.Key, env.jwtCredential.Secret)
	if err != nil {
		respondWithError(w, fmt.Sprintf("Could not create access token: %s", err.Error()), http.StatusInternalServerError, metric.RequestLogin)
		return
	}

	for _, hook := range env.hook.afterLoginHooks {
		err := (*hook)(env, auth, accessToken)
		if err != nil {
			respondWithError(w, err.Error(), err.statusCode, metric.RequestLogin)
			return
		}
	}

	json.NewEncoder(w).Encode(loginAuthResponse{
		AccessToken: accessToken,
	})

	metric.RequestSuccess.WithLabelValues(metric.RequestLogin).Inc()
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
