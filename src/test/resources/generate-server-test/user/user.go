package main

import (
	"encoding/json"
	"flag"
	"fmt"
	"log"
	"net/http"
	"strconv"

	"github.com/TempleEight/spec-golang/user/dao"
	"github.com/TempleEight/spec-golang/user/metric"
	"github.com/TempleEight/spec-golang/user/util"
	valid "github.com/go-playground/validator/v10"
	"github.com/google/uuid"
	"github.com/gorilla/mux"
	"github.com/prometheus/client_golang/prometheus"
	"github.com/prometheus/client_golang/prometheus/promhttp"
)

// env defines the environment that requests should be executed within
type env struct {
	dao   dao.Datastore
	hook  Hook
	valid *valid.Validate
}

// createUserRequest contains the client-provided information required to create a single user
type createUserRequest struct {
	Name *string `json:"name" validate:"required,gte=2,lte=255"`
}

// updateUserRequest contains the client-provided information required to update a single user
type updateUserRequest struct {
	Name *string `json:"name" validate:"required,gte=2,lte=255"`
}

// createUserResponse contains a newly created user to be returned to the client
type createUserResponse struct {
	ID   uuid.UUID `json:"id"`
	Name string    `json:"name"`
}

// readUserResponse contains a single user to be returned to the client
type readUserResponse struct {
	ID   uuid.UUID `json:"id"`
	Name string    `json:"name"`
}

// updateUserResponse contains a newly updated user to be returned to the client
type updateUserResponse struct {
	ID   uuid.UUID `json:"id"`
	Name string    `json:"name"`
}

// defaultRouter generates a router for this service
func defaultRouter(env *env) *mux.Router {
	r := mux.NewRouter()
	// Mux directs to first matching route, i.e. the order matters
	r.HandleFunc("/user", env.createUserHandler).Methods(http.MethodPost)
	r.HandleFunc("/user/{id}", env.readUserHandler).Methods(http.MethodGet)
	r.HandleFunc("/user/{id}", env.updateUserHandler).Methods(http.MethodPut)
	r.HandleFunc("/user/{id}", env.deleteUserHandler).Methods(http.MethodDelete)
	r.Use(jsonMiddleware)
	return r
}

