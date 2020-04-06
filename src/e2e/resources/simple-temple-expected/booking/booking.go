package main

import (
	"encoding/json"
	"flag"
	"fmt"
	"log"
	"net/http"

	"github.com/squat/and/dab/booking/dao"
	"github.com/squat/and/dab/booking/util"
	valid "github.com/asaskevich/govalidator"
	"github.com/google/uuid"
	"github.com/gorilla/mux"
)

// env defines the environment that requests should be executed within
type env struct {
	dao dao.Datastore
}

// createBookingResponse contains a newly created booking to be returned to the client
type createBookingResponse struct {
	ID uuid.UUID
}

// readBookingResponse contains a single booking to be returned to the client
type readBookingResponse struct {
	ID uuid.UUID
}

// updateBookingResponse contains a newly updated booking to be returned to the client
type updateBookingResponse struct {
	ID uuid.UUID
}

// router generates a router for this service
func (env *env) router() *mux.Router {
	r := mux.NewRouter()
	// Mux directs to first matching route, i.e. the order matters
	r.HandleFunc("/booking", env.createBookingHandler).Methods(http.MethodPost)
	r.HandleFunc("/booking/{id}", env.readBookingHandler).Methods(http.MethodGet)
	r.HandleFunc("/booking/{id}", env.updateBookingHandler).Methods(http.MethodPut)
	r.HandleFunc("/booking/{id}", env.deleteBookingHandler).Methods(http.MethodDelete)
	r.Use(jsonMiddleware)
	return r
}

func main() {
	configPtr := flag.String("config", "/etc/booking-service/config.json", "configuration filepath")
	flag.Parse()

	// Require all struct fields by default
	valid.SetFieldsRequiredByDefault(true)

	config, err := util.GetConfig(*configPtr)
	if err != nil {
		log.Fatal(err)
	}

	d, err := dao.Init(config)
	if err != nil {
		log.Fatal(err)
	}

	env := env{d}

	log.Fatal(http.ListenAndServe(":1028", env.router()))
}

func jsonMiddleware(next http.Handler) http.Handler {
	return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		// All responses are JSON, set header accordingly
		w.Header().Set("Content-Type", "application/json")
		next.ServeHTTP(w, r)
	})
}

func (env *env) createBookingHandler(w http.ResponseWriter, r *http.Request) {
	auth, err := util.ExtractAuthIDFromRequest(r.Header)
	if err != nil {
		errMsg := util.CreateErrorJSON(fmt.Sprintf("Could not authorize request: %s", err.Error()))
		http.Error(w, errMsg, http.StatusUnauthorized)
		return
	}

	var req createBookingRequest
	err = json.NewDecoder(r.Body).Decode(&req)
	if err != nil {
		errMsg := util.CreateErrorJSON(fmt.Sprintf("Invalid request parameters: %s", err.Error()))
		http.Error(w, errMsg, http.StatusBadRequest)
		return
	}

	if {
		errMsg := util.CreateErrorJSON("Missing request parameter(s)")
		http.Error(w, errMsg, http.StatusBadRequest)
		return
	}

	_, err = valid.ValidateStruct(req)
	if err != nil {
		errMsg := util.CreateErrorJSON(fmt.Sprintf("Invalid request parameters: %s", err.Error()))
		http.Error(w, errMsg, http.StatusBadRequest)
		return
	}

	uuid, err := uuid.NewUUID()
	if err != nil {
		errMsg := util.CreateErrorJSON(fmt.Sprintf("Could not create UUID: %s", err.Error()))
		http.Error(w, errMsg, http.StatusInternalServerError)
		return
	}

	booking, err := env.dao.CreateBooking(dao.CreateBookingInput{
		ID:     uuid,
		AuthID: auth.ID,
	})
	if err != nil {
		errMsg := util.CreateErrorJSON(fmt.Sprintf("Something went wrong: %s", err.Error()))
		http.Error(w, errMsg, http.StatusInternalServerError)
		return
	}

	json.NewEncoder(w).Encode(createBookingResponse{
		ID: booking.ID,
	})
}

func (env *env) readBookingHandler(w http.ResponseWriter, r *http.Request) {

}

func (env *env) updateBookingHandler(w http.ResponseWriter, r *http.Request) {

}

func (env *env) deleteBookingHandler(w http.ResponseWriter, r *http.Request) {

}