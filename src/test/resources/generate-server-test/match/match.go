package main

import (
	"encoding/json"
	"flag"
	"fmt"
	"log"
	"net/http"
	"strconv"
	"time"

	"github.com/TempleEight/spec-golang/match/comm"
	"github.com/TempleEight/spec-golang/match/dao"
	"github.com/TempleEight/spec-golang/match/metric"
	"github.com/TempleEight/spec-golang/match/util"
	valid "github.com/asaskevich/govalidator"
	"github.com/google/uuid"
	"github.com/gorilla/mux"
	"github.com/prometheus/client_golang/prometheus"
	"github.com/prometheus/client_golang/prometheus/promhttp"
)

// env defines the environment that requests should be executed within
type env struct {
	dao  dao.Datastore
	hook Hook
	comm comm.Comm
}

// createMatchRequest contains the client-provided information required to create a single match
type createMatchRequest struct {
	UserOne *uuid.UUID `json:"userOne" valid:"required"`
	UserTwo *uuid.UUID `json:"userTwo" valid:"required"`
}

// updateMatchRequest contains the client-provided information required to update a single match
type updateMatchRequest struct {
	UserOne *uuid.UUID `json:"userOne" valid:"required"`
	UserTwo *uuid.UUID `json:"userTwo" valid:"required"`
}

// listMatchElement contains a single match list element
type listMatchElement struct {
	ID        uuid.UUID `json:"id"`
	UserOne   uuid.UUID `json:"userOne"`
	UserTwo   uuid.UUID `json:"userTwo"`
	MatchedOn string    `json:"matchedOn"`
}

// listMatchResponse contains a single match list to be returned to the client
type listMatchResponse struct {
	MatchList []listMatchElement
}

// createMatchResponse contains a newly created match to be returned to the client
type createMatchResponse struct {
	ID        uuid.UUID `json:"id"`
	UserOne   uuid.UUID `json:"userOne"`
	UserTwo   uuid.UUID `json:"userTwo"`
	MatchedOn string    `json:"matchedOn"`
}

// readMatchResponse contains a single match to be returned to the client
type readMatchResponse struct {
	ID        uuid.UUID `json:"id"`
	UserOne   uuid.UUID `json:"userOne"`
	UserTwo   uuid.UUID `json:"userTwo"`
	MatchedOn string    `json:"matchedOn"`
}

// updateMatchResponse contains a newly updated match to be returned to the client
type updateMatchResponse struct {
	ID        uuid.UUID `json:"id"`
	UserOne   uuid.UUID `json:"userOne"`
	UserTwo   uuid.UUID `json:"userTwo"`
	MatchedOn string    `json:"matchedOn"`
}

// defaultRouter generates a router for this service
func defaultRouter(env *env) *mux.Router {
	r := mux.NewRouter()
	// Mux directs to first matching route, i.e. the order matters
	r.HandleFunc("/match/all", env.listMatchHandler).Methods(http.MethodGet)
	r.HandleFunc("/match", env.createMatchHandler).Methods(http.MethodPost)
	r.HandleFunc("/match/{id}", env.readMatchHandler).Methods(http.MethodGet)
	r.HandleFunc("/match/{id}", env.updateMatchHandler).Methods(http.MethodPut)
	r.HandleFunc("/match/{id}", env.deleteMatchHandler).Methods(http.MethodDelete)
	r.Use(jsonMiddleware)
	return r
}

func main() {
	configPtr := flag.String("config", "/etc/match-service/config.json", "configuration filepath")
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

	env := env{d, Hook{}, c}

	// Call into non-generated entry-point
	router := defaultRouter(&env)
	env.setup(router)

	log.Fatal(http.ListenAndServe(":81", router))
}

func jsonMiddleware(next http.Handler) http.Handler {
	return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		// All responses are JSON, set header accordingly
		w.Header().Set("Content-Type", "application/json")
		next.ServeHTTP(w, r)
	})
}