func main() {
	configPtr := flag.String("config", "/etc/user-service/config.json", "configuration filepath")
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

	env := env{d, Hook{}, valid.New()}

	// Call into non-generated entry-point
	router := defaultRouter(&env)
	env.setup(router)

	log.Fatal(http.ListenAndServe(":80", router))
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

func (env *env) createUserHandler(w http.ResponseWriter, r *http.Request) {
	auth, err := util.ExtractAuthIDFromRequest(r.Header)
	if err != nil {
		respondWithError(w, fmt.Sprintf("Could not authorize request: %s", err.Error()), http.StatusUnauthorized, metric.RequestCreateUser)
		return
	}

	var req createUserRequest
	err = json.NewDecoder(r.Body).Decode(&req)
	if err != nil {
		respondWithError(w, fmt.Sprintf("Invalid request parameters: %s", err.Error()), http.StatusBadRequest, metric.RequestCreateUser)
		return
	}

	if req.Name == nil {
		respondWithError(w, "Missing request parameter(s)", http.StatusBadRequest, metric.RequestCreateUser)
		return
	}

	err = env.valid.Struct(req)
	if err != nil {
		respondWithError(w, fmt.Sprintf("Invalid request parameters: %s", err.Error()), http.StatusBadRequest, metric.RequestCreateUser)
		return
	}

	input := dao.CreateUserInput{
		ID:   auth.ID,
		Name: *req.Name,
	}

	for _, hook := range env.hook.beforeCreateUserHooks {
		err := (*hook)(env, req, &input, auth)
		if err != nil {
			respondWithError(w, err.Error(), err.statusCode, metric.RequestCreateUser)
			return
		}
	}

	timer := prometheus.NewTimer(metric.DatabaseRequestDuration.WithLabelValues(metric.RequestCreateUser))
	user, err := env.dao.CreateUser(input)
	timer.ObserveDuration()
	if err != nil {
		respondWithError(w, fmt.Sprintf("Something went wrong: %s", err.Error()), http.StatusInternalServerError, metric.RequestCreateUser)
		return
	}

	for _, hook := range env.hook.afterCreateUserHooks {
		err := (*hook)(env, user, auth)
		if err != nil {
			respondWithError(w, err.Error(), err.statusCode, metric.RequestCreateUser)
			return
		}
	}

	json.NewEncoder(w).Encode(createUserResponse{
		ID:   user.ID,
		Name: user.Name,
	})

	metric.RequestSuccess.WithLabelValues(metric.RequestCreateUser).Inc()
}

func (env *env) readUserHandler(w http.ResponseWriter, r *http.Request) {
	auth, err := util.ExtractAuthIDFromRequest(r.Header)
	if err != nil {
		respondWithError(w, fmt.Sprintf("Could not authorize request: %s", err.Error()), http.StatusUnauthorized, metric.RequestReadUser)
		return
	}

	userID, err := util.ExtractIDFromRequest(mux.Vars(r))
	if err != nil {
		respondWithError(w, err.Error(), http.StatusBadRequest, metric.RequestReadUser)
		return
	}

	input := dao.ReadUserInput{
		ID: userID,
	}

	for _, hook := range env.hook.beforeReadUserHooks {
		err := (*hook)(env, &input, auth)
		if err != nil {
			respondWithError(w, err.Error(), err.statusCode, metric.RequestReadUser)
			return
		}
	}

	timer := prometheus.NewTimer(metric.DatabaseRequestDuration.WithLabelValues(metric.RequestReadUser))
	user, err := env.dao.ReadUser(input)
	timer.ObserveDuration()
	if err != nil {
		switch err.(type) {
		case dao.ErrUserNotFound:
			respondWithError(w, err.Error(), http.StatusNotFound, metric.RequestReadUser)
		default:
			respondWithError(w, fmt.Sprintf("Something went wrong: %s", err.Error()), http.StatusInternalServerError, metric.RequestReadUser)
		}
		return
	}

	for _, hook := range env.hook.afterReadUserHooks {
		err := (*hook)(env, user, auth)
		if err != nil {
			respondWithError(w, err.Error(), err.statusCode, metric.RequestReadUser)
			return
		}
	}

	json.NewEncoder(w).Encode(readUserResponse{
		ID:   user.ID,
		Name: user.Name,
	})

	metric.RequestSuccess.WithLabelValues(metric.RequestReadUser).Inc()
}

func (env *env) updateUserHandler(w http.ResponseWriter, r *http.Request) {
	auth, err := util.ExtractAuthIDFromRequest(r.Header)
	if err != nil {
		respondWithError(w, fmt.Sprintf("Could not authorize request: %s", err.Error()), http.StatusUnauthorized, metric.RequestUpdateUser)
		return
	}

	userID, err := util.ExtractIDFromRequest(mux.Vars(r))
	if err != nil {
		respondWithError(w, err.Error(), http.StatusBadRequest, metric.RequestUpdateUser)
		return
	}

	if auth.ID != userID {
		respondWithError(w, "Unauthorized", http.StatusUnauthorized, metric.RequestUpdateUser)
		return
	}

	var req updateUserRequest
	err = json.NewDecoder(r.Body).Decode(&req)
	if err != nil {
		respondWithError(w, fmt.Sprintf("Invalid request parameters: %s", err.Error()), http.StatusBadRequest, metric.RequestUpdateUser)
		return
	}

	if req.Name == nil {
		respondWithError(w, "Missing request parameter(s)", http.StatusBadRequest, metric.RequestUpdateUser)
		return
	}

	err = env.valid.Struct(req)
	if err != nil {
		respondWithError(w, fmt.Sprintf("Invalid request parameters: %s", err.Error()), http.StatusBadRequest, metric.RequestUpdateUser)
		return
	}

	input := dao.UpdateUserInput{
		ID:   userID,
		Name: *req.Name,
	}

	for _, hook := range env.hook.beforeUpdateUserHooks {
		err := (*hook)(env, req, &input, auth)
		if err != nil {
			respondWithError(w, err.Error(), err.statusCode, metric.RequestUpdateUser)
			return
		}
	}

	timer := prometheus.NewTimer(metric.DatabaseRequestDuration.WithLabelValues(metric.RequestUpdateUser))
	user, err := env.dao.UpdateUser(input)
	timer.ObserveDuration()
	if err != nil {
		switch err.(type) {
		case dao.ErrUserNotFound:
			respondWithError(w, err.Error(), http.StatusNotFound, metric.RequestUpdateUser)
		default:
			respondWithError(w, fmt.Sprintf("Something went wrong: %s", err.Error()), http.StatusInternalServerError, metric.RequestUpdateUser)
		}
		return
	}

	for _, hook := range env.hook.afterUpdateUserHooks {
		err := (*hook)(env, user, auth)
		if err != nil {
			respondWithError(w, err.Error(), err.statusCode, metric.RequestUpdateUser)
			return
		}
	}

	json.NewEncoder(w).Encode(updateUserResponse{
		ID:   user.ID,
		Name: user.Name,
	})

	metric.RequestSuccess.WithLabelValues(metric.RequestUpdateUser).Inc()
}

func (env *env) deleteUserHandler(w http.ResponseWriter, r *http.Request) {
	auth, err := util.ExtractAuthIDFromRequest(r.Header)
	if err != nil {
		respondWithError(w, fmt.Sprintf("Could not authorize request: %s", err.Error()), http.StatusUnauthorized, metric.RequestDeleteUser)
		return
	}

	userID, err := util.ExtractIDFromRequest(mux.Vars(r))
	if err != nil {
		respondWithError(w, err.Error(), http.StatusBadRequest, metric.RequestDeleteUser)
		return
	}

	if auth.ID != userID {
		respondWithError(w, "Unauthorized", http.StatusUnauthorized, metric.RequestDeleteUser)
		return
	}

	input := dao.DeleteUserInput{
		ID: userID,
	}

	for _, hook := range env.hook.beforeDeleteUserHooks {
		err := (*hook)(env, &input, auth)
		if err != nil {
			respondWithError(w, err.Error(), err.statusCode, metric.RequestDeleteUser)
			return
		}
	}

	timer := prometheus.NewTimer(metric.DatabaseRequestDuration.WithLabelValues(metric.RequestDeleteUser))
	err = env.dao.DeleteUser(input)
	timer.ObserveDuration()
	if err != nil {
		switch err.(type) {
		case dao.ErrUserNotFound:
			respondWithError(w, err.Error(), http.StatusNotFound, metric.RequestDeleteUser)
		default:
			respondWithError(w, fmt.Sprintf("Something went wrong: %s", err.Error()), http.StatusInternalServerError, metric.RequestDeleteUser)
		}
		return
	}

	for _, hook := range env.hook.afterDeleteUserHooks {
		err := (*hook)(env, auth)
		if err != nil {
			respondWithError(w, err.Error(), err.statusCode, metric.RequestDeleteUser)
			return
		}
	}

	json.NewEncoder(w).Encode(struct{}{})

	metric.RequestSuccess.WithLabelValues(metric.RequestDeleteUser).Inc()
}
