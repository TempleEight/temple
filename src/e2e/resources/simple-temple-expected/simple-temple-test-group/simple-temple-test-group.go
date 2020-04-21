package main

import (
	"encoding/json"
	"flag"
	"fmt"
	"log"
	"net/http"
	"strconv"

	"github.com/squat/and/dab/simple-temple-test-group/dao"
	"github.com/squat/and/dab/simple-temple-test-group/metric"
	"github.com/squat/and/dab/simple-temple-test-group/util"
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

// createSimpleTempleTestGroupResponse contains a newly created simpleTempleTestGroup to be returned to the client
type createSimpleTempleTestGroupResponse struct {
	ID uuid.UUID `json:"id"`
}

// readSimpleTempleTestGroupResponse contains a single simpleTempleTestGroup to be returned to the client
type readSimpleTempleTestGroupResponse struct {
	ID uuid.UUID `json:"id"`
}

// defaultRouter generates a router for this service
func defaultRouter(env *env) *mux.Router {
	r := mux.NewRouter()
	// Mux directs to first matching route, i.e. the order matters
	r.HandleFunc("/simple-temple-test-group", env.createSimpleTempleTestGroupHandler).Methods(http.MethodPost)
	r.HandleFunc("/simple-temple-test-group/{id}", env.readSimpleTempleTestGroupHandler).Methods(http.MethodGet)
	r.HandleFunc("/simple-temple-test-group/{id}", env.deleteSimpleTempleTestGroupHandler).Methods(http.MethodDelete)
	r.Use(jsonMiddleware)
	return r
}

func main() {
	configPtr := flag.String("config", "/etc/simple-temple-test-group-service/config.json", "configuration filepath")
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

	log.Fatal(http.ListenAndServe(":1030", router))
}

func jsonMiddleware(next http.Handler) http.Handler {
	return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		// All responses are JSON, set header accordingly
		w.Header().Set("Content-Type", "application/json")
		next.ServeHTTP(w, r)
	})
}

func checkAuthorization(env *env, simpleTempleTestGroupID uuid.UUID, auth *util.Auth) (bool, error) {
	input := dao.ReadSimpleTempleTestGroupInput{
		ID: simpleTempleTestGroupID,
	}
	simpleTempleTestGroup, err := env.dao.ReadSimpleTempleTestGroup(input)
	if err != nil {
		return false, err
	}
	return simpleTempleTestGroup.CreatedBy == auth.ID, nil
}

// respondWithError responds to a HTTP request with a JSON error response
func respondWithError(w http.ResponseWriter, err string, statusCode int, requestType string) {
	w.WriteHeader(statusCode)
	fmt.Fprintln(w, util.CreateErrorJSON(err))
	metric.RequestFailure.WithLabelValues(requestType, strconv.Itoa(statusCode)).Inc()
}

func (env *env) createSimpleTempleTestGroupHandler(w http.ResponseWriter, r *http.Request) {
	auth, err := util.ExtractAuthIDFromRequest(r.Header)
	if err != nil {
		respondWithError(w, fmt.Sprintf("Could not authorize request: %s", err.Error()), http.StatusUnauthorized, metric.RequestCreateSimpleTempleTestGroup)
		return
	}

	uuid, err := uuid.NewUUID()
	if err != nil {
		respondWithError(w, fmt.Sprintf("Could not create UUID: %s", err.Error()), http.StatusInternalServerError, metric.RequestCreateSimpleTempleTestGroup)
		return
	}

	input := dao.CreateSimpleTempleTestGroupInput{
		ID:     uuid,
		AuthID: auth.ID,
	}

	for _, hook := range env.hook.beforeCreateSimpleTempleTestGroupHooks {
		err := (*hook)(env, &input, auth)
		if err != nil {
			respondWithError(w, err.Error(), err.statusCode, metric.RequestCreateSimpleTempleTestGroup)
			return
		}
	}

	timer := prometheus.NewTimer(metric.DatabaseRequestDuration.WithLabelValues(metric.RequestCreateSimpleTempleTestGroup))
	simpleTempleTestGroup, err := env.dao.CreateSimpleTempleTestGroup(input)
	timer.ObserveDuration()
	if err != nil {
		respondWithError(w, fmt.Sprintf("Something went wrong: %s", err.Error()), http.StatusInternalServerError, metric.RequestCreateSimpleTempleTestGroup)
		return
	}

	for _, hook := range env.hook.afterCreateSimpleTempleTestGroupHooks {
		err := (*hook)(env, simpleTempleTestGroup, auth)
		if err != nil {
			respondWithError(w, err.Error(), err.statusCode, metric.RequestCreateSimpleTempleTestGroup)
			return
		}
	}

	json.NewEncoder(w).Encode(createSimpleTempleTestGroupResponse{
		ID: simpleTempleTestGroup.ID,
	})

	metric.RequestSuccess.WithLabelValues(metric.RequestCreateSimpleTempleTestGroup).Inc()
}

