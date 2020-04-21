package main

import (
	"encoding/base64"
	"encoding/json"
	"flag"
	"fmt"
	"log"
	"net/http"
	"strconv"
	"time"

	"github.com/squat/and/dab/simple-temple-test-user/dao"
	"github.com/squat/and/dab/simple-temple-test-user/metric"
	"github.com/squat/and/dab/simple-temple-test-user/util"
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

// createSimpleTempleTestUserRequest contains the client-provided information required to create a single simpleTempleTestUser
type createSimpleTempleTestUserRequest struct {
	SimpleTempleTestUser *string  `json:"simpleTempleTestUser" validate:"required"`
	Email                *string  `json:"email" validate:"required,gte=5,lte=40"`
	FirstName            *string  `json:"firstName" validate:"required"`
	LastName             *string  `json:"lastName" validate:"required"`
	CreatedAt            *string  `json:"createdAt" validate:"required,datetime=2006-01-02T15:04:05.999999999Z07:00"`
	NumberOfDogs         *int32   `json:"numberOfDogs" validate:"required"`
	CurrentBankBalance   *float32 `json:"currentBankBalance" validate:"required,gte=0.0"`
	BirthDate            *string  `json:"birthDate" validate:"required"`
	BreakfastTime        *string  `json:"breakfastTime" validate:"required"`
}

// updateSimpleTempleTestUserRequest contains the client-provided information required to update a single simpleTempleTestUser
type updateSimpleTempleTestUserRequest struct {
	SimpleTempleTestUser *string  `json:"simpleTempleTestUser" validate:"required"`
	Email                *string  `json:"email" validate:"required,gte=5,lte=40"`
	FirstName            *string  `json:"firstName" validate:"required"`
	LastName             *string  `json:"lastName" validate:"required"`
	CreatedAt            *string  `json:"createdAt" validate:"required,datetime=2006-01-02T15:04:05.999999999Z07:00"`
	NumberOfDogs         *int32   `json:"numberOfDogs" validate:"required"`
	CurrentBankBalance   *float32 `json:"currentBankBalance" validate:"required,gte=0.0"`
	BirthDate            *string  `json:"birthDate" validate:"required"`
	BreakfastTime        *string  `json:"breakfastTime" validate:"required"`
}

// createFredRequest contains the client-provided information required to create a single fred
type createFredRequest struct {
	Field  *string    `json:"field" valid:"type(*string),required"`
	Friend *uuid.UUID `json:"friend" valid:"required"`
	Image  *string    `json:"image" valid:"type(*string),base64,required"`
}

// updateFredRequest contains the client-provided information required to update a single fred
type updateFredRequest struct {
	Field  *string    `json:"field" valid:"type(*string),required"`
	Friend *uuid.UUID `json:"friend" valid:"required"`
	Image  *string    `json:"image" valid:"type(*string),base64,required"`
}

// listSimpleTempleTestUserElement contains a single simpleTempleTestUser list element
type listSimpleTempleTestUserElement struct {
	ID                   uuid.UUID `json:"id"`
	SimpleTempleTestUser string    `json:"simpleTempleTestUser"`
	Email                string    `json:"email"`
	FirstName            string    `json:"firstName"`
	LastName             string    `json:"lastName"`
	CreatedAt            string    `json:"createdAt"`
	NumberOfDogs         int32     `json:"numberOfDogs"`
	CurrentBankBalance   float32   `json:"currentBankBalance"`
	BirthDate            string    `json:"birthDate"`
	BreakfastTime        string    `json:"breakfastTime"`
}

// listSimpleTempleTestUserResponse contains a single simpleTempleTestUser list to be returned to the client
type listSimpleTempleTestUserResponse struct {
	SimpleTempleTestUserList []listSimpleTempleTestUserElement
}

// createSimpleTempleTestUserResponse contains a newly created simpleTempleTestUser to be returned to the client
type createSimpleTempleTestUserResponse struct {
	ID                   uuid.UUID `json:"id"`
	SimpleTempleTestUser string    `json:"simpleTempleTestUser"`
	Email                string    `json:"email"`
	FirstName            string    `json:"firstName"`
	LastName             string    `json:"lastName"`
	CreatedAt            string    `json:"createdAt"`
	NumberOfDogs         int32     `json:"numberOfDogs"`
	CurrentBankBalance   float32   `json:"currentBankBalance"`
	BirthDate            string    `json:"birthDate"`
	BreakfastTime        string    `json:"breakfastTime"`
}

// readSimpleTempleTestUserResponse contains a single simpleTempleTestUser to be returned to the client
type readSimpleTempleTestUserResponse struct {
	ID                   uuid.UUID `json:"id"`
	SimpleTempleTestUser string    `json:"simpleTempleTestUser"`
	Email                string    `json:"email"`
	FirstName            string    `json:"firstName"`
	LastName             string    `json:"lastName"`
	CreatedAt            string    `json:"createdAt"`
	NumberOfDogs         int32     `json:"numberOfDogs"`
	CurrentBankBalance   float32   `json:"currentBankBalance"`
	BirthDate            string    `json:"birthDate"`
	BreakfastTime        string    `json:"breakfastTime"`
}

// updateSimpleTempleTestUserResponse contains a newly updated simpleTempleTestUser to be returned to the client
type updateSimpleTempleTestUserResponse struct {
	ID                   uuid.UUID `json:"id"`
	SimpleTempleTestUser string    `json:"simpleTempleTestUser"`
	Email                string    `json:"email"`
	FirstName            string    `json:"firstName"`
	LastName             string    `json:"lastName"`
	CreatedAt            string    `json:"createdAt"`
	NumberOfDogs         int32     `json:"numberOfDogs"`
	CurrentBankBalance   float32   `json:"currentBankBalance"`
	BirthDate            string    `json:"birthDate"`
	BreakfastTime        string    `json:"breakfastTime"`
}

// listFredElement contains a single fred list element
type listFredElement struct {
	ID     uuid.UUID `json:"id"`
	Field  string    `json:"field"`
	Friend uuid.UUID `json:"friend"`
	Image  string    `json:"image"`
}

// listFredResponse contains a single fred list to be returned to the client
type listFredResponse struct {
	FredList []listFredElement
}

// createFredResponse contains a newly created fred to be returned to the client
type createFredResponse struct {
	ID     uuid.UUID `json:"id"`
	Field  string    `json:"field"`
	Friend uuid.UUID `json:"friend"`
	Image  string    `json:"image"`
}

// readFredResponse contains a single fred to be returned to the client
type readFredResponse struct {
	ID     uuid.UUID `json:"id"`
	Field  string    `json:"field"`
	Friend uuid.UUID `json:"friend"`
	Image  string    `json:"image"`
}

// updateFredResponse contains a newly updated fred to be returned to the client
type updateFredResponse struct {
	ID     uuid.UUID `json:"id"`
	Field  string    `json:"field"`
	Friend uuid.UUID `json:"friend"`
	Image  string    `json:"image"`
}

// defaultRouter generates a router for this service
func defaultRouter(env *env) *mux.Router {
	r := mux.NewRouter()
	// Mux directs to first matching route, i.e. the order matters
	r.HandleFunc("/simple-temple-test-user/all", env.listSimpleTempleTestUserHandler).Methods(http.MethodGet)
	r.HandleFunc("/simple-temple-test-user", env.createSimpleTempleTestUserHandler).Methods(http.MethodPost)
	r.HandleFunc("/simple-temple-test-user/{id}", env.readSimpleTempleTestUserHandler).Methods(http.MethodGet)
	r.HandleFunc("/simple-temple-test-user/{id}", env.updateSimpleTempleTestUserHandler).Methods(http.MethodPut)
	r.HandleFunc("/simple-temple-test-user", env.identifySimpleTempleTestUserHandler).Methods(http.MethodGet)
	r.HandleFunc("/simple-temple-test-user/{parent_id}/fred/all", env.listFredHandler).Methods(http.MethodGet)
	r.HandleFunc("/simple-temple-test-user/{parent_id}/fred", env.createFredHandler).Methods(http.MethodPost)
	r.HandleFunc("/simple-temple-test-user/{parent_id}/fred/{id}", env.readFredHandler).Methods(http.MethodGet)
	r.HandleFunc("/simple-temple-test-user/{parent_id}/fred/{id}", env.updateFredHandler).Methods(http.MethodPut)
	r.HandleFunc("/simple-temple-test-user/{parent_id}/fred/{id}", env.deleteFredHandler).Methods(http.MethodDelete)
	r.Use(jsonMiddleware)
	return r
}

func main() {
	configPtr := flag.String("config", "/etc/simple-temple-test-user-service/config.json", "configuration filepath")
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

	log.Fatal(http.ListenAndServe(":1026", router))
}

func jsonMiddleware(next http.Handler) http.Handler {
	return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		// All responses are JSON, set header accordingly
		w.Header().Set("Content-Type", "application/json")
		next.ServeHTTP(w, r)
	})
}

