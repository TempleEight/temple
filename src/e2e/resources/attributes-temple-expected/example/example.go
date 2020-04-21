package main

import (
	"encoding/json"
	"flag"
	"fmt"
	"log"
	"net/http"

	"github.com/squat/and/dab/example/dao"
	"github.com/squat/and/dab/example/util"
	valid "github.com/go-playground/validator/v10"
	"github.com/google/uuid"
	"github.com/gorilla/mux"
)

// env defines the environment that requests should be executed within
type env struct {
	dao   dao.Datastore
	hook  Hook
	valid *valid.Validate
}

// createExampleRequest contains the client-provided information required to create a single example
type createExampleRequest struct {
	ClientAttribute *string `json:"clientAttribute" validate:"required"`
}

// updateExampleRequest contains the client-provided information required to update a single example
type updateExampleRequest struct {
	ClientAttribute *string `json:"clientAttribute" validate:"required"`
}

// createExampleResponse contains a newly created example to be returned to the client
type createExampleResponse struct {
	ID                 uuid.UUID `json:"id"`
	ServerSetAttribute bool      `json:"serverSetAttribute"`
}

// readExampleResponse contains a single example to be returned to the client
type readExampleResponse struct {
	ID                 uuid.UUID `json:"id"`
	ServerSetAttribute bool      `json:"serverSetAttribute"`
}

// updateExampleResponse contains a newly updated example to be returned to the client
type updateExampleResponse struct {
	ID                 uuid.UUID `json:"id"`
	ServerSetAttribute bool      `json:"serverSetAttribute"`
}

// defaultRouter generates a router for this service
func defaultRouter(env *env) *mux.Router {
	r := mux.NewRouter()
	// Mux directs to first matching route, i.e. the order matters
	r.HandleFunc("/example", env.createExampleHandler).Methods(http.MethodPost)
	r.HandleFunc("/example/{id}", env.readExampleHandler).Methods(http.MethodGet)
	r.HandleFunc("/example/{id}", env.updateExampleHandler).Methods(http.MethodPut)
	r.HandleFunc("/example/{id}", env.deleteExampleHandler).Methods(http.MethodDelete)
	r.Use(jsonMiddleware)
	return r
}

func main() {
	configPtr := flag.String("config", "/etc/example-service/config.json", "configuration filepath")
	flag.Parse()

	config, err := util.GetConfig(*configPtr)
	if err != nil {
		log.Fatal(err)
	}

	d, err := dao.Init(config)
	if err != nil {
		log.Fatal(err)
	}

	env := env{d, Hook{}, valid.New()}

	// Call into non-generated entry-point
	router := defaultRouter(&env)
	env.setup(router)

	log.Fatal(http.ListenAndServe(":1026", router))
}

func jsonMiddleware(next http.Handler) http.Handler {
	return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		// All responses are JSON, set header accordingly
		w.Header().Set("Content-Type", "application/json")
		next.ServeHTTP(w, r)
	})
}

// respondWithError responds to a HTTP request with a JSON error response
func respondWithError(w http.ResponseWriter, err string, statusCode int) {
	w.WriteHeader(statusCode)
	fmt.Fprintln(w, util.CreateErrorJSON(err))
}

func (env *env) createExampleHandler(w http.ResponseWriter, r *http.Request) {
	var req createExampleRequest
	err := json.NewDecoder(r.Body).Decode(&req)
	if err != nil {
		respondWithError(w, fmt.Sprintf("Invalid request parameters: %s", err.Error()), http.StatusBadRequest)
		return
	}

	if req.ClientAttribute == nil {
		respondWithError(w, "Missing request parameter(s)", http.StatusBadRequest)
		return
	}

	err = env.valid.Struct(req)
	if err != nil {
		respondWithError(w, fmt.Sprintf("Invalid request parameters: %s", err.Error()), http.StatusBadRequest)
		return
	}

	uuid, err := uuid.NewUUID()
	if err != nil {
		respondWithError(w, fmt.Sprintf("Could not create UUID: %s", err.Error()), http.StatusInternalServerError)
		return
	}

	input := dao.CreateExampleInput{
		ID: uuid,
	}

	for _, hook := range env.hook.beforeCreateExampleHooks {
		err := (*hook)(env, req, &input)
		if err != nil {
			respondWithError(w, err.Error(), err.statusCode)
			return
		}
	}

	example, err := env.dao.CreateExample(input)
	if err != nil {
		respondWithError(w, fmt.Sprintf("Something went wrong: %s", err.Error()), http.StatusInternalServerError)
		return
	}

	for _, hook := range env.hook.afterCreateExampleHooks {
		err := (*hook)(env, example)
		if err != nil {
			respondWithError(w, err.Error(), err.statusCode)
			return
		}
	}

	json.NewEncoder(w).Encode(createExampleResponse{
		ID:                 example.ID,
		ServerSetAttribute: example.ServerSetAttribute,
	})
}

