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
	valid "github.com/asaskevich/govalidator"
	"github.com/google/uuid"
	"github.com/gorilla/mux"
)

// env defines the environment that requests should be executed within
type env struct {
	dao dao.Datastore
}

// createTempleUserRequest contains the client-provided information required to create a single templeUser
type createTempleUserRequest struct {
	IntField      *int32   `json:"intField" valid:"type(*int32),required"`
	DoubleField   *float64 `json:"doubleField" valid:"type(*float64),required"`
	StringField   *string  `json:"stringField" valid:"type(*string),required"`
	BoolField     *bool    `json:"boolField" valid:"type(*bool),required"`
	DateField     *string  `json:"dateField" valid:"type(*string),required"`
	TimeField     *string  `json:"timeField" valid:"type(*string),required"`
	DateTimeField *string  `json:"dateTimeField" valid:"type(*string),rfc3339,required"`
	BlobField     *string  `json:"blobField" valid:"type(*string),base64,required"`
}

// updateTempleUserRequest contains the client-provided information required to update a single templeUser
type updateTempleUserRequest struct {
	IntField      *int32   `json:"intField" valid:"type(*int32),required"`
	DoubleField   *float64 `json:"doubleField" valid:"type(*float64),required"`
	StringField   *string  `json:"stringField" valid:"type(*string),required"`
	BoolField     *bool    `json:"boolField" valid:"type(*bool),required"`
	DateField     *string  `json:"dateField" valid:"type(*string),required"`
	TimeField     *string  `json:"timeField" valid:"type(*string),required"`
	DateTimeField *string  `json:"dateTimeField" valid:"type(*string),rfc3339,required"`
	BlobField     *string  `json:"blobField" valid:"type(*string),base64,required"`
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

func (env *env) createTempleUserHandler(w http.ResponseWriter, r *http.Request) {
	var req createTempleUserRequest
	err := json.NewDecoder(r.Body).Decode(&req)
	if err != nil {
		errMsg := util.CreateErrorJSON(fmt.Sprintf("Invalid request parameters: %s", err.Error()))
		http.Error(w, errMsg, http.StatusBadRequest)
		return
	}

	if req.IntField == nil || req.DoubleField == nil || req.StringField == nil || req.BoolField == nil || req.DateField == nil || req.TimeField == nil || req.DateTimeField == nil || req.BlobField == nil {
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

	dateField, err := time.Parse("2006-01-02", *req.DateField)
	if err != nil {
		errMsg := util.CreateErrorJSON(fmt.Sprintf("Invalid date string: %s", err.Error()))
		http.Error(w, errMsg, http.StatusBadRequest)
		return
	}

	timeField, err := time.Parse("15:04:05.999999999", *req.TimeField)
	if err != nil {
		errMsg := util.CreateErrorJSON(fmt.Sprintf("Invalid time string: %s", err.Error()))
		http.Error(w, errMsg, http.StatusBadRequest)
		return
	}

	dateTimeField, err := time.Parse(time.RFC3339Nano, *req.DateTimeField)
	if err != nil {
		errMsg := util.CreateErrorJSON(fmt.Sprintf("Invalid datetime string: %s", err.Error()))
		http.Error(w, errMsg, http.StatusBadRequest)
		return
	}

	uuid, err := uuid.NewUUID()
	if err != nil {
		errMsg := util.CreateErrorJSON(fmt.Sprintf("Could not create UUID: %s", err.Error()))
		http.Error(w, errMsg, http.StatusInternalServerError)
		return
	}

	templeUser, err := env.dao.CreateTempleUser(dao.CreateTempleUserInput{
		ID:            uuid,
		IntField:      *req.IntField,
		DoubleField:   *req.DoubleField,
		StringField:   *req.StringField,
		BoolField:     *req.BoolField,
		DateField:     dateField,
		TimeField:     timeField,
		DateTimeField: dateTimeField,
		BlobField:     *req.BlobField,
	})
	if err != nil {
		errMsg := util.CreateErrorJSON(fmt.Sprintf("Something went wrong: %s", err.Error()))
		http.Error(w, errMsg, http.StatusInternalServerError)
		return
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
		http.Error(w, util.CreateErrorJSON(err.Error()), http.StatusBadRequest)
		return
	}

	templeUser, err := env.dao.ReadTempleUser(dao.ReadTempleUserInput{
		ID: templeUserID,
	})
	if err != nil {
		switch err.(type) {
		case dao.ErrTempleUserNotFound:
			http.Error(w, util.CreateErrorJSON(err.Error()), http.StatusNotFound)
		default:
			errMsg := util.CreateErrorJSON(fmt.Sprintf("Something went wrong: %s", err.Error()))
			http.Error(w, errMsg, http.StatusInternalServerError)
		}
		return
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
		http.Error(w, util.CreateErrorJSON(err.Error()), http.StatusBadRequest)
		return
	}

	var req updateTempleUserRequest
	err = json.NewDecoder(r.Body).Decode(&req)
	if err != nil {
		errMsg := util.CreateErrorJSON(fmt.Sprintf("Invalid request parameters: %s", err.Error()))
		http.Error(w, errMsg, http.StatusBadRequest)
		return
	}

	if req.IntField == nil || req.DoubleField == nil || req.StringField == nil || req.BoolField == nil || req.DateField == nil || req.TimeField == nil || req.DateTimeField == nil || req.BlobField == nil {
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

	dateField, err := time.Parse("2006-01-02", *req.DateField)
	if err != nil {
		errMsg := util.CreateErrorJSON(fmt.Sprintf("Invalid date string: %s", err.Error()))
		http.Error(w, errMsg, http.StatusBadRequest)
		return
	}

	timeField, err := time.Parse("15:04:05.999999999", *req.TimeField)
	if err != nil {
		errMsg := util.CreateErrorJSON(fmt.Sprintf("Invalid time string: %s", err.Error()))
		http.Error(w, errMsg, http.StatusBadRequest)
		return
	}

	dateTimeField, err := time.Parse(time.RFC3339Nano, *req.DateTimeField)
	if err != nil {
		errMsg := util.CreateErrorJSON(fmt.Sprintf("Invalid datetime string: %s", err.Error()))
		http.Error(w, errMsg, http.StatusBadRequest)
		return
	}

	templeUser, err := env.dao.UpdateTempleUser(dao.UpdateTempleUserInput{
		ID:            templeUserID,
		IntField:      *req.IntField,
		DoubleField:   *req.DoubleField,
		StringField:   *req.StringField,
		BoolField:     *req.BoolField,
		DateField:     dateField,
		TimeField:     timeField,
		DateTimeField: dateTimeField,
		BlobField:     *req.BlobField,
	})
	if err != nil {
		switch err.(type) {
		case dao.ErrTempleUserNotFound:
			http.Error(w, util.CreateErrorJSON(err.Error()), http.StatusNotFound)
		default:
			errMsg := util.CreateErrorJSON(fmt.Sprintf("Something went wrong: %s", err.Error()))
			http.Error(w, errMsg, http.StatusInternalServerError)
		}
		return
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
		http.Error(w, util.CreateErrorJSON(err.Error()), http.StatusBadRequest)
		return
	}

	err = env.dao.DeleteTempleUser(dao.DeleteTempleUserInput{
		ID: templeUserID,
	})
	if err != nil {
		switch err.(type) {
		case dao.ErrTempleUserNotFound:
			http.Error(w, util.CreateErrorJSON(err.Error()), http.StatusNotFound)
		default:
			errMsg := util.CreateErrorJSON(fmt.Sprintf("Something went wrong: %s", err.Error()))
			http.Error(w, errMsg, http.StatusInternalServerError)
		}
		return
	}

	json.NewEncoder(w).Encode(struct{}{})
}