func checkAuthorization(env *env, simpleTempleTestUserID uuid.UUID, auth *util.Auth) (bool, error) {
	input := dao.ReadSimpleTempleTestUserInput{
		ID: simpleTempleTestUserID,
	}
	simpleTempleTestUser, err := env.dao.ReadSimpleTempleTestUser(input)
	if err != nil {
		return false, err
	}
	return simpleTempleTestUser.ID == auth.ID, nil
}

func checkFredParent(env *env, fredID uuid.UUID, parentID uuid.UUID) (bool, error) {
	input := dao.ReadFredInput{
		ID: fredID,
	}
	fred, err := env.dao.ReadFred(input)
	if err != nil {
		return false, err
	}
	return fred.ParentID == parentID, nil
}

// respondWithError responds to a HTTP request with a JSON error response
func respondWithError(w http.ResponseWriter, err string, statusCode int, requestType string) {
	w.WriteHeader(statusCode)
	fmt.Fprintln(w, util.CreateErrorJSON(err))
	metric.RequestFailure.WithLabelValues(requestType, strconv.Itoa(statusCode)).Inc()
}

func (env *env) listSimpleTempleTestUserHandler(w http.ResponseWriter, r *http.Request) {
	auth, err := util.ExtractAuthIDFromRequest(r.Header)
	if err != nil {
		respondWithError(w, fmt.Sprintf("Could not authorize request: %s", err.Error()), http.StatusUnauthorized, metric.RequestListSimpleTempleTestUser)
		return
	}

	for _, hook := range env.hook.beforeListSimpleTempleTestUserHooks {
		err := (*hook)(env, auth)
		if err != nil {
			respondWithError(w, err.Error(), err.statusCode, metric.RequestListSimpleTempleTestUser)
			return
		}
	}

	timer := prometheus.NewTimer(metric.DatabaseRequestDuration.WithLabelValues(metric.RequestListSimpleTempleTestUser))
	simpleTempleTestUserList, err := env.dao.ListSimpleTempleTestUser()
	timer.ObserveDuration()
	if err != nil {
		respondWithError(w, fmt.Sprintf("Something went wrong: %s", err.Error()), http.StatusInternalServerError, metric.RequestListSimpleTempleTestUser)
		return
	}

	for _, hook := range env.hook.afterListSimpleTempleTestUserHooks {
		err := (*hook)(env, simpleTempleTestUserList, auth)
		if err != nil {
			respondWithError(w, err.Error(), err.statusCode, metric.RequestListSimpleTempleTestUser)
			return
		}
	}

	simpleTempleTestUserListResp := listSimpleTempleTestUserResponse{
		SimpleTempleTestUserList: make([]listSimpleTempleTestUserElement, 0),
	}

	for _, simpleTempleTestUser := range *simpleTempleTestUserList {
		simpleTempleTestUserListResp.SimpleTempleTestUserList = append(simpleTempleTestUserListResp.SimpleTempleTestUserList, listSimpleTempleTestUserElement{
			ID:                   simpleTempleTestUser.ID,
			SimpleTempleTestUser: simpleTempleTestUser.SimpleTempleTestUser,
			Email:                simpleTempleTestUser.Email,
			FirstName:            simpleTempleTestUser.FirstName,
			LastName:             simpleTempleTestUser.LastName,
			CreatedAt:            simpleTempleTestUser.CreatedAt.Format(time.RFC3339),
			NumberOfDogs:         simpleTempleTestUser.NumberOfDogs,
			CurrentBankBalance:   simpleTempleTestUser.CurrentBankBalance,
			BirthDate:            simpleTempleTestUser.BirthDate.Format("2006-01-02"),
			BreakfastTime:        simpleTempleTestUser.BreakfastTime.Format("15:04:05.999999999"),
		})
	}

	json.NewEncoder(w).Encode(simpleTempleTestUserListResp)

	metric.RequestSuccess.WithLabelValues(metric.RequestListSimpleTempleTestUser).Inc()
}