func (env *env) readExampleHandler(w http.ResponseWriter, r *http.Request) {
	exampleID, err := util.ExtractIDFromRequest(mux.Vars(r))
	if err != nil {
		respondWithError(w, err.Error(), http.StatusBadRequest)
		return
	}

	input := dao.ReadExampleInput{
		ID: exampleID,
	}

	for _, hook := range env.hook.beforeReadExampleHooks {
		err := (*hook)(env, &input)
		if err != nil {
			respondWithError(w, err.Error(), err.statusCode)
			return
		}
	}

	example, err := env.dao.ReadExample(input)
	if err != nil {
		switch err.(type) {
		case dao.ErrExampleNotFound:
			respondWithError(w, err.Error(), http.StatusNotFound)
		default:
			respondWithError(w, fmt.Sprintf("Something went wrong: %s", err.Error()), http.StatusInternalServerError)
		}
		return
	}

	for _, hook := range env.hook.afterReadExampleHooks {
		err := (*hook)(env, example)
		if err != nil {
			respondWithError(w, err.Error(), err.statusCode)
			return
		}
	}

	json.NewEncoder(w).Encode(readExampleResponse{
		ID:                 example.ID,
		ServerSetAttribute: example.ServerSetAttribute,
	})
}

func (env *env) updateExampleHandler(w http.ResponseWriter, r *http.Request) {
	exampleID, err := util.ExtractIDFromRequest(mux.Vars(r))
	if err != nil {
		respondWithError(w, err.Error(), http.StatusBadRequest)
		return
	}

	var req updateExampleRequest
	err = json.NewDecoder(r.Body).Decode(&req)
	if err != nil {
		respondWithError(w, fmt.Sprintf("Invalid request parameters: %s", err.Error()), http.StatusBadRequest)
		return
	}

	if req.ClientAttribute == nil {
		respondWithError(w, "Missing request parameter(s)", http.StatusBadRequest)
		return
	}

	err = env.valid.Struct(req)
	if err != nil {
		respondWithError(w, fmt.Sprintf("Invalid request parameters: %s", err.Error()), http.StatusBadRequest)
		return
	}

	input := dao.UpdateExampleInput{
		ID: exampleID,
	}

	for _, hook := range env.hook.beforeUpdateExampleHooks {
		err := (*hook)(env, req, &input)
		if err != nil {
			respondWithError(w, err.Error(), err.statusCode)
			return
		}
	}

	example, err := env.dao.UpdateExample(input)
	if err != nil {
		switch err.(type) {
		case dao.ErrExampleNotFound:
			respondWithError(w, err.Error(), http.StatusNotFound)
		default:
			respondWithError(w, fmt.Sprintf("Something went wrong: %s", err.Error()), http.StatusInternalServerError)
		}
		return
	}

	for _, hook := range env.hook.afterUpdateExampleHooks {
		err := (*hook)(env, example)
		if err != nil {
			respondWithError(w, err.Error(), err.statusCode)
			return
		}
	}

	json.NewEncoder(w).Encode(updateExampleResponse{
		ID:                 example.ID,
		ServerSetAttribute: example.ServerSetAttribute,
	})
}

func (env *env) deleteExampleHandler(w http.ResponseWriter, r *http.Request) {
	exampleID, err := util.ExtractIDFromRequest(mux.Vars(r))
	if err != nil {
		respondWithError(w, err.Error(), http.StatusBadRequest)
		return
	}

	input := dao.DeleteExampleInput{
		ID: exampleID,
	}

	for _, hook := range env.hook.beforeDeleteExampleHooks {
		err := (*hook)(env, &input)
		if err != nil {
			respondWithError(w, err.Error(), err.statusCode)
			return
		}
	}

	err = env.dao.DeleteExample(input)
	if err != nil {
		switch err.(type) {
		case dao.ErrExampleNotFound:
			respondWithError(w, err.Error(), http.StatusNotFound)
		default:
			respondWithError(w, fmt.Sprintf("Something went wrong: %s", err.Error()), http.StatusInternalServerError)
		}
		return
	}

	for _, hook := range env.hook.afterDeleteExampleHooks {
		err := (*hook)(env)
		if err != nil {
			respondWithError(w, err.Error(), err.statusCode)
			return
		}
	}

	json.NewEncoder(w).Encode(struct{}{})
}