func checkAuthorization(env *env, matchID uuid.UUID, auth *util.Auth) (bool, error) {
	input := dao.ReadMatchInput{
		ID: matchID,
	}
	match, err := env.dao.ReadMatch(input)
	if err != nil {
		return false, err
	}
	return match.CreatedBy == auth.ID, nil
}

// respondWithError responds to a HTTP request with a JSON error response
func respondWithError(w http.ResponseWriter, err string, statusCode int, requestType string) {
	w.WriteHeader(statusCode)
	fmt.Fprintln(w, util.CreateErrorJSON(err))
	metric.RequestFailure.WithLabelValues(requestType, strconv.Itoa(statusCode)).Inc()
}

func (env *env) listMatchHandler(w http.ResponseWriter, r *http.Request) {
	auth, err := util.ExtractAuthIDFromRequest(r.Header)
	if err != nil {
		respondWithError(w, fmt.Sprintf("Could not authorize request: %s", err.Error()), http.StatusUnauthorized, metric.RequestListMatch)
		return
	}

	input := dao.ListMatchInput{
		AuthID: auth.ID,
	}

	for _, hook := range env.hook.beforeListMatchHooks {
		err := (*hook)(env, &input, auth)
		if err != nil {
			respondWithError(w, err.Error(), err.statusCode, metric.RequestListMatch)
			return
		}
	}

	timer := prometheus.NewTimer(metric.DatabaseRequestDuration.WithLabelValues(metric.RequestListMatch))
	matchList, err := env.dao.ListMatch(input)
	timer.ObserveDuration()
	if err != nil {
		respondWithError(w, fmt.Sprintf("Something went wrong: %s", err.Error()), http.StatusInternalServerError, metric.RequestListMatch)
		return
	}

	for _, hook := range env.hook.afterListMatchHooks {
		err := (*hook)(env, matchList, auth)
		if err != nil {
			respondWithError(w, err.Error(), err.statusCode, metric.RequestListMatch)
			return
		}
	}

	matchListResp := listMatchResponse{
		MatchList: make([]listMatchElement, 0),
	}

	for _, match := range *matchList {
		matchListResp.MatchList = append(matchListResp.MatchList, listMatchElement{
			ID:        match.ID,
			UserOne:   match.UserOne,
			UserTwo:   match.UserTwo,
			MatchedOn: match.MatchedOn.Format(time.RFC3339),
		})
	}

	json.NewEncoder(w).Encode(matchListResp)

	metric.RequestSuccess.WithLabelValues(metric.RequestListMatch).Inc()
}