func (env *env) createSimpleTempleTestUserHandler(w http.ResponseWriter, r *http.Request) {
	auth, err := util.ExtractAuthIDFromRequest(r.Header)
	if err != nil {
		respondWithError(w, fmt.Sprintf("Could not authorize request: %s", err.Error()), http.StatusUnauthorized, metric.RequestCreateSimpleTempleTestUser)
		return
	}

	var req createSimpleTempleTestUserRequest
	err = json.NewDecoder(r.Body).Decode(&req)
	if err != nil {
		respondWithError(w, fmt.Sprintf("Invalid request parameters: %s", err.Error()), http.StatusBadRequest, metric.RequestCreateSimpleTempleTestUser)
		return
	}

	if req.SimpleTempleTestUser == nil || req.Email == nil || req.FirstName == nil || req.LastName == nil || req.CreatedAt == nil || req.NumberOfDogs == nil || req.CurrentBankBalance == nil || req.BirthDate == nil || req.BreakfastTime == nil {
		respondWithError(w, "Missing request parameter(s)", http.StatusBadRequest, metric.RequestCreateSimpleTempleTestUser)
		return
	}

	err = env.valid.Struct(req)
	if err != nil {
		respondWithError(w, fmt.Sprintf("Invalid request parameters: %s", err.Error()), http.StatusBadRequest, metric.RequestCreateSimpleTempleTestUser)
		return
	}

	createdAt, err := time.Parse(time.RFC3339Nano, *req.CreatedAt)
	if err != nil {
		respondWithError(w, fmt.Sprintf("Invalid datetime string: %s", err.Error()), http.StatusBadRequest, metric.RequestCreateSimpleTempleTestUser)
		return
	}

	birthDate, err := time.Parse("2006-01-02", *req.BirthDate)
	if err != nil {
		respondWithError(w, fmt.Sprintf("Invalid date string: %s", err.Error()), http.StatusBadRequest, metric.RequestCreateSimpleTempleTestUser)
		return
	}

	breakfastTime, err := time.Parse("15:04:05.999999999", *req.BreakfastTime)
	if err != nil {
		respondWithError(w, fmt.Sprintf("Invalid time string: %s", err.Error()), http.StatusBadRequest, metric.RequestCreateSimpleTempleTestUser)
		return
	}

	input := dao.CreateSimpleTempleTestUserInput{
		ID:                   auth.ID,
		SimpleTempleTestUser: *req.SimpleTempleTestUser,
		Email:                *req.Email,
		FirstName:            *req.FirstName,
		LastName:             *req.LastName,
		CreatedAt:            createdAt,
		NumberOfDogs:         *req.NumberOfDogs,
		CurrentBankBalance:   *req.CurrentBankBalance,
		BirthDate:            birthDate,
		BreakfastTime:        breakfastTime,
	}

	for _, hook := range env.hook.beforeCreateSimpleTempleTestUserHooks {
		err := (*hook)(env, req, &input, auth)
		if err != nil {
			respondWithError(w, err.Error(), err.statusCode, metric.RequestCreateSimpleTempleTestUser)
			return
		}
	}

	timer := prometheus.NewTimer(metric.DatabaseRequestDuration.WithLabelValues(metric.RequestCreateSimpleTempleTestUser))
	simpleTempleTestUser, err := env.dao.CreateSimpleTempleTestUser(input)
	timer.ObserveDuration()
	if err != nil {
		respondWithError(w, fmt.Sprintf("Something went wrong: %s", err.Error()), http.StatusInternalServerError, metric.RequestCreateSimpleTempleTestUser)
		return
	}

	for _, hook := range env.hook.afterCreateSimpleTempleTestUserHooks {
		err := (*hook)(env, simpleTempleTestUser, auth)
		if err != nil {
			respondWithError(w, err.Error(), err.statusCode, metric.RequestCreateSimpleTempleTestUser)
			return
		}
	}

	json.NewEncoder(w).Encode(createSimpleTempleTestUserResponse{
		ID:                   simpleTempleTestUser.ID,
		SimpleTempleTestUser: simpleTempleTestUser.SimpleTempleTestUser,
		Email:                simpleTempleTestUser.Email,
		FirstName:            simpleTempleTestUser.FirstName,
		LastName:             simpleTempleTestUser.LastName,
		CreatedAt:            simpleTempleTestUser.CreatedAt.Format(time.RFC3339),
		NumberOfDogs:         simpleTempleTestUser.NumberOfDogs,
		CurrentBankBalance:   simpleTempleTestUser.CurrentBankBalance,
		BirthDate:            simpleTempleTestUser.BirthDate.Format("2006-01-02"),
		BreakfastTime:        simpleTempleTestUser.BreakfastTime.Format("15:04:05.999999999"),
	})

	metric.RequestSuccess.WithLabelValues(metric.RequestCreateSimpleTempleTestUser).Inc()
}