func (env *env) readSimpleTempleTestGroupHandler(w http.ResponseWriter, r *http.Request) {
	auth, err := util.ExtractAuthIDFromRequest(r.Header)
	if err != nil {
		respondWithError(w, fmt.Sprintf("Could not authorize request: %s", err.Error()), http.StatusUnauthorized, metric.RequestReadSimpleTempleTestGroup)
		return
	}

	simpleTempleTestGroupID, err := util.ExtractIDFromRequest(mux.Vars(r))
	if err != nil {
		respondWithError(w, err.Error(), http.StatusBadRequest, metric.RequestReadSimpleTempleTestGroup)
		return
	}

	authorized, err := checkAuthorization(env, simpleTempleTestGroupID, auth)
	if err != nil {
		switch err.(type) {
		case dao.ErrSimpleTempleTestGroupNotFound:
			respondWithError(w, "Unauthorized", http.StatusUnauthorized, metric.RequestReadSimpleTempleTestGroup)
		default:
			respondWithError(w, fmt.Sprintf("Something went wrong: %s", err.Error()), http.StatusInternalServerError, metric.RequestReadSimpleTempleTestGroup)
		}
		return
	}
	if !authorized {
		respondWithError(w, "Unauthorized", http.StatusUnauthorized, metric.RequestReadSimpleTempleTestGroup)
		return
	}

	input := dao.ReadSimpleTempleTestGroupInput{
		ID: simpleTempleTestGroupID,
	}

	for _, hook := range env.hook.beforeReadSimpleTempleTestGroupHooks {
		err := (*hook)(env, &input, auth)
		if err != nil {
			respondWithError(w, err.Error(), err.statusCode, metric.RequestReadSimpleTempleTestGroup)
			return
		}
	}

	timer := prometheus.NewTimer(metric.DatabaseRequestDuration.WithLabelValues(metric.RequestReadSimpleTempleTestGroup))
	simpleTempleTestGroup, err := env.dao.ReadSimpleTempleTestGroup(input)
	timer.ObserveDuration()
	if err != nil {
		switch err.(type) {
		case dao.ErrSimpleTempleTestGroupNotFound:
			respondWithError(w, err.Error(), http.StatusNotFound, metric.RequestReadSimpleTempleTestGroup)
		default:
			respondWithError(w, fmt.Sprintf("Something went wrong: %s", err.Error()), http.StatusInternalServerError, metric.RequestReadSimpleTempleTestGroup)
		}
		return
	}

	for _, hook := range env.hook.afterReadSimpleTempleTestGroupHooks {
		err := (*hook)(env, simpleTempleTestGroup, auth)
		if err != nil {
			respondWithError(w, err.Error(), err.statusCode, metric.RequestReadSimpleTempleTestGroup)
			return
		}
	}

	json.NewEncoder(w).Encode(readSimpleTempleTestGroupResponse{
		ID: simpleTempleTestGroup.ID,
	})

	metric.RequestSuccess.WithLabelValues(metric.RequestReadSimpleTempleTestGroup).Inc()
}

func (env *env) deleteSimpleTempleTestGroupHandler(w http.ResponseWriter, r *http.Request) {
	auth, err := util.ExtractAuthIDFromRequest(r.Header)
	if err != nil {
		respondWithError(w, fmt.Sprintf("Could not authorize request: %s", err.Error()), http.StatusUnauthorized, metric.RequestDeleteSimpleTempleTestGroup)
		return
	}

	simpleTempleTestGroupID, err := util.ExtractIDFromRequest(mux.Vars(r))
	if err != nil {
		respondWithError(w, err.Error(), http.StatusBadRequest, metric.RequestDeleteSimpleTempleTestGroup)
		return
	}

	authorized, err := checkAuthorization(env, simpleTempleTestGroupID, auth)
	if err != nil {
		switch err.(type) {
		case dao.ErrSimpleTempleTestGroupNotFound:
			respondWithError(w, "Unauthorized", http.StatusUnauthorized, metric.RequestDeleteSimpleTempleTestGroup)
		default:
			respondWithError(w, fmt.Sprintf("Something went wrong: %s", err.Error()), http.StatusInternalServerError, metric.RequestDeleteSimpleTempleTestGroup)
		}
		return
	}
	if !authorized {
		respondWithError(w, "Unauthorized", http.StatusUnauthorized, metric.RequestDeleteSimpleTempleTestGroup)
		return
	}

	input := dao.DeleteSimpleTempleTestGroupInput{
		ID: simpleTempleTestGroupID,
	}

	for _, hook := range env.hook.beforeDeleteSimpleTempleTestGroupHooks {
		err := (*hook)(env, &input, auth)
		if err != nil {
			respondWithError(w, err.Error(), err.statusCode, metric.RequestDeleteSimpleTempleTestGroup)
			return
		}
	}

	timer := prometheus.NewTimer(metric.DatabaseRequestDuration.WithLabelValues(metric.RequestDeleteSimpleTempleTestGroup))
	err = env.dao.DeleteSimpleTempleTestGroup(input)
	timer.ObserveDuration()
	if err != nil {
		switch err.(type) {
		case dao.ErrSimpleTempleTestGroupNotFound:
			respondWithError(w, err.Error(), http.StatusNotFound, metric.RequestDeleteSimpleTempleTestGroup)
		default:
			respondWithError(w, fmt.Sprintf("Something went wrong: %s", err.Error()), http.StatusInternalServerError, metric.RequestDeleteSimpleTempleTestGroup)
		}
		return
	}

	for _, hook := range env.hook.afterDeleteSimpleTempleTestGroupHooks {
		err := (*hook)(env, auth)
		if err != nil {
			respondWithError(w, err.Error(), err.statusCode, metric.RequestDeleteSimpleTempleTestGroup)
			return
		}
	}

	json.NewEncoder(w).Encode(struct{}{})

	metric.RequestSuccess.WithLabelValues(metric.RequestDeleteSimpleTempleTestGroup).Inc()
}