func (env *env) createMatchHandler(w http.ResponseWriter, r *http.Request) {
	auth, err := util.ExtractAuthIDFromRequest(r.Header)
	if err != nil {
		respondWithError(w, fmt.Sprintf("Could not authorize request: %s", err.Error()), http.StatusUnauthorized, metric.RequestCreateMatch)
		return
	}

	var req createMatchRequest
	err = json.NewDecoder(r.Body).Decode(&req)
	if err != nil {
		respondWithError(w, fmt.Sprintf("Invalid request parameters: %s", err.Error()), http.StatusBadRequest, metric.RequestCreateMatch)
		return
	}

	if req.UserOne == nil || req.UserTwo == nil {
		respondWithError(w, "Missing request parameter(s)", http.StatusBadRequest, metric.RequestCreateMatch)
		return
	}

	_, err = valid.ValidateStruct(req)
	if err != nil {
		respondWithError(w, fmt.Sprintf("Invalid request parameters: %s", err.Error()), http.StatusBadRequest, metric.RequestCreateMatch)
		return
	}

	userOneValid, err := env.comm.CheckUser(*req.UserOne, r.Header.Get("Authorization"))
	if err != nil {
		respondWithError(w, fmt.Sprintf("Unable to reach user service: %s", err.Error()), http.StatusInternalServerError, metric.RequestCreateMatch)
		return
	}
	if !userOneValid {
		respondWithError(w, fmt.Sprintf("Unknown User: %s", req.UserOne.String()), http.StatusBadRequest, metric.RequestCreateMatch)
		return
	}

	userTwoValid, err := env.comm.CheckUser(*req.UserTwo, r.Header.Get("Authorization"))
	if err != nil {
		respondWithError(w, fmt.Sprintf("Unable to reach user service: %s", err.Error()), http.StatusInternalServerError, metric.RequestCreateMatch)
		return
	}
	if !userTwoValid {
		respondWithError(w, fmt.Sprintf("Unknown User: %s", req.UserTwo.String()), http.StatusBadRequest, metric.RequestCreateMatch)
		return
	}

	uuid, err := uuid.NewUUID()
	if err != nil {
		respondWithError(w, fmt.Sprintf("Could not create UUID: %s", err.Error()), http.StatusInternalServerError, metric.RequestCreateMatch)
		return
	}

	input := dao.CreateMatchInput{
		ID:      uuid,
		AuthID:  auth.ID,
		UserOne: *req.UserOne,
		UserTwo: *req.UserTwo,
	}

	for _, hook := range env.hook.beforeCreateMatchHooks {
		err := (*hook)(env, req, &input, auth)
		if err != nil {
			respondWithError(w, err.Error(), err.statusCode, metric.RequestCreateMatch)
			return
		}
	}

	timer := prometheus.NewTimer(metric.DatabaseRequestDuration.WithLabelValues(metric.RequestCreateMatch))
	match, err := env.dao.CreateMatch(input)
	timer.ObserveDuration()
	if err != nil {
		respondWithError(w, fmt.Sprintf("Something went wrong: %s", err.Error()), http.StatusInternalServerError, metric.RequestCreateMatch)
		return
	}

	for _, hook := range env.hook.afterCreateMatchHooks {
		err := (*hook)(env, match, auth)
		if err != nil {
			respondWithError(w, err.Error(), err.statusCode, metric.RequestCreateMatch)
			return
		}
	}

	json.NewEncoder(w).Encode(createMatchResponse{
		ID:        match.ID,
		UserOne:   match.UserOne,
		UserTwo:   match.UserTwo,
		MatchedOn: match.MatchedOn.Format(time.RFC3339),
	})

	metric.RequestSuccess.WithLabelValues(metric.RequestCreateMatch).Inc()
}

func (env *env) readMatchHandler(w http.ResponseWriter, r *http.Request) {
	auth, err := util.ExtractAuthIDFromRequest(r.Header)
	if err != nil {
		respondWithError(w, fmt.Sprintf("Could not authorize request: %s", err.Error()), http.StatusUnauthorized, metric.RequestReadMatch)
		return
	}

	matchID, err := util.ExtractIDFromRequest(mux.Vars(r))
	if err != nil {
		respondWithError(w, err.Error(), http.StatusBadRequest, metric.RequestReadMatch)
		return
	}

	authorized, err := checkAuthorization(env, matchID, auth)
	if err != nil {
		switch err.(type) {
		case dao.ErrMatchNotFound:
			respondWithError(w, "Unauthorized", http.StatusUnauthorized, metric.RequestReadMatch)
		default:
			respondWithError(w, fmt.Sprintf("Something went wrong: %s", err.Error()), http.StatusInternalServerError, metric.RequestReadMatch)
		}
		return
	}
	if !authorized {
		respondWithError(w, "Unauthorized", http.StatusUnauthorized, metric.RequestReadMatch)
		return
	}

	input := dao.ReadMatchInput{
		ID: matchID,
	}

	for _, hook := range env.hook.beforeReadMatchHooks {
		err := (*hook)(env, &input, auth)
		if err != nil {
			respondWithError(w, err.Error(), err.statusCode, metric.RequestReadMatch)
			return
		}
	}

	timer := prometheus.NewTimer(metric.DatabaseRequestDuration.WithLabelValues(metric.RequestReadMatch))
	match, err := env.dao.ReadMatch(input)
	timer.ObserveDuration()
	if err != nil {
		switch err.(type) {
		case dao.ErrMatchNotFound:
			respondWithError(w, err.Error(), http.StatusNotFound, metric.RequestReadMatch)
		default:
			respondWithError(w, fmt.Sprintf("Something went wrong: %s", err.Error()), http.StatusInternalServerError, metric.RequestReadMatch)
		}
		return
	}

	for _, hook := range env.hook.afterReadMatchHooks {
		err := (*hook)(env, match, auth)
		if err != nil {
			respondWithError(w, err.Error(), err.statusCode, metric.RequestReadMatch)
			return
		}
	}

	json.NewEncoder(w).Encode(readMatchResponse{
		ID:        match.ID,
		UserOne:   match.UserOne,
		UserTwo:   match.UserTwo,
		MatchedOn: match.MatchedOn.Format(time.RFC3339),
	})

	metric.RequestSuccess.WithLabelValues(metric.RequestReadMatch).Inc()
}