func (env *env) readSimpleTempleTestUserHandler(w http.ResponseWriter, r *http.Request) {
	auth, err := util.ExtractAuthIDFromRequest(r.Header)
	if err != nil {
		respondWithError(w, fmt.Sprintf("Could not authorize request: %s", err.Error()), http.StatusUnauthorized, metric.RequestReadSimpleTempleTestUser)
		return
	}

	simpleTempleTestUserID, err := util.ExtractIDFromRequest(mux.Vars(r))
	if err != nil {
		respondWithError(w, err.Error(), http.StatusBadRequest, metric.RequestReadSimpleTempleTestUser)
		return
	}

	input := dao.ReadSimpleTempleTestUserInput{
		ID: simpleTempleTestUserID,
	}

	for _, hook := range env.hook.beforeReadSimpleTempleTestUserHooks {
		err := (*hook)(env, &input, auth)
		if err != nil {
			respondWithError(w, err.Error(), err.statusCode, metric.RequestReadSimpleTempleTestUser)
			return
		}
	}

	timer := prometheus.NewTimer(metric.DatabaseRequestDuration.WithLabelValues(metric.RequestReadSimpleTempleTestUser))
	simpleTempleTestUser, err := env.dao.ReadSimpleTempleTestUser(input)
	timer.ObserveDuration()
	if err != nil {
		switch err.(type) {
		case dao.ErrSimpleTempleTestUserNotFound:
			respondWithError(w, err.Error(), http.StatusNotFound, metric.RequestReadSimpleTempleTestUser)
		default:
			respondWithError(w, fmt.Sprintf("Something went wrong: %s", err.Error()), http.StatusInternalServerError, metric.RequestReadSimpleTempleTestUser)
		}
		return
	}

	for _, hook := range env.hook.afterReadSimpleTempleTestUserHooks {
		err := (*hook)(env, simpleTempleTestUser, auth)
		if err != nil {
			respondWithError(w, err.Error(), err.statusCode, metric.RequestReadSimpleTempleTestUser)
			return
		}
	}

	json.NewEncoder(w).Encode(readSimpleTempleTestUserResponse{
		ID:                   simpleTempleTestUser.ID,
		SimpleTempleTestUser: simpleTempleTestUser.SimpleTempleTestUser,
		Email:                simpleTempleTestUser.Email,
		FirstName:            simpleTempleTestUser.FirstName,
		LastName:             simpleTempleTestUser.LastName,
		CreatedAt:            simpleTempleTestUser.CreatedAt.Format(time.RFC3339),
		NumberOfDogs:         simpleTempleTestUser.NumberOfDogs,
		CurrentBankBalance:   simpleTempleTestUser.CurrentBankBalance,
		BirthDate:            simpleTempleTestUser.BirthDate.Format("2006-01-02"),
		BreakfastTime:        simpleTempleTestUser.BreakfastTime.Format("15:04:05.999999999"),
	})

	metric.RequestSuccess.WithLabelValues(metric.RequestReadSimpleTempleTestUser).Inc()
}

