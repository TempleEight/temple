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
		respondWithError(w, fmt.Sprintf("Could not authorize request: %s", err.Error()), http.StatusUnauthorized, metric.RequestList)
		return
	}

	input := dao.ListMatchInput{
		AuthID: auth.ID,
	}

	for _, hook := range env.hook.beforeListHooks {
		err := (*hook)(env, &input)
		if err != nil {
			respondWithError(w, err.Error(), err.statusCode, metric.RequestList)
			return
		}
	}

	timer := prometheus.NewTimer(metric.DatabaseRequestDuration.WithLabelValues(metric.RequestList))
	matchList, err := env.dao.ListMatch(input)
	timer.ObserveDuration()
	if err != nil {
		respondWithError(w, fmt.Sprintf("Something went wrong: %s", err.Error()), http.StatusInternalServerError, metric.RequestList)
		return
	}

	for _, hook := range env.hook.afterListHooks {
		err := (*hook)(env, matchList)
		if err != nil {
			respondWithError(w, err.Error(), err.statusCode, metric.RequestList)
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

	metric.RequestSuccess.WithLabelValues(metric.RequestList).Inc()
}

func (env *env) createMatchHandler(w http.ResponseWriter, r *http.Request) {
	auth, err := util.ExtractAuthIDFromRequest(r.Header)
	if err != nil {
		respondWithError(w, fmt.Sprintf("Could not authorize request: %s", err.Error()), http.StatusUnauthorized, metric.RequestCreate)
		return
	}

	var req createMatchRequest
	err = json.NewDecoder(r.Body).Decode(&req)
	if err != nil {
		respondWithError(w, fmt.Sprintf("Invalid request parameters: %s", err.Error()), http.StatusBadRequest, metric.RequestCreate)
		return
	}

	if req.UserOne == nil || req.UserTwo == nil {
		respondWithError(w, "Missing request parameter(s)", http.StatusBadRequest, metric.RequestCreate)
		return
	}

	_, err = valid.ValidateStruct(req)
	if err != nil {
		respondWithError(w, fmt.Sprintf("Invalid request parameters: %s", err.Error()), http.StatusBadRequest, metric.RequestCreate)
		return
	}

	userOneValid, err := env.comm.CheckUser(*req.UserOne, r.Header.Get("Authorization"))
	if err != nil {
		respondWithError(w, fmt.Sprintf("Unable to reach user service: %s", err.Error()), http.StatusInternalServerError, metric.RequestCreate)
		return
	}
	if !userOneValid {
		respondWithError(w, fmt.Sprintf("Unknown User: %s", req.UserOne.String()), http.StatusBadRequest, metric.RequestCreate)
		return
	}

	userTwoValid, err := env.comm.CheckUser(*req.UserTwo, r.Header.Get("Authorization"))
	if err != nil {
		respondWithError(w, fmt.Sprintf("Unable to reach user service: %s", err.Error()), http.StatusInternalServerError, metric.RequestCreate)
		return
	}
	if !userTwoValid {
		respondWithError(w, fmt.Sprintf("Unknown User: %s", req.UserTwo.String()), http.StatusBadRequest, metric.RequestCreate)
		return
	}

	uuid, err := uuid.NewUUID()
	if err != nil {
		respondWithError(w, fmt.Sprintf("Could not create UUID: %s", err.Error()), http.StatusInternalServerError, metric.RequestCreate)
		return
	}

	input := dao.CreateMatchInput{
		ID:      uuid,
		AuthID:  auth.ID,
		UserOne: *req.UserOne,
		UserTwo: *req.UserTwo,
	}

	for _, hook := range env.hook.beforeCreateHooks {
		err := (*hook)(env, req, &input)
		if err != nil {
			respondWithError(w, err.Error(), err.statusCode, metric.RequestCreate)
			return
		}
	}

	timer := prometheus.NewTimer(metric.DatabaseRequestDuration.WithLabelValues(metric.RequestCreate))
	match, err := env.dao.CreateMatch(input)
	timer.ObserveDuration()
	if err != nil {
		respondWithError(w, fmt.Sprintf("Something went wrong: %s", err.Error()), http.StatusInternalServerError, metric.RequestCreate)
		return
	}

	for _, hook := range env.hook.afterCreateHooks {
		err := (*hook)(env, match)
		if err != nil {
			respondWithError(w, err.Error(), err.statusCode, metric.RequestCreate)
			return
		}
	}

	json.NewEncoder(w).Encode(createMatchResponse{
		ID:        match.ID,
		UserOne:   match.UserOne,
		UserTwo:   match.UserTwo,
		MatchedOn: match.MatchedOn.Format(time.RFC3339),
	})

	metric.RequestSuccess.WithLabelValues(metric.RequestCreate).Inc()
}

func (env *env) readMatchHandler(w http.ResponseWriter, r *http.Request) {
	auth, err := util.ExtractAuthIDFromRequest(r.Header)
	if err != nil {
		respondWithError(w, fmt.Sprintf("Could not authorize request: %s", err.Error()), http.StatusUnauthorized, metric.RequestRead)
		return
	}

	matchID, err := util.ExtractIDFromRequest(mux.Vars(r))
	if err != nil {
		respondWithError(w, err.Error(), http.StatusBadRequest, metric.RequestRead)
		return
	}

	authorized, err := checkAuthorization(env, matchID, auth)
	if err != nil {
		switch err.(type) {
		case dao.ErrMatchNotFound:
			respondWithError(w, "Unauthorized", http.StatusUnauthorized, metric.RequestRead)
		default:
			respondWithError(w, fmt.Sprintf("Something went wrong: %s", err.Error()), http.StatusInternalServerError, metric.RequestRead)
		}
		return
	}
	if !authorized {
		respondWithError(w, "Unauthorized", http.StatusUnauthorized, metric.RequestRead)
		return
	}

	input := dao.ReadMatchInput{
		ID: matchID,
	}

	for _, hook := range env.hook.beforeReadHooks {
		err := (*hook)(env, &input)
		if err != nil {
			respondWithError(w, err.Error(), err.statusCode, metric.RequestRead)
			return
		}
	}

	timer := prometheus.NewTimer(metric.DatabaseRequestDuration.WithLabelValues(metric.RequestRead))
	match, err := env.dao.ReadMatch(input)
	timer.ObserveDuration()
	if err != nil {
		switch err.(type) {
		case dao.ErrMatchNotFound:
			respondWithError(w, err.Error(), http.StatusNotFound, metric.RequestRead)
		default:
			respondWithError(w, fmt.Sprintf("Something went wrong: %s", err.Error()), http.StatusInternalServerError, metric.RequestRead)
		}
		return
	}

	for _, hook := range env.hook.afterReadHooks {
		err := (*hook)(env, match)
		if err != nil {
			respondWithError(w, err.Error(), err.statusCode, metric.RequestRead)
			return
		}
	}

	json.NewEncoder(w).Encode(readMatchResponse{
		ID:        match.ID,
		UserOne:   match.UserOne,
		UserTwo:   match.UserTwo,
		MatchedOn: match.MatchedOn.Format(time.RFC3339),
	})

	metric.RequestSuccess.WithLabelValues(metric.RequestRead).Inc()
}

func (env *env) updateMatchHandler(w http.ResponseWriter, r *http.Request) {
	auth, err := util.ExtractAuthIDFromRequest(r.Header)
	if err != nil {
		respondWithError(w, fmt.Sprintf("Could not authorize request: %s", err.Error()), http.StatusUnauthorized, metric.RequestUpdate)
		return
	}

	matchID, err := util.ExtractIDFromRequest(mux.Vars(r))
	if err != nil {
		respondWithError(w, err.Error(), http.StatusBadRequest, metric.RequestUpdate)
		return
	}

	authorized, err := checkAuthorization(env, matchID, auth)
	if err != nil {
		switch err.(type) {
		case dao.ErrMatchNotFound:
			respondWithError(w, "Unauthorized", http.StatusUnauthorized, metric.RequestUpdate)
		default:
			respondWithError(w, fmt.Sprintf("Something went wrong: %s", err.Error()), http.StatusInternalServerError, metric.RequestUpdate)
		}
		return
	}
	if !authorized {
		respondWithError(w, "Unauthorized", http.StatusUnauthorized, metric.RequestUpdate)
		return
	}

	var req updateMatchRequest
	err = json.NewDecoder(r.Body).Decode(&req)
	if err != nil {
		respondWithError(w, fmt.Sprintf("Invalid request parameters: %s", err.Error()), http.StatusBadRequest, metric.RequestUpdate)
		return
	}

	if req.UserOne == nil || req.UserTwo == nil {
		respondWithError(w, "Missing request parameter(s)", http.StatusBadRequest, metric.RequestUpdate)
		return
	}

	_, err = valid.ValidateStruct(req)
	if err != nil {
		respondWithError(w, fmt.Sprintf("Invalid request parameters: %s", err.Error()), http.StatusBadRequest, metric.RequestUpdate)
		return
	}

	userOneValid, err := env.comm.CheckUser(*req.UserOne, r.Header.Get("Authorization"))
	if err != nil {
		respondWithError(w, fmt.Sprintf("Unable to reach user service: %s", err.Error()), http.StatusInternalServerError, metric.RequestUpdate)
		return
	}
	if !userOneValid {
		respondWithError(w, fmt.Sprintf("Unknown User: %s", req.UserOne.String()), http.StatusBadRequest, metric.RequestUpdate)
		return
	}

	userTwoValid, err := env.comm.CheckUser(*req.UserTwo, r.Header.Get("Authorization"))
	if err != nil {
		respondWithError(w, fmt.Sprintf("Unable to reach user service: %s", err.Error()), http.StatusInternalServerError, metric.RequestUpdate)
		return
	}
	if !userTwoValid {
		respondWithError(w, fmt.Sprintf("Unknown User: %s", req.UserTwo.String()), http.StatusBadRequest, metric.RequestUpdate)
		return
	}

	input := dao.UpdateMatchInput{
		ID:      matchID,
		UserOne: *req.UserOne,
		UserTwo: *req.UserTwo,
	}

	for _, hook := range env.hook.beforeUpdateHooks {
		err := (*hook)(env, req, &input)
		if err != nil {
			respondWithError(w, err.Error(), err.statusCode, metric.RequestUpdate)
			return
		}
	}

	timer := prometheus.NewTimer(metric.DatabaseRequestDuration.WithLabelValues(metric.RequestUpdate))
	match, err := env.dao.UpdateMatch(input)
	timer.ObserveDuration()
	if err != nil {
		switch err.(type) {
		case dao.ErrMatchNotFound:
			respondWithError(w, err.Error(), http.StatusNotFound, metric.RequestUpdate)
		default:
			respondWithError(w, fmt.Sprintf("Something went wrong: %s", err.Error()), http.StatusInternalServerError, metric.RequestUpdate)
		}
		return
	}

	for _, hook := range env.hook.afterUpdateHooks {
		err := (*hook)(env, match)
		if err != nil {
			respondWithError(w, err.Error(), err.statusCode, metric.RequestUpdate)
			return
		}
	}

	json.NewEncoder(w).Encode(updateMatchResponse{
		ID:        match.ID,
		UserOne:   match.UserOne,
		UserTwo:   match.UserTwo,
		MatchedOn: match.MatchedOn.Format(time.RFC3339),
	})

	metric.RequestSuccess.WithLabelValues(metric.RequestUpdate).Inc()
}

func (env *env) deleteMatchHandler(w http.ResponseWriter, r *http.Request) {
	auth, err := util.ExtractAuthIDFromRequest(r.Header)
	if err != nil {
		respondWithError(w, fmt.Sprintf("Could not authorize request: %s", err.Error()), http.StatusUnauthorized, metric.RequestDelete)
		return
	}

	matchID, err := util.ExtractIDFromRequest(mux.Vars(r))
	if err != nil {
		respondWithError(w, err.Error(), http.StatusBadRequest, metric.RequestDelete)
		return
	}

	authorized, err := checkAuthorization(env, matchID, auth)
	if err != nil {
		switch err.(type) {
		case dao.ErrMatchNotFound:
			respondWithError(w, "Unauthorized", http.StatusUnauthorized, metric.RequestDelete)
		default:
			respondWithError(w, fmt.Sprintf("Something went wrong: %s", err.Error()), http.StatusInternalServerError, metric.RequestDelete)
		}
		return
	}
	if !authorized {
		respondWithError(w, "Unauthorized", http.StatusUnauthorized, metric.RequestDelete)
		return
	}

	input := dao.DeleteMatchInput{
		ID: matchID,
	}

	for _, hook := range env.hook.beforeDeleteHooks {
		err := (*hook)(env, &input)
		if err != nil {
			respondWithError(w, err.Error(), err.statusCode, metric.RequestDelete)
			return
		}
	}

	timer := prometheus.NewTimer(metric.DatabaseRequestDuration.WithLabelValues(metric.RequestDelete))
	err = env.dao.DeleteMatch(input)
	timer.ObserveDuration()
	if err != nil {
		switch err.(type) {
		case dao.ErrMatchNotFound:
			respondWithError(w, err.Error(), http.StatusNotFound, metric.RequestDelete)
		default:
			respondWithError(w, fmt.Sprintf("Something went wrong: %s", err.Error()), http.StatusInternalServerError, metric.RequestDelete)
		}
		return
	}

	for _, hook := range env.hook.afterDeleteHooks {
		err := (*hook)(env)
		if err != nil {
			respondWithError(w, err.Error(), err.statusCode, metric.RequestDelete)
			return
		}
	}

	json.NewEncoder(w).Encode(struct{}{})

	metric.RequestSuccess.WithLabelValues(metric.RequestDelete).Inc()
}