func (env *env) updateMatchHandler(w http.ResponseWriter, r *http.Request) {
	auth, err := util.ExtractAuthIDFromRequest(r.Header)
	if err != nil {
		respondWithError(w, fmt.Sprintf("Could not authorize request: %s", err.Error()), http.StatusUnauthorized, metric.RequestUpdateMatch)
		return
	}

	matchID, err := util.ExtractIDFromRequest(mux.Vars(r))
	if err != nil {
		respondWithError(w, err.Error(), http.StatusBadRequest, metric.RequestUpdateMatch)
		return
	}

	authorized, err := checkAuthorization(env, matchID, auth)
	if err != nil {
		switch err.(type) {
		case dao.ErrMatchNotFound:
			respondWithError(w, "Unauthorized", http.StatusUnauthorized, metric.RequestUpdateMatch)
		default:
			respondWithError(w, fmt.Sprintf("Something went wrong: %s", err.Error()), http.StatusInternalServerError, metric.RequestUpdateMatch)
		}
		return
	}
	if !authorized {
		respondWithError(w, "Unauthorized", http.StatusUnauthorized, metric.RequestUpdateMatch)
		return
	}

	var req updateMatchRequest
	err = json.NewDecoder(r.Body).Decode(&req)
	if err != nil {
		respondWithError(w, fmt.Sprintf("Invalid request parameters: %s", err.Error()), http.StatusBadRequest, metric.RequestUpdateMatch)
		return
	}

	if req.UserOne == nil || req.UserTwo == nil {
		respondWithError(w, "Missing request parameter(s)", http.StatusBadRequest, metric.RequestUpdateMatch)
		return
	}

	_, err = valid.ValidateStruct(req)
	if err != nil {
		respondWithError(w, fmt.Sprintf("Invalid request parameters: %s", err.Error()), http.StatusBadRequest, metric.RequestUpdateMatch)
		return
	}

	userOneValid, err := env.comm.CheckUser(*req.UserOne, r.Header.Get("Authorization"))
	if err != nil {
		respondWithError(w, fmt.Sprintf("Unable to reach user service: %s", err.Error()), http.StatusInternalServerError, metric.RequestUpdateMatch)
		return
	}
	if !userOneValid {
		respondWithError(w, fmt.Sprintf("Unknown User: %s", req.UserOne.String()), http.StatusBadRequest, metric.RequestUpdateMatch)
		return
	}

	userTwoValid, err := env.comm.CheckUser(*req.UserTwo, r.Header.Get("Authorization"))
	if err != nil {
		respondWithError(w, fmt.Sprintf("Unable to reach user service: %s", err.Error()), http.StatusInternalServerError, metric.RequestUpdateMatch)
		return
	}
	if !userTwoValid {
		respondWithError(w, fmt.Sprintf("Unknown User: %s", req.UserTwo.String()), http.StatusBadRequest, metric.RequestUpdateMatch)
		return
	}

	input := dao.UpdateMatchInput{
		ID:      matchID,
		UserOne: *req.UserOne,
		UserTwo: *req.UserTwo,
	}

	for _, hook := range env.hook.beforeUpdateMatchHooks {
		err := (*hook)(env, req, &input, auth)
		if err != nil {
			respondWithError(w, err.Error(), err.statusCode, metric.RequestUpdateMatch)
			return
		}
	}

	timer := prometheus.NewTimer(metric.DatabaseRequestDuration.WithLabelValues(metric.RequestUpdateMatch))
	match, err := env.dao.UpdateMatch(input)
	timer.ObserveDuration()
	if err != nil {
		switch err.(type) {
		case dao.ErrMatchNotFound:
			respondWithError(w, err.Error(), http.StatusNotFound, metric.RequestUpdateMatch)
		default:
			respondWithError(w, fmt.Sprintf("Something went wrong: %s", err.Error()), http.StatusInternalServerError, metric.RequestUpdateMatch)
		}
		return
	}

	for _, hook := range env.hook.afterUpdateMatchHooks {
		err := (*hook)(env, match, auth)
		if err != nil {
			respondWithError(w, err.Error(), err.statusCode, metric.RequestUpdateMatch)
			return
		}
	}

	json.NewEncoder(w).Encode(updateMatchResponse{
		ID:        match.ID,
		UserOne:   match.UserOne,
		UserTwo:   match.UserTwo,
		MatchedOn: match.MatchedOn.Format(time.RFC3339),
	})

	metric.RequestSuccess.WithLabelValues(metric.RequestUpdateMatch).Inc()
}