func (env *env) updateSimpleTempleTestUserHandler(w http.ResponseWriter, r *http.Request) {
	auth, err := util.ExtractAuthIDFromRequest(r.Header)
	if err != nil {
		respondWithError(w, fmt.Sprintf("Could not authorize request: %s", err.Error()), http.StatusUnauthorized, metric.RequestUpdateSimpleTempleTestUser)
		return
	}

	simpleTempleTestUserID, err := util.ExtractIDFromRequest(mux.Vars(r))
	if err != nil {
		respondWithError(w, err.Error(), http.StatusBadRequest, metric.RequestUpdateSimpleTempleTestUser)
		return
	}

	if auth.ID != simpleTempleTestUserID {
		respondWithError(w, "Unauthorized", http.StatusUnauthorized, metric.RequestUpdateSimpleTempleTestUser)
		return
	}

	var req updateSimpleTempleTestUserRequest
	err = json.NewDecoder(r.Body).Decode(&req)
	if err != nil {
		respondWithError(w, fmt.Sprintf("Invalid request parameters: %s", err.Error()), http.StatusBadRequest, metric.RequestUpdateSimpleTempleTestUser)
		return
	}

	if req.SimpleTempleTestUser == nil || req.Email == nil || req.FirstName == nil || req.LastName == nil || req.CreatedAt == nil || req.NumberOfDogs == nil || req.CurrentBankBalance == nil || req.BirthDate == nil || req.BreakfastTime == nil {
		respondWithError(w, "Missing request parameter(s)", http.StatusBadRequest, metric.RequestUpdateSimpleTempleTestUser)
		return
	}

	err = env.valid.Struct(req)
	if err != nil {
		respondWithError(w, fmt.Sprintf("Invalid request parameters: %s", err.Error()), http.StatusBadRequest, metric.RequestUpdateSimpleTempleTestUser)
		return
	}

	createdAt, err := time.Parse(time.RFC3339Nano, *req.CreatedAt)
	if err != nil {
		respondWithError(w, fmt.Sprintf("Invalid datetime string: %s", err.Error()), http.StatusBadRequest, metric.RequestUpdateSimpleTempleTestUser)
		return
	}

	birthDate, err := time.Parse("2006-01-02", *req.BirthDate)
	if err != nil {
		respondWithError(w, fmt.Sprintf("Invalid date string: %s", err.Error()), http.StatusBadRequest, metric.RequestUpdateSimpleTempleTestUser)
		return
	}

	breakfastTime, err := time.Parse("15:04:05.999999999", *req.BreakfastTime)
	if err != nil {
		respondWithError(w, fmt.Sprintf("Invalid time string: %s", err.Error()), http.StatusBadRequest, metric.RequestUpdateSimpleTempleTestUser)
		return
	}

	input := dao.UpdateSimpleTempleTestUserInput{
		ID:                   simpleTempleTestUserID,
		SimpleTempleTestUser: *req.SimpleTempleTestUser,
		Email:                *req.Email,
		FirstName:            *req.FirstName,
		LastName:             *req.LastName,
		CreatedAt:            createdAt,
		NumberOfDogs:         *req.NumberOfDogs,
		CurrentBankBalance:   *req.CurrentBankBalance,
		BirthDate:            birthDate,
		BreakfastTime:        breakfastTime,
	}

	for _, hook := range env.hook.beforeUpdateSimpleTempleTestUserHooks {
		err := (*hook)(env, req, &input, auth)
		if err != nil {
			respondWithError(w, err.Error(), err.statusCode, metric.RequestUpdateSimpleTempleTestUser)
			return
		}
	}

	timer := prometheus.NewTimer(metric.DatabaseRequestDuration.WithLabelValues(metric.RequestUpdateSimpleTempleTestUser))
	simpleTempleTestUser, err := env.dao.UpdateSimpleTempleTestUser(input)
	timer.ObserveDuration()
	if err != nil {
		switch err.(type) {
		case dao.ErrSimpleTempleTestUserNotFound:
			respondWithError(w, err.Error(), http.StatusNotFound, metric.RequestUpdateSimpleTempleTestUser)
		default:
			respondWithError(w, fmt.Sprintf("Something went wrong: %s", err.Error()), http.StatusInternalServerError, metric.RequestUpdateSimpleTempleTestUser)
		}
		return
	}

	for _, hook := range env.hook.afterUpdateSimpleTempleTestUserHooks {
		err := (*hook)(env, simpleTempleTestUser, auth)
		if err != nil {
			respondWithError(w, err.Error(), err.statusCode, metric.RequestUpdateSimpleTempleTestUser)
			return
		}
	}

	json.NewEncoder(w).Encode(updateSimpleTempleTestUserResponse{
		ID:                   simpleTempleTestUser.ID,
		SimpleTempleTestUser: simpleTempleTestUser.SimpleTempleTestUser,
		Email:                simpleTempleTestUser.Email,
		FirstName:            simpleTempleTestUser.FirstName,
		LastName:             simpleTempleTestUser.LastName,
		CreatedAt:            simpleTempleTestUser.CreatedAt.Format(time.RFC3339),
		NumberOfDogs:         simpleTempleTestUser.NumberOfDogs,
		CurrentBankBalance:   simpleTempleTestUser.CurrentBankBalance,
		BirthDate:            simpleTempleTestUser.BirthDate.Format("2006-01-02"),
		BreakfastTime:        simpleTempleTestUser.BreakfastTime.Format("15:04:05.999999999"),
	})

	metric.RequestSuccess.WithLabelValues(metric.RequestUpdateSimpleTempleTestUser).Inc()
}

func (env *env) identifySimpleTempleTestUserHandler(w http.ResponseWriter, r *http.Request) {
	auth, err := util.ExtractAuthIDFromRequest(r.Header)
	if err != nil {
		respondWithError(w, fmt.Sprintf("Could not authorize request: %s", err.Error()), http.StatusUnauthorized, metric.RequestIdentifySimpleTempleTestUser)
		return
	}

	input := dao.IdentifySimpleTempleTestUserInput{
		ID: auth.ID,
	}

	for _, hook := range env.hook.beforeIdentifySimpleTempleTestUserHooks {
		err := (*hook)(env, &input, auth)
		if err != nil {
			respondWithError(w, err.Error(), err.statusCode, metric.RequestIdentifySimpleTempleTestUser)
			return
		}
	}

	timer := prometheus.NewTimer(metric.DatabaseRequestDuration.WithLabelValues(metric.RequestIdentifySimpleTempleTestUser))
	simpleTempleTestUser, err := env.dao.IdentifySimpleTempleTestUser(input)
	timer.ObserveDuration()
	if err != nil {
		switch err.(type) {
		case dao.ErrSimpleTempleTestUserNotFound:
			respondWithError(w, err.Error(), http.StatusNotFound, metric.RequestIdentifySimpleTempleTestUser)
		default:
			respondWithError(w, fmt.Sprintf("Something went wrong: %s", err.Error()), http.StatusInternalServerError, metric.RequestIdentifySimpleTempleTestUser)
		}
		return
	}

	for _, hook := range env.hook.afterIdentifySimpleTempleTestUserHooks {
		err := (*hook)(env, simpleTempleTestUser, auth)
		if err != nil {
			respondWithError(w, err.Error(), err.statusCode, metric.RequestIdentifySimpleTempleTestUser)
			return
		}
	}

	url := fmt.Sprintf("http://%s:%s/api%s/%s", r.Header.Get("X-Forwarded-Host"), r.Header.Get("X-Forwarded-Port"), r.URL.Path, simpleTempleTestUser.ID)
	w.Header().Set("Location", url)
	w.WriteHeader(http.StatusFound)

	metric.RequestSuccess.WithLabelValues(metric.RequestIdentifySimpleTempleTestUser).Inc()
}

