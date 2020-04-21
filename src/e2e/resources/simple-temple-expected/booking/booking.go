package main

import (
	"encoding/json"
	"flag"
	"fmt"
	"log"
	"net/http"
	"strconv"

	"github.com/squat/and/dab/booking/dao"
	"github.com/squat/and/dab/booking/metric"
	"github.com/squat/and/dab/booking/util"
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

// createBookingResponse contains a newly created booking to be returned to the client
type createBookingResponse struct {
	ID uuid.UUID `json:"id"`
}

// readBookingResponse contains a single booking to be returned to the client
type readBookingResponse struct {
	ID uuid.UUID `json:"id"`
}

// defaultRouter generates a router for this service
func defaultRouter(env *env) *mux.Router {
	r := mux.NewRouter()
	// Mux directs to first matching route, i.e. the order matters
	r.HandleFunc("/booking", env.createBookingHandler).Methods(http.MethodPost)
	r.HandleFunc("/booking/{id}", env.readBookingHandler).Methods(http.MethodGet)
	r.HandleFunc("/booking/{id}", env.deleteBookingHandler).Methods(http.MethodDelete)
	r.Use(jsonMiddleware)
	return r
}

func main() {
	configPtr := flag.String("config", "/etc/booking-service/config.json", "configuration filepath")
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

	log.Fatal(http.ListenAndServe(":1028", router))
}

func jsonMiddleware(next http.Handler) http.Handler {
	return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		// All responses are JSON, set header accordingly
		w.Header().Set("Content-Type", "application/json")
		next.ServeHTTP(w, r)
	})
}

func checkAuthorization(env *env, bookingID uuid.UUID, auth *util.Auth) (bool, error) {
	input := dao.ReadBookingInput{
		ID: bookingID,
	}
	booking, err := env.dao.ReadBooking(input)
	if err != nil {
		return false, err
	}
	return booking.CreatedBy == auth.ID, nil
}

// respondWithError responds to a HTTP request with a JSON error response
func respondWithError(w http.ResponseWriter, err string, statusCode int, requestType string) {
	w.WriteHeader(statusCode)
	fmt.Fprintln(w, util.CreateErrorJSON(err))
	metric.RequestFailure.WithLabelValues(requestType, strconv.Itoa(statusCode)).Inc()
}

func (env *env) createBookingHandler(w http.ResponseWriter, r *http.Request) {
	auth, err := util.ExtractAuthIDFromRequest(r.Header)
	if err != nil {
		respondWithError(w, fmt.Sprintf("Could not authorize request: %s", err.Error()), http.StatusUnauthorized, metric.RequestCreateBooking)
		return
	}

	uuid, err := uuid.NewUUID()
	if err != nil {
		respondWithError(w, fmt.Sprintf("Could not create UUID: %s", err.Error()), http.StatusInternalServerError, metric.RequestCreateBooking)
		return
	}

	input := dao.CreateBookingInput{
		ID:     uuid,
		AuthID: auth.ID,
	}

	for _, hook := range env.hook.beforeCreateHooks {
		err := (*hook)(env, &input, auth)
		if err != nil {
			respondWithError(w, err.Error(), err.statusCode, metric.RequestCreateBooking)
			return
		}
	}

	timer := prometheus.NewTimer(metric.DatabaseRequestDuration.WithLabelValues(metric.RequestCreateBooking))
	booking, err := env.dao.CreateBooking(input)
	timer.ObserveDuration()
	if err != nil {
		respondWithError(w, fmt.Sprintf("Something went wrong: %s", err.Error()), http.StatusInternalServerError, metric.RequestCreateBooking)
		return
	}

	for _, hook := range env.hook.afterCreateHooks {
		err := (*hook)(env, booking, auth)
		if err != nil {
			respondWithError(w, err.Error(), err.statusCode, metric.RequestCreateBooking)
			return
		}
	}

	json.NewEncoder(w).Encode(createBookingResponse{
		ID: booking.ID,
	})

	metric.RequestSuccess.WithLabelValues(metric.RequestCreateBooking).Inc()
}