func (env *env) deleteMatchHandler(w http.ResponseWriter, r *http.Request) {
	auth, err := util.ExtractAuthIDFromRequest(r.Header)
	if err != nil {
		respondWithError(w, fmt.Sprintf("Could not authorize request: %s", err.Error()), http.StatusUnauthorized, metric.RequestDeleteMatch)
		return
	}

	matchID, err := util.ExtractIDFromRequest(mux.Vars(r))
	if err != nil {
		respondWithError(w, err.Error(), http.StatusBadRequest, metric.RequestDeleteMatch)
		return
	}

	authorized, err := checkAuthorization(env, matchID, auth)
	if err != nil {
		switch err.(type) {
		case dao.ErrMatchNotFound:
			respondWithError(w, "Unauthorized", http.StatusUnauthorized, metric.RequestDeleteMatch)
		default:
			respondWithError(w, fmt.Sprintf("Something went wrong: %s", err.Error()), http.StatusInternalServerError, metric.RequestDeleteMatch)
		}
		return
	}
	if !authorized {
		respondWithError(w, "Unauthorized", http.StatusUnauthorized, metric.RequestDeleteMatch)
		return
	}

	input := dao.DeleteMatchInput{
		ID: matchID,
	}

	for _, hook := range env.hook.beforeDeleteMatchHooks {
		err := (*hook)(env, &input, auth)
		if err != nil {
			respondWithError(w, err.Error(), err.statusCode, metric.RequestDeleteMatch)
			return
		}
	}

	timer := prometheus.NewTimer(metric.DatabaseRequestDuration.WithLabelValues(metric.RequestDeleteMatch))
	err = env.dao.DeleteMatch(input)
	timer.ObserveDuration()
	if err != nil {
		switch err.(type) {
		case dao.ErrMatchNotFound:
			respondWithError(w, err.Error(), http.StatusNotFound, metric.RequestDeleteMatch)
		default:
			respondWithError(w, fmt.Sprintf("Something went wrong: %s", err.Error()), http.StatusInternalServerError, metric.RequestDeleteMatch)
		}
		return
	}

	for _, hook := range env.hook.afterDeleteMatchHooks {
		err := (*hook)(env, auth)
		if err != nil {
			respondWithError(w, err.Error(), err.statusCode, metric.RequestDeleteMatch)
			return
		}
	}

	json.NewEncoder(w).Encode(struct{}{})

	metric.RequestSuccess.WithLabelValues(metric.RequestDeleteMatch).Inc()
}