func (env *env) listFredHandler(w http.ResponseWriter, r *http.Request) {
	auth, err := util.ExtractAuthIDFromRequest(r.Header)
	if err != nil {
		respondWithError(w, fmt.Sprintf("Could not authorize request: %s", err.Error()), http.StatusUnauthorized, metric.RequestListFred)
		return
	}

	simpleTempleTestUserID, err := util.ExtractParentIDFromRequest(mux.Vars(r))
	if err != nil {
		respondWithError(w, err.Error(), http.StatusBadRequest, metric.RequestListFred)
		return
	}

	input := dao.ListFredInput{
		ParentID: simpleTempleTestUserID,
	}

	for _, hook := range env.hook.beforeListFredHooks {
		err := (*hook)(env, auth)
		if err != nil {
			respondWithError(w, err.Error(), err.statusCode, metric.RequestListFred)
			return
		}
	}

	timer := prometheus.NewTimer(metric.DatabaseRequestDuration.WithLabelValues(metric.RequestListFred))
	fredList, err := env.dao.ListFred(input)
	timer.ObserveDuration()
	if err != nil {
		respondWithError(w, fmt.Sprintf("Something went wrong: %s", err.Error()), http.StatusInternalServerError, metric.RequestListFred)
		return
	}

	for _, hook := range env.hook.afterListFredHooks {
		err := (*hook)(env, fredList, auth)
		if err != nil {
			respondWithError(w, err.Error(), err.statusCode, metric.RequestListFred)
			return
		}
	}

	fredListResp := listFredResponse{
		FredList: make([]listFredElement, 0),
	}

	for _, fred := range *fredList {
		fredListResp.FredList = append(fredListResp.FredList, listFredElement{
			ID:     fred.ID,
			Field:  fred.Field,
			Friend: fred.Friend,
			Image:  base64.StdEncoding.EncodeToString(fred.Image),
		})
	}

	json.NewEncoder(w).Encode(fredListResp)

	metric.RequestSuccess.WithLabelValues(metric.RequestListFred).Inc()
}

func (env *env) createFredHandler(w http.ResponseWriter, r *http.Request) {
	auth, err := util.ExtractAuthIDFromRequest(r.Header)
	if err != nil {
		respondWithError(w, fmt.Sprintf("Could not authorize request: %s", err.Error()), http.StatusUnauthorized, metric.RequestCreateFred)
		return
	}

	simpleTempleTestUserID, err := util.ExtractParentIDFromRequest(mux.Vars(r))
	if err != nil {
		respondWithError(w, err.Error(), http.StatusBadRequest, metric.RequestCreateFred)
		return
	}

	authorized, err := checkAuthorization(env, simpleTempleTestUserID, auth)
	if err != nil {
		switch err.(type) {
		case dao.ErrSimpleTempleTestUserNotFound:
			respondWithError(w, "Unauthorized", http.StatusUnauthorized, metric.RequestCreateFred)
		default:
			respondWithError(w, fmt.Sprintf("Something went wrong: %s", err.Error()), http.StatusInternalServerError, metric.RequestCreateFred)
		}
		return
	}
	if !authorized {
		respondWithError(w, "Unauthorized", http.StatusUnauthorized, metric.RequestCreateFred)
		return
	}

	var req createFredRequest
	err = json.NewDecoder(r.Body).Decode(&req)
	if err != nil {
		respondWithError(w, fmt.Sprintf("Invalid request parameters: %s", err.Error()), http.StatusBadRequest, metric.RequestCreateFred)
		return
	}

	if req.Field == nil || req.Friend == nil || req.Image == nil {
		respondWithError(w, "Missing request parameter(s)", http.StatusBadRequest, metric.RequestCreateFred)
		return
	}

	_, err = valid.ValidateStruct(req)
	if err != nil {
		respondWithError(w, fmt.Sprintf("Invalid request parameters: %s", err.Error()), http.StatusBadRequest, metric.RequestCreateFred)
		return
	}

	image, err := base64.StdEncoding.DecodeString(*req.Image)
	if err != nil {
		respondWithError(w, fmt.Sprintf("Invalid request parameters: %s", err.Error()), http.StatusBadRequest, metric.RequestCreateFred)
		return
	}

	uuid, err := uuid.NewUUID()
	if err != nil {
		respondWithError(w, fmt.Sprintf("Could not create UUID: %s", err.Error()), http.StatusInternalServerError, metric.RequestCreateFred)
		return
	}

	input := dao.CreateFredInput{
		ID:     uuid,
		Field:  *req.Field,
		Friend: *req.Friend,
		Image:  image,
	}

	for _, hook := range env.hook.beforeCreateFredHooks {
		err := (*hook)(env, req, &input, auth)
		if err != nil {
			respondWithError(w, err.Error(), err.statusCode, metric.RequestCreateFred)
			return
		}
	}

	timer := prometheus.NewTimer(metric.DatabaseRequestDuration.WithLabelValues(metric.RequestCreateFred))
	fred, err := env.dao.CreateFred(input)
	timer.ObserveDuration()
	if err != nil {
		respondWithError(w, fmt.Sprintf("Something went wrong: %s", err.Error()), http.StatusInternalServerError, metric.RequestCreateFred)
		return
	}

	for _, hook := range env.hook.afterCreateFredHooks {
		err := (*hook)(env, fred, auth)
		if err != nil {
			respondWithError(w, err.Error(), err.statusCode, metric.RequestCreateFred)
			return
		}
	}

	json.NewEncoder(w).Encode(createFredResponse{
		ID:     fred.ID,
		Field:  fred.Field,
		Friend: fred.Friend,
		Image:  base64.StdEncoding.EncodeToString(fred.Image),
	})

	metric.RequestSuccess.WithLabelValues(metric.RequestCreateFred).Inc()
}