func (env *env) readBookingHandler(w http.ResponseWriter, r *http.Request) {
	auth, err := util.ExtractAuthIDFromRequest(r.Header)
	if err != nil {
		respondWithError(w, fmt.Sprintf("Could not authorize request: %s", err.Error()), http.StatusUnauthorized, metric.RequestReadBooking)
		return
	}

	bookingID, err := util.ExtractIDFromRequest(mux.Vars(r))
	if err != nil {
		respondWithError(w, err.Error(), http.StatusBadRequest, metric.RequestReadBooking)
		return
	}

	authorized, err := checkAuthorization(env, bookingID, auth)
	if err != nil {
		switch err.(type) {
		case dao.ErrBookingNotFound:
			respondWithError(w, "Unauthorized", http.StatusUnauthorized, metric.RequestReadBooking)
		default:
			respondWithError(w, fmt.Sprintf("Something went wrong: %s", err.Error()), http.StatusInternalServerError, metric.RequestReadBooking)
		}
		return
	}
	if !authorized {
		respondWithError(w, "Unauthorized", http.StatusUnauthorized, metric.RequestReadBooking)
		return
	}

	input := dao.ReadBookingInput{
		ID: bookingID,
	}

	for _, hook := range env.hook.beforeReadHooks {
		err := (*hook)(env, &input, auth)
		if err != nil {
			respondWithError(w, err.Error(), err.statusCode, metric.RequestReadBooking)
			return
		}
	}

	timer := prometheus.NewTimer(metric.DatabaseRequestDuration.WithLabelValues(metric.RequestReadBooking))
	booking, err := env.dao.ReadBooking(input)
	timer.ObserveDuration()
	if err != nil {
		switch err.(type) {
		case dao.ErrBookingNotFound:
			respondWithError(w, err.Error(), http.StatusNotFound, metric.RequestReadBooking)
		default:
			respondWithError(w, fmt.Sprintf("Something went wrong: %s", err.Error()), http.StatusInternalServerError, metric.RequestReadBooking)
		}
		return
	}

	for _, hook := range env.hook.afterReadHooks {
		err := (*hook)(env, booking, auth)
		if err != nil {
			respondWithError(w, err.Error(), err.statusCode, metric.RequestReadBooking)
			return
		}
	}

	json.NewEncoder(w).Encode(readBookingResponse{
		ID: booking.ID,
	})

	metric.RequestSuccess.WithLabelValues(metric.RequestReadBooking).Inc()
}

func (env *env) deleteBookingHandler(w http.ResponseWriter, r *http.Request) {
	auth, err := util.ExtractAuthIDFromRequest(r.Header)
	if err != nil {
		respondWithError(w, fmt.Sprintf("Could not authorize request: %s", err.Error()), http.StatusUnauthorized, metric.RequestDeleteBooking)
		return
	}

	bookingID, err := util.ExtractIDFromRequest(mux.Vars(r))
	if err != nil {
		respondWithError(w, err.Error(), http.StatusBadRequest, metric.RequestDeleteBooking)
		return
	}

	authorized, err := checkAuthorization(env, bookingID, auth)
	if err != nil {
		switch err.(type) {
		case dao.ErrBookingNotFound:
			respondWithError(w, "Unauthorized", http.StatusUnauthorized, metric.RequestDeleteBooking)
		default:
			respondWithError(w, fmt.Sprintf("Something went wrong: %s", err.Error()), http.StatusInternalServerError, metric.RequestDeleteBooking)
		}
		return
	}
	if !authorized {
		respondWithError(w, "Unauthorized", http.StatusUnauthorized, metric.RequestDeleteBooking)
		return
	}

	input := dao.DeleteBookingInput{
		ID: bookingID,
	}

	for _, hook := range env.hook.beforeDeleteHooks {
		err := (*hook)(env, &input, auth)
		if err != nil {
			respondWithError(w, err.Error(), err.statusCode, metric.RequestDeleteBooking)
			return
		}
	}

	timer := prometheus.NewTimer(metric.DatabaseRequestDuration.WithLabelValues(metric.RequestDeleteBooking))
	err = env.dao.DeleteBooking(input)
	timer.ObserveDuration()
	if err != nil {
		switch err.(type) {
		case dao.ErrBookingNotFound:
			respondWithError(w, err.Error(), http.StatusNotFound, metric.RequestDeleteBooking)
		default:
			respondWithError(w, fmt.Sprintf("Something went wrong: %s", err.Error()), http.StatusInternalServerError, metric.RequestDeleteBooking)
		}
		return
	}

	for _, hook := range env.hook.afterDeleteHooks {
		err := (*hook)(env, auth)
		if err != nil {
			respondWithError(w, err.Error(), err.statusCode, metric.RequestDeleteBooking)
			return
		}
	}

	json.NewEncoder(w).Encode(struct{}{})

	metric.RequestSuccess.WithLabelValues(metric.RequestDeleteBooking).Inc()
}
