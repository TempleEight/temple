package main

import (
	"encoding/base64"
	"encoding/json"
	"flag"
	"fmt"
	"log"
	"net/http"
	"time"

	"github.com/squat/and/dab/temple-user/dao"
	"github.com/squat/and/dab/temple-user/util"
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

// createTempleUserRequest contains the client-provided information required to create a single templeUser
type createTempleUserRequest struct {
	IntField      *int32   `json:"intField" validate:"required"`
	DoubleField   *float64 `json:"doubleField" validate:"required"`
	StringField   *string  `json:"stringField" validate:"required"`
	BoolField     *bool    `json:"boolField" validate:"required"`
	DateField     *string  `json:"dateField" validate:"required"`
	TimeField     *string  `json:"timeField" validate:"required"`
	DateTimeField *string  `json:"dateTimeField" validate:"required,datetime=2006-01-02T15:04:05.999999999Z07:00"`
	BlobField     *string  `json:"blobField" validate:"base64,required"`
}

// updateTempleUserRequest contains the client-provided information required to update a single templeUser
type updateTempleUserRequest struct {
	IntField      *int32   `json:"intField" validate:"required"`
	DoubleField   *float64 `json:"doubleField" validate:"required"`
	StringField   *string  `json:"stringField" validate:"required"`
	BoolField     *bool    `json:"boolField" validate:"required"`
	DateField     *string  `json:"dateField" validate:"required"`
	TimeField     *string  `json:"timeField" validate:"required"`
	DateTimeField *string  `json:"dateTimeField" validate:"required,datetime=2006-01-02T15:04:05.999999999Z07:00"`
	BlobField     *string  `json:"blobField" validate:"base64,required"`
}

// createTempleUserResponse contains a newly created templeUser to be returned to the client
type createTempleUserResponse struct {
	ID            uuid.UUID `json:"id"`
	IntField      int32     `json:"intField"`
	DoubleField   float64   `json:"doubleField"`
	StringField   string    `json:"stringField"`
	BoolField     bool      `json:"boolField"`
	DateField     string    `json:"dateField"`
	TimeField     string    `json:"timeField"`
	DateTimeField string    `json:"dateTimeField"`
	BlobField     string    `json:"blobField"`
}

// readTempleUserResponse contains a single templeUser to be returned to the client
type readTempleUserResponse struct {
	ID            uuid.UUID `json:"id"`
	IntField      int32     `json:"intField"`
	DoubleField   float64   `json:"doubleField"`
	StringField   string    `json:"stringField"`
	BoolField     bool      `json:"boolField"`
	DateField     string    `json:"dateField"`
	TimeField     string    `json:"timeField"`
	DateTimeField string    `json:"dateTimeField"`
	BlobField     string    `json:"blobField"`
}

// updateTempleUserResponse contains a newly updated templeUser to be returned to the client
type updateTempleUserResponse struct {
	ID            uuid.UUID `json:"id"`
	IntField      int32     `json:"intField"`
	DoubleField   float64   `json:"doubleField"`
	StringField   string    `json:"stringField"`
	BoolField     bool      `json:"boolField"`
	DateField     string    `json:"dateField"`
	TimeField     string    `json:"timeField"`
	DateTimeField string    `json:"dateTimeField"`
	BlobField     string    `json:"blobField"`
}

// defaultRouter generates a router for this service
func defaultRouter(env *env) *mux.Router {
	r := mux.NewRouter()
	// Mux directs to first matching route, i.e. the order matters
	r.HandleFunc("/temple-user", env.createTempleUserHandler).Methods(http.MethodPost)
	r.HandleFunc("/temple-user/{id}", env.readTempleUserHandler).Methods(http.MethodGet)
	r.HandleFunc("/temple-user/{id}", env.updateTempleUserHandler).Methods(http.MethodPut)
	r.HandleFunc("/temple-user/{id}", env.deleteTempleUserHandler).Methods(http.MethodDelete)
	r.Use(jsonMiddleware)
	return r
}

func main() {
	configPtr := flag.String("config", "/etc/temple-user-service/config.json", "configuration filepath")
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

func (env *env) createTempleUserHandler(w http.ResponseWriter, r *http.Request) {
	var req createTempleUserRequest
	err := json.NewDecoder(r.Body).Decode(&req)
	if err != nil {
		respondWithError(w, fmt.Sprintf("Invalid request parameters: %s", err.Error()), http.StatusBadRequest)
		return
	}

	if req.IntField == nil || req.DoubleField == nil || req.StringField == nil || req.BoolField == nil || req.DateField == nil || req.TimeField == nil || req.DateTimeField == nil || req.BlobField == nil {
		respondWithError(w, "Missing request parameter(s)", http.StatusBadRequest)
		return
	}

	err = env.valid.Struct(req)
	if err != nil {
		respondWithError(w, fmt.Sprintf("Invalid request parameters: %s", err.Error()), http.StatusBadRequest)
		return
	}

	dateField, err := time.Parse("2006-01-02", *req.DateField)
	if err != nil {
		respondWithError(w, fmt.Sprintf("Invalid date string: %s", err.Error()), http.StatusBadRequest)
		return
	}

	timeField, err := time.Parse("15:04:05.999999999", *req.TimeField)
	if err != nil {
		respondWithError(w, fmt.Sprintf("Invalid time string: %s", err.Error()), http.StatusBadRequest)
		return
	}

	dateTimeField, err := time.Parse(time.RFC3339Nano, *req.DateTimeField)
	if err != nil {
		respondWithError(w, fmt.Sprintf("Invalid datetime string: %s", err.Error()), http.StatusBadRequest)
		return
	}

	blobField, err := base64.StdEncoding.DecodeString(*req.BlobField)
	if err != nil {
		respondWithError(w, fmt.Sprintf("Invalid request parameters: %s", err.Error()), http.StatusBadRequest)
		return
	}

	uuid, err := uuid.NewUUID()
	if err != nil {
		respondWithError(w, fmt.Sprintf("Could not create UUID: %s", err.Error()), http.StatusInternalServerError)
		return
	}

	input := dao.CreateTempleUserInput{
		ID:            uuid,
		IntField:      *req.IntField,
		DoubleField:   *req.DoubleField,
		StringField:   *req.StringField,
		BoolField:     *req.BoolField,
		DateField:     dateField,
		TimeField:     timeField,
		DateTimeField: dateTimeField,
		BlobField:     blobField,
	}

	for _, hook := range env.hook.beforeCreateTempleUserHooks {
		err := (*hook)(env, req, &input)
		if err != nil {
			respondWithError(w, err.Error(), err.statusCode)
			return
		}
	}

	templeUser, err := env.dao.CreateTempleUser(input)
	if err != nil {
		respondWithError(w, fmt.Sprintf("Something went wrong: %s", err.Error()), http.StatusInternalServerError)
		return
	}

	for _, hook := range env.hook.afterCreateTempleUserHooks {
		err := (*hook)(env, templeUser)
		if err != nil {
			respondWithError(w, err.Error(), err.statusCode)
			return
		}
	}

	json.NewEncoder(w).Encode(createTempleUserResponse{
		ID:            templeUser.ID,
		IntField:      templeUser.IntField,
		DoubleField:   templeUser.DoubleField,
		StringField:   templeUser.StringField,
		BoolField:     templeUser.BoolField,
		DateField:     templeUser.DateField.Format("2006-01-02"),
		TimeField:     templeUser.TimeField.Format("15:04:05.999999999"),
		DateTimeField: templeUser.DateTimeField.Format(time.RFC3339),
		BlobField:     base64.StdEncoding.EncodeToString(templeUser.BlobField),
	})
}

func (env *env) readTempleUserHandler(w http.ResponseWriter, r *http.Request) {
	templeUserID, err := util.ExtractIDFromRequest(mux.Vars(r))
	if err != nil {
		respondWithError(w, err.Error(), http.StatusBadRequest)
		return
	}

	input := dao.ReadTempleUserInput{
		ID: templeUserID,
	}

	for _, hook := range env.hook.beforeReadTempleUserHooks {
		err := (*hook)(env, &input)
		if err != nil {
			respondWithError(w, err.Error(), err.statusCode)
			return
		}
	}

	templeUser, err := env.dao.ReadTempleUser(input)
	if err != nil {
		switch err.(type) {
		case dao.ErrTempleUserNotFound:
			respondWithError(w, err.Error(), http.StatusNotFound)
		default:
			respondWithError(w, fmt.Sprintf("Something went wrong: %s", err.Error()), http.StatusInternalServerError)
		}
		return
	}

	for _, hook := range env.hook.afterReadTempleUserHooks {
		err := (*hook)(env, templeUser)
		if err != nil {
			respondWithError(w, err.Error(), err.statusCode)
			return
		}
	}

	json.NewEncoder(w).Encode(readTempleUserResponse{
		ID:            templeUser.ID,
		IntField:      templeUser.IntField,
		DoubleField:   templeUser.DoubleField,
		StringField:   templeUser.StringField,
		BoolField:     templeUser.BoolField,
		DateField:     templeUser.DateField.Format("2006-01-02"),
		TimeField:     templeUser.TimeField.Format("15:04:05.999999999"),
		DateTimeField: templeUser.DateTimeField.Format(time.RFC3339),
		BlobField:     base64.StdEncoding.EncodeToString(templeUser.BlobField),
	})
}

func (env *env) updateTempleUserHandler(w http.ResponseWriter, r *http.Request) {
	templeUserID, err := util.ExtractIDFromRequest(mux.Vars(r))
	if err != nil {
		respondWithError(w, err.Error(), http.StatusBadRequest)
		return
	}

	var req updateTempleUserRequest
	err = json.NewDecoder(r.Body).Decode(&req)
	if err != nil {
		respondWithError(w, fmt.Sprintf("Invalid request parameters: %s", err.Error()), http.StatusBadRequest)
		return
	}

	if req.IntField == nil || req.DoubleField == nil || req.StringField == nil || req.BoolField == nil || req.DateField == nil || req.TimeField == nil || req.DateTimeField == nil || req.BlobField == nil {
		respondWithError(w, "Missing request parameter(s)", http.StatusBadRequest)
		return
	}

	err = env.valid.Struct(req)
	if err != nil {
		respondWithError(w, fmt.Sprintf("Invalid request parameters: %s", err.Error()), http.StatusBadRequest)
		return
	}

	dateField, err := time.Parse("2006-01-02", *req.DateField)
	if err != nil {
		respondWithError(w, fmt.Sprintf("Invalid date string: %s", err.Error()), http.StatusBadRequest)
		return
	}

	timeField, err := time.Parse("15:04:05.999999999", *req.TimeField)
	if err != nil {
		respondWithError(w, fmt.Sprintf("Invalid time string: %s", err.Error()), http.StatusBadRequest)
		return
	}

	dateTimeField, err := time.Parse(time.RFC3339Nano, *req.DateTimeField)
	if err != nil {
		respondWithError(w, fmt.Sprintf("Invalid datetime string: %s", err.Error()), http.StatusBadRequest)
		return
	}

	blobField, err := base64.StdEncoding.DecodeString(*req.BlobField)
	if err != nil {
		respondWithError(w, fmt.Sprintf("Invalid request parameters: %s", err.Error()), http.StatusBadRequest)
		return
	}

	input := dao.UpdateTempleUserInput{
		ID:            templeUserID,
		IntField:      *req.IntField,
		DoubleField:   *req.DoubleField,
		StringField:   *req.StringField,
		BoolField:     *req.BoolField,
		DateField:     dateField,
		TimeField:     timeField,
		DateTimeField: dateTimeField,
		BlobField:     blobField,
	}

	for _, hook := range env.hook.beforeUpdateTempleUserHooks {
		err := (*hook)(env, req, &input)
		if err != nil {
			respondWithError(w, err.Error(), err.statusCode)
			return
		}
	}

	templeUser, err := env.dao.UpdateTempleUser(input)
	if err != nil {
		switch err.(type) {
		case dao.ErrTempleUserNotFound:
			respondWithError(w, err.Error(), http.StatusNotFound)
		default:
			respondWithError(w, fmt.Sprintf("Something went wrong: %s", err.Error()), http.StatusInternalServerError)
		}
		return
	}

	for _, hook := range env.hook.afterUpdateTempleUserHooks {
		err := (*hook)(env, templeUser)
		if err != nil {
			respondWithError(w, err.Error(), err.statusCode)
			return
		}
	}

	json.NewEncoder(w).Encode(updateTempleUserResponse{
		ID:            templeUser.ID,
		IntField:      templeUser.IntField,
		DoubleField:   templeUser.DoubleField,
		StringField:   templeUser.StringField,
		BoolField:     templeUser.BoolField,
		DateField:     templeUser.DateField.Format("2006-01-02"),
		TimeField:     templeUser.TimeField.Format("15:04:05.999999999"),
		DateTimeField: templeUser.DateTimeField.Format(time.RFC3339),
		BlobField:     base64.StdEncoding.EncodeToString(templeUser.BlobField),
	})
}

func (env *env) deleteTempleUserHandler(w http.ResponseWriter, r *http.Request) {
	templeUserID, err := util.ExtractIDFromRequest(mux.Vars(r))
	if err != nil {
		respondWithError(w, err.Error(), http.StatusBadRequest)
		return
	}

	input := dao.DeleteTempleUserInput{
		ID: templeUserID,
	}

	for _, hook := range env.hook.beforeDeleteTempleUserHooks {
		err := (*hook)(env, &input)
		if err != nil {
			respondWithError(w, err.Error(), err.statusCode)
			return
		}
	}

	err = env.dao.DeleteTempleUser(input)
	if err != nil {
		switch err.(type) {
		case dao.ErrTempleUserNotFound:
			respondWithError(w, err.Error(), http.StatusNotFound)
		default:
			respondWithError(w, fmt.Sprintf("Something went wrong: %s", err.Error()), http.StatusInternalServerError)
		}
		return
	}

	for _, hook := range env.hook.afterDeleteTempleUserHooks {
		err := (*hook)(env)
		if err != nil {
			respondWithError(w, err.Error(), err.statusCode)
			return
		}
	}

	json.NewEncoder(w).Encode(struct{}{})
}