func (env *env) readFredHandler(w http.ResponseWriter, r *http.Request) {
	auth, err := util.ExtractAuthIDFromRequest(r.Header)
	if err != nil {
		respondWithError(w, fmt.Sprintf("Could not authorize request: %s", err.Error()), http.StatusUnauthorized, metric.RequestReadFred)
		return
	}

	fredID, err := util.ExtractIDFromRequest(mux.Vars(r))
	if err != nil {
		respondWithError(w, err.Error(), http.StatusBadRequest, metric.RequestReadFred)
		return
	}

	simpleTempleTestUserID, err := util.ExtractParentIDFromRequest(mux.Vars(r))
	if err != nil {
		respondWithError(w, err.Error(), http.StatusBadRequest, metric.RequestReadFred)
		return
	}

	correctParent, err := checkFredParent(env, fredID, simpleTempleTestUserID)
	if err != nil {
		switch err.(type) {
		case dao.ErrFredNotFound:
			respondWithError(w, "Not Found", http.StatusNotFound, metric.RequestReadFred)
		default:
			respondWithError(w, fmt.Sprintf("Something went wrong: %s", err.Error()), http.StatusInternalServerError, metric.RequestReadFred)
		}
		return
	}
	if !correctParent {
		respondWithError(w, "Not Found", http.StatusNotFound, metric.RequestReadFred)
		return
	}

	input := dao.ReadFredInput{
		ID: fredID,
	}

	for _, hook := range env.hook.beforeReadFredHooks {
		err := (*hook)(env, &input, auth)
		if err != nil {
			respondWithError(w, err.Error(), err.statusCode, metric.RequestReadFred)
			return
		}
	}

	timer := prometheus.NewTimer(metric.DatabaseRequestDuration.WithLabelValues(metric.RequestReadFred))
	fred, err := env.dao.ReadFred(input)
	timer.ObserveDuration()
	if err != nil {
		switch err.(type) {
		case dao.ErrFredNotFound:
			respondWithError(w, err.Error(), http.StatusNotFound, metric.RequestReadFred)
		default:
			respondWithError(w, fmt.Sprintf("Something went wrong: %s", err.Error()), http.StatusInternalServerError, metric.RequestReadFred)
		}
		return
	}

	for _, hook := range env.hook.afterReadFredHooks {
		err := (*hook)(env, fred, auth)
		if err != nil {
			respondWithError(w, err.Error(), err.statusCode, metric.RequestReadFred)
			return
		}
	}

	json.NewEncoder(w).Encode(readFredResponse{
		ID:     fred.ID,
		Field:  fred.Field,
		Friend: fred.Friend,
		Image:  base64.StdEncoding.EncodeToString(fred.Image),
	})

	metric.RequestSuccess.WithLabelValues(metric.RequestReadFred).Inc()
}

