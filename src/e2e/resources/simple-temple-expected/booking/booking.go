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
	dao  dao.Datastore
	hook Hook
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

	env := env{d, Hook{}}

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

func (env *env) createBookingHandler(w http.ResponseWriter, r *http.Request) {
	auth, err := util.ExtractAuthIDFromRequest(r.Header)
	if err != nil {
		errMsg := util.CreateErrorJSON(fmt.Sprintf("Could not authorize request: %s", err.Error()))
		http.Error(w, errMsg, http.StatusUnauthorized)
		return
	}

	uuid, err := uuid.NewUUID()
	if err != nil {
		errMsg := util.CreateErrorJSON(fmt.Sprintf("Could not create UUID: %s", err.Error()))
		http.Error(w, errMsg, http.StatusInternalServerError)
		return
	}

	input := dao.CreateBookingInput{
		ID:     uuid,
		AuthID: auth.ID,
	}

	for _, hook := range env.hook.beforeCreateHooks {
		err := (*hook)(env, &input)
		if err != nil {
			// TODO
			return
		}
	}

	booking, err := env.dao.CreateBooking(input)
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
	auth, err := util.ExtractAuthIDFromRequest(r.Header)
	if err != nil {
		errMsg := util.CreateErrorJSON(fmt.Sprintf("Could not authorize request: %s", err.Error()))
		http.Error(w, errMsg, http.StatusUnauthorized)
		return
	}

	bookingID, err := util.ExtractIDFromRequest(mux.Vars(r))
	if err != nil {
		http.Error(w, util.CreateErrorJSON(err.Error()), http.StatusBadRequest)
		return
	}

	authorized, err := checkAuthorization(env, bookingID, auth)
	if err != nil {
		switch err.(type) {
		case dao.ErrBookingNotFound:
			errMsg := util.CreateErrorJSON("Unauthorized")
			http.Error(w, errMsg, http.StatusUnauthorized)
		default:
			errMsg := util.CreateErrorJSON(fmt.Sprintf("Something went wrong: %s", err.Error()))
			http.Error(w, errMsg, http.StatusInternalServerError)
		}
		return
	}
	if !authorized {
		errMsg := util.CreateErrorJSON("Unauthorized")
		http.Error(w, errMsg, http.StatusUnauthorized)
		return
	}

	input := dao.ReadBookingInput{
		ID: bookingID,
	}

	for _, hook := range env.hook.beforeReadHooks {
		err := (*hook)(env, &input)
		if err != nil {
			// TODO
			return
		}
	}

	booking, err := env.dao.ReadBooking(input)
	if err != nil {
		switch err.(type) {
		case dao.ErrBookingNotFound:
			http.Error(w, util.CreateErrorJSON(err.Error()), http.StatusNotFound)
		default:
			errMsg := util.CreateErrorJSON(fmt.Sprintf("Something went wrong: %s", err.Error()))
			http.Error(w, errMsg, http.StatusInternalServerError)
		}
		return
	}

	json.NewEncoder(w).Encode(readBookingResponse{
		ID: booking.ID,
	})
}

func (env *env) deleteBookingHandler(w http.ResponseWriter, r *http.Request) {
	auth, err := util.ExtractAuthIDFromRequest(r.Header)
	if err != nil {
		errMsg := util.CreateErrorJSON(fmt.Sprintf("Could not authorize request: %s", err.Error()))
		http.Error(w, errMsg, http.StatusUnauthorized)
		return
	}

	bookingID, err := util.ExtractIDFromRequest(mux.Vars(r))
	if err != nil {
		http.Error(w, util.CreateErrorJSON(err.Error()), http.StatusBadRequest)
		return
	}

	authorized, err := checkAuthorization(env, bookingID, auth)
	if err != nil {
		switch err.(type) {
		case dao.ErrBookingNotFound:
			errMsg := util.CreateErrorJSON("Unauthorized")
			http.Error(w, errMsg, http.StatusUnauthorized)
		default:
			errMsg := util.CreateErrorJSON(fmt.Sprintf("Something went wrong: %s", err.Error()))
			http.Error(w, errMsg, http.StatusInternalServerError)
		}
		return
	}
	if !authorized {
		errMsg := util.CreateErrorJSON("Unauthorized")
		http.Error(w, errMsg, http.StatusUnauthorized)
		return
	}

	input := dao.DeleteBookingInput{
		ID: bookingID,
	}

	for _, hook := range env.hook.beforeDeleteHooks {
		err := (*hook)(env, &input)
		if err != nil {
			// TODO
			return
		}
	}

	err = env.dao.DeleteBooking(input)
	if err != nil {
		switch err.(type) {
		case dao.ErrBookingNotFound:
			http.Error(w, util.CreateErrorJSON(err.Error()), http.StatusNotFound)
		default:
			errMsg := util.CreateErrorJSON(fmt.Sprintf("Something went wrong: %s", err.Error()))
			http.Error(w, errMsg, http.StatusInternalServerError)
		}
		return
	}

	json.NewEncoder(w).Encode(struct{}{})
}