func (env *env) updateFredHandler(w http.ResponseWriter, r *http.Request) {
	auth, err := util.ExtractAuthIDFromRequest(r.Header)
	if err != nil {
		respondWithError(w, fmt.Sprintf("Could not authorize request: %s", err.Error()), http.StatusUnauthorized, metric.RequestUpdateFred)
		return
	}

	fredID, err := util.ExtractIDFromRequest(mux.Vars(r))
	if err != nil {
		respondWithError(w, err.Error(), http.StatusBadRequest, metric.RequestUpdateFred)
		return
	}

	simpleTempleTestUserID, err := util.ExtractParentIDFromRequest(mux.Vars(r))
	if err != nil {
		respondWithError(w, err.Error(), http.StatusBadRequest, metric.RequestUpdateFred)
		return
	}

	correctParent, err := checkFredParent(env, fredID, simpleTempleTestUserID)
	if err != nil {
		switch err.(type) {
		case dao.ErrFredNotFound:
			respondWithError(w, "Not Found", http.StatusNotFound, metric.RequestUpdateFred)
		default:
			respondWithError(w, fmt.Sprintf("Something went wrong: %s", err.Error()), http.StatusInternalServerError, metric.RequestUpdateFred)
		}
		return
	}
	if !correctParent {
		respondWithError(w, "Not Found", http.StatusNotFound, metric.RequestUpdateFred)
		return
	}

	authorized, err := checkAuthorization(env, simpleTempleTestUserID, auth)
	if err != nil {
		switch err.(type) {
		case dao.ErrSimpleTempleTestUserNotFound:
			respondWithError(w, "Unauthorized", http.StatusUnauthorized, metric.RequestUpdateFred)
		default:
			respondWithError(w, fmt.Sprintf("Something went wrong: %s", err.Error()), http.StatusInternalServerError, metric.RequestUpdateFred)
		}
		return
	}
	if !authorized {
		respondWithError(w, "Unauthorized", http.StatusUnauthorized, metric.RequestUpdateFred)
		return
	}

	var req updateFredRequest
	err = json.NewDecoder(r.Body).Decode(&req)
	if err != nil {
		respondWithError(w, fmt.Sprintf("Invalid request parameters: %s", err.Error()), http.StatusBadRequest, metric.RequestUpdateFred)
		return
	}

	if req.Field == nil || req.Friend == nil || req.Image == nil {
		respondWithError(w, "Missing request parameter(s)", http.StatusBadRequest, metric.RequestUpdateFred)
		return
	}

	_, err = valid.ValidateStruct(req)
	if err != nil {
		respondWithError(w, fmt.Sprintf("Invalid request parameters: %s", err.Error()), http.StatusBadRequest, metric.RequestUpdateFred)
		return
	}

	image, err := base64.StdEncoding.DecodeString(*req.Image)
	if err != nil {
		respondWithError(w, fmt.Sprintf("Invalid request parameters: %s", err.Error()), http.StatusBadRequest, metric.RequestUpdateFred)
		return
	}

	input := dao.UpdateFredInput{
		ID:     fredID,
		Field:  *req.Field,
		Friend: *req.Friend,
		Image:  image,
	}

	for _, hook := range env.hook.beforeUpdateFredHooks {
		err := (*hook)(env, req, &input, auth)
		if err != nil {
			respondWithError(w, err.Error(), err.statusCode, metric.RequestUpdateFred)
			return
		}
	}

	timer := prometheus.NewTimer(metric.DatabaseRequestDuration.WithLabelValues(metric.RequestUpdateFred))
	fred, err := env.dao.UpdateFred(input)
	timer.ObserveDuration()
	if err != nil {
		switch err.(type) {
		case dao.ErrFredNotFound:
			respondWithError(w, err.Error(), http.StatusNotFound, metric.RequestUpdateFred)
		default:
			respondWithError(w, fmt.Sprintf("Something went wrong: %s", err.Error()), http.StatusInternalServerError, metric.RequestUpdateFred)
		}
		return
	}

	for _, hook := range env.hook.afterUpdateFredHooks {
		err := (*hook)(env, fred, auth)
		if err != nil {
			respondWithError(w, err.Error(), err.statusCode, metric.RequestUpdateFred)
			return
		}
	}

	json.NewEncoder(w).Encode(updateFredResponse{
		ID:     fred.ID,
		Field:  fred.Field,
		Friend: fred.Friend,
		Image:  base64.StdEncoding.EncodeToString(fred.Image),
	})

	metric.RequestSuccess.WithLabelValues(metric.RequestUpdateFred).Inc()
}

func (env *env) deleteFredHandler(w http.ResponseWriter, r *http.Request) {
	auth, err := util.ExtractAuthIDFromRequest(r.Header)
	if err != nil {
		respondWithError(w, fmt.Sprintf("Could not authorize request: %s", err.Error()), http.StatusUnauthorized, metric.RequestDeleteFred)
		return
	}

	fredID, err := util.ExtractIDFromRequest(mux.Vars(r))
	if err != nil {
		respondWithError(w, err.Error(), http.StatusBadRequest, metric.RequestDeleteFred)
		return
	}

	simpleTempleTestUserID, err := util.ExtractParentIDFromRequest(mux.Vars(r))
	if err != nil {
		respondWithError(w, err.Error(), http.StatusBadRequest, metric.RequestDeleteFred)
		return
	}

	correctParent, err := checkFredParent(env, fredID, simpleTempleTestUserID)
	if err != nil {
		switch err.(type) {
		case dao.ErrFredNotFound:
			respondWithError(w, "Not Found", http.StatusNotFound, metric.RequestDeleteFred)
		default:
			respondWithError(w, fmt.Sprintf("Something went wrong: %s", err.Error()), http.StatusInternalServerError, metric.RequestDeleteFred)
		}
		return
	}
	if !correctParent {
		respondWithError(w, "Not Found", http.StatusNotFound, metric.RequestDeleteFred)
		return
	}

	authorized, err := checkAuthorization(env, simpleTempleTestUserID, auth)
	if err != nil {
		switch err.(type) {
		case dao.ErrSimpleTempleTestUserNotFound:
			respondWithError(w, "Unauthorized", http.StatusUnauthorized, metric.RequestDeleteFred)
		default:
			respondWithError(w, fmt.Sprintf("Something went wrong: %s", err.Error()), http.StatusInternalServerError, metric.RequestDeleteFred)
		}
		return
	}
	if !authorized {
		respondWithError(w, "Unauthorized", http.StatusUnauthorized, metric.RequestDeleteFred)
		return
	}

	input := dao.DeleteFredInput{
		ID: fredID,
	}

	for _, hook := range env.hook.beforeDeleteFredHooks {
		err := (*hook)(env, &input, auth)
		if err != nil {
			respondWithError(w, err.Error(), err.statusCode, metric.RequestDeleteFred)
			return
		}
	}

	timer := prometheus.NewTimer(metric.DatabaseRequestDuration.WithLabelValues(metric.RequestDeleteFred))
	err = env.dao.DeleteFred(input)
	timer.ObserveDuration()
	if err != nil {
		switch err.(type) {
		case dao.ErrFredNotFound:
			respondWithError(w, err.Error(), http.StatusNotFound, metric.RequestDeleteFred)
		default:
			respondWithError(w, fmt.Sprintf("Something went wrong: %s", err.Error()), http.StatusInternalServerError, metric.RequestDeleteFred)
		}
		return
	}

	for _, hook := range env.hook.afterDeleteFredHooks {
		err := (*hook)(env, auth)
		if err != nil {
			respondWithError(w, err.Error(), err.statusCode, metric.RequestDeleteFred)
			return
		}
	}

	json.NewEncoder(w).Encode(struct{}{})

	metric.RequestSuccess.WithLabelValues(metric.RequestDeleteFred).Inc()
}
