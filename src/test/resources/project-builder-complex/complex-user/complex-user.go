package main

import (
	"encoding/base64"
	"encoding/json"
	"flag"
	"fmt"
	"log"
	"net/http"
	"time"

	"github.com/squat/and/dab/complex-user/dao"
	"github.com/squat/and/dab/complex-user/util"
	valid "github.com/asaskevich/govalidator"
	"github.com/google/uuid"
	"github.com/gorilla/mux"
)

// env defines the environment that requests should be executed within
type env struct {
	dao dao.Datastore
}

// createComplexUserRequest contains the client-provided information required to create a single complexUser
type createComplexUserRequest struct {
	SmallIntField      *uint16  `valid:"type(*uint16),required,range(10|100)"`
	IntField           *uint32  `valid:"type(*uint32),required,range(10|100)"`
	BigIntField        *uint64  `valid:"type(*uint64),required,range(10|100)"`
	FloatField         *float32 `valid:"type(*float32),required,range(0.0|300.0)"`
	DoubleField        *float64 `valid:"type(*float64),required,range(0.0|123.0)"`
	StringField        *string  `valid:"type(*string),required"`
	BoundedStringField *string  `valid:"type(*string),required,stringlength(0|5)"`
	BoolField          *bool    `valid:"type(*bool),required"`
	DateField          *string  `valid:"type(*string),required"`
	TimeField          *string  `valid:"type(*string),required"`
	DateTimeField      *string  `valid:"type(*string),rfc3339,required"`
	BlobField          *string  `valid:"type(*string),base64,required"`
}

// updateComplexUserRequest contains the client-provided information required to update a single complexUser
type updateComplexUserRequest struct {
	SmallIntField      *uint16  `valid:"type(*uint16),required,range(10|100)"`
	IntField           *uint32  `valid:"type(*uint32),required,range(10|100)"`
	BigIntField        *uint64  `valid:"type(*uint64),required,range(10|100)"`
	FloatField         *float32 `valid:"type(*float32),required,range(0.0|300.0)"`
	DoubleField        *float64 `valid:"type(*float64),required,range(0.0|123.0)"`
	StringField        *string  `valid:"type(*string),required"`
	BoundedStringField *string  `valid:"type(*string),required,stringlength(0|5)"`
	BoolField          *bool    `valid:"type(*bool),required"`
	DateField          *string  `valid:"type(*string),required"`
	TimeField          *string  `valid:"type(*string),required"`
	DateTimeField      *string  `valid:"type(*string),rfc3339,required"`
	BlobField          *string  `valid:"type(*string),base64,required"`
}

// createComplexUserResponse contains a newly created complexUser to be returned to the client
type createComplexUserResponse struct {
	ID                 uuid.UUID
	SmallIntField      uint16
	IntField           uint32
	BigIntField        uint64
	FloatField         float32
	DoubleField        float64
	StringField        string
	BoundedStringField string
	BoolField          bool
	DateField          string
	TimeField          string
	DateTimeField      string
	BlobField          string
}

// readComplexUserResponse contains a single complexUser to be returned to the client
type readComplexUserResponse struct {
	ID                 uuid.UUID
	SmallIntField      uint16
	IntField           uint32
	BigIntField        uint64
	FloatField         float32
	DoubleField        float64
	StringField        string
	BoundedStringField string
	BoolField          bool
	DateField          string
	TimeField          string
	DateTimeField      string
	BlobField          string
}

// updateComplexUserResponse contains a newly updated complexUser to be returned to the client
type updateComplexUserResponse struct {
	ID                 uuid.UUID
	SmallIntField      uint16
	IntField           uint32
	BigIntField        uint64
	FloatField         float32
	DoubleField        float64
	StringField        string
	BoundedStringField string
	BoolField          bool
	DateField          string
	TimeField          string
	DateTimeField      string
	BlobField          string
}

// router generates a router for this service
func (env *env) router() *mux.Router {
	r := mux.NewRouter()
	// Mux directs to first matching route, i.e. the order matters
	r.HandleFunc("/complex-user", env.createComplexUserHandler).Methods(http.MethodPost)
	r.HandleFunc("/complex-user/{id}", env.readComplexUserHandler).Methods(http.MethodGet)
	r.HandleFunc("/complex-user/{id}", env.updateComplexUserHandler).Methods(http.MethodPut)
	r.HandleFunc("/complex-user/{id}", env.deleteComplexUserHandler).Methods(http.MethodDelete)
	r.Use(jsonMiddleware)
	return r
}

func main() {
	configPtr := flag.String("config", "/etc/complex-user-service/config.json", "configuration filepath")
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

	log.Fatal(http.ListenAndServe(":1026", env.router()))
}

func jsonMiddleware(next http.Handler) http.Handler {
	return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		// All responses are JSON, set header accordingly
		w.Header().Set("Content-Type", "application/json")
		next.ServeHTTP(w, r)
	})
}

func (env *env) createComplexUserHandler(w http.ResponseWriter, r *http.Request) {
	auth, err := util.ExtractAuthIDFromRequest(r.Header)
	if err != nil {
		errMsg := util.CreateErrorJSON(fmt.Sprintf("Could not authorize request: %s", err.Error()))
		http.Error(w, errMsg, http.StatusUnauthorized)
		return
	}

	var req createComplexUserRequest
	err = json.NewDecoder(r.Body).Decode(&req)
	if err != nil {
		errMsg := util.CreateErrorJSON(fmt.Sprintf("Invalid request parameters: %s", err.Error()))
		http.Error(w, errMsg, http.StatusBadRequest)
		return
	}

	if req.SmallIntField == nil || req.IntField == nil || req.BigIntField == nil || req.FloatField == nil || req.DoubleField == nil || req.StringField == nil || req.BoundedStringField == nil || req.BoolField == nil || req.DateField == nil || req.TimeField == nil || req.DateTimeField == nil || req.BlobField == nil {
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

	complexUser, err := env.dao.CreateComplexUser(dao.CreateComplexUserInput{
		ID:                 auth.ID,
		SmallIntField:      *req.SmallIntField,
		IntField:           *req.IntField,
		BigIntField:        *req.BigIntField,
		FloatField:         *req.FloatField,
		DoubleField:        *req.DoubleField,
		StringField:        *req.StringField,
		BoundedStringField: *req.BoundedStringField,
		BoolField:          *req.BoolField,
		DateField:          dateField,
		TimeField:          timeField,
		DateTimeField:      dateTimeField,
		BlobField:          *req.BlobField,
	})
	if err != nil {
		errMsg := util.CreateErrorJSON(fmt.Sprintf("Something went wrong: %s", err.Error()))
		http.Error(w, errMsg, http.StatusInternalServerError)
		return
	}

	json.NewEncoder(w).Encode(createComplexUserResponse{
		ID:                 complexUser.ID,
		SmallIntField:      complexUser.SmallIntField,
		IntField:           complexUser.IntField,
		BigIntField:        complexUser.BigIntField,
		FloatField:         complexUser.FloatField,
		DoubleField:        complexUser.DoubleField,
		StringField:        complexUser.StringField,
		BoundedStringField: complexUser.BoundedStringField,
		BoolField:          complexUser.BoolField,
		DateField:          complexUser.DateField.Format("2006-01-02"),
		TimeField:          complexUser.TimeField.Format("15:04:05.999999999"),
		DateTimeField:      complexUser.DateTimeField.Format(time.RFC3339),
		BlobField:          base64.StdEncoding.EncodeToString(complexUser.BlobField),
	})
}

func (env *env) readComplexUserHandler(w http.ResponseWriter, r *http.Request) {
	auth, err := util.ExtractAuthIDFromRequest(r.Header)
	if err != nil {
		errMsg := util.CreateErrorJSON(fmt.Sprintf("Could not authorize request: %s", err.Error()))
		http.Error(w, errMsg, http.StatusUnauthorized)
		return
	}

	complexUserID, err := util.ExtractIDFromRequest(mux.Vars(r))
	if err != nil {
		http.Error(w, util.CreateErrorJSON(err.Error()), http.StatusBadRequest)
		return
	}

	if auth.ID != complexUserID {
		errMsg := util.CreateErrorJSON("Unauthorized")
		http.Error(w, errMsg, http.StatusUnauthorized)
		return
	}

	complexUser, err := env.dao.ReadComplexUser(dao.ReadComplexUserInput{
		ID: complexUserID,
	})
	if err != nil {
		switch err.(type) {
		case dao.ErrComplexUserNotFound:
			http.Error(w, util.CreateErrorJSON(err.Error()), http.StatusNotFound)
		default:
			errMsg := util.CreateErrorJSON(fmt.Sprintf("Something went wrong: %s", err.Error()))
			http.Error(w, errMsg, http.StatusInternalServerError)
		}
		return
	}

	json.NewEncoder(w).Encode(readComplexUserResponse{
		ID:                 complexUser.ID,
		SmallIntField:      complexUser.SmallIntField,
		IntField:           complexUser.IntField,
		BigIntField:        complexUser.BigIntField,
		FloatField:         complexUser.FloatField,
		DoubleField:        complexUser.DoubleField,
		StringField:        complexUser.StringField,
		BoundedStringField: complexUser.BoundedStringField,
		BoolField:          complexUser.BoolField,
		DateField:          complexUser.DateField.Format("2006-01-02"),
		TimeField:          complexUser.TimeField.Format("15:04:05.999999999"),
		DateTimeField:      complexUser.DateTimeField.Format(time.RFC3339),
		BlobField:          base64.StdEncoding.EncodeToString(complexUser.BlobField),
	})
}

func (env *env) updateComplexUserHandler(w http.ResponseWriter, r *http.Request) {
	auth, err := util.ExtractAuthIDFromRequest(r.Header)
	if err != nil {
		errMsg := util.CreateErrorJSON(fmt.Sprintf("Could not authorize request: %s", err.Error()))
		http.Error(w, errMsg, http.StatusUnauthorized)
		return
	}

	complexUserID, err := util.ExtractIDFromRequest(mux.Vars(r))
	if err != nil {
		http.Error(w, util.CreateErrorJSON(err.Error()), http.StatusBadRequest)
		return
	}

	if auth.ID != complexUserID {
		errMsg := util.CreateErrorJSON("Unauthorized")
		http.Error(w, errMsg, http.StatusUnauthorized)
		return
	}

	var req updateComplexUserRequest
	err = json.NewDecoder(r.Body).Decode(&req)
	if err != nil {
		errMsg := util.CreateErrorJSON(fmt.Sprintf("Invalid request parameters: %s", err.Error()))
		http.Error(w, errMsg, http.StatusBadRequest)
		return
	}

	if req.SmallIntField == nil || req.IntField == nil || req.BigIntField == nil || req.FloatField == nil || req.DoubleField == nil || req.StringField == nil || req.BoundedStringField == nil || req.BoolField == nil || req.DateField == nil || req.TimeField == nil || req.DateTimeField == nil || req.BlobField == nil {
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

	complexUser, err := env.dao.UpdateComplexUser(dao.UpdateComplexUserInput{
		ID:                 complexUserID,
		SmallIntField:      *req.SmallIntField,
		IntField:           *req.IntField,
		BigIntField:        *req.BigIntField,
		FloatField:         *req.FloatField,
		DoubleField:        *req.DoubleField,
		StringField:        *req.StringField,
		BoundedStringField: *req.BoundedStringField,
		BoolField:          *req.BoolField,
		DateField:          dateField,
		TimeField:          timeField,
		DateTimeField:      dateTimeField,
		BlobField:          *req.BlobField,
	})
	if err != nil {
		switch err.(type) {
		case dao.ErrComplexUserNotFound:
			http.Error(w, util.CreateErrorJSON(err.Error()), http.StatusNotFound)
		default:
			errMsg := util.CreateErrorJSON(fmt.Sprintf("Something went wrong: %s", err.Error()))
			http.Error(w, errMsg, http.StatusInternalServerError)
		}
		return
	}

	json.NewEncoder(w).Encode(updateComplexUserResponse{
		ID:                 complexUser.ID,
		SmallIntField:      complexUser.SmallIntField,
		IntField:           complexUser.IntField,
		BigIntField:        complexUser.BigIntField,
		FloatField:         complexUser.FloatField,
		DoubleField:        complexUser.DoubleField,
		StringField:        complexUser.StringField,
		BoundedStringField: complexUser.BoundedStringField,
		BoolField:          complexUser.BoolField,
		DateField:          complexUser.DateField.Format("2006-01-02"),
		TimeField:          complexUser.TimeField.Format("15:04:05.999999999"),
		DateTimeField:      complexUser.DateTimeField.Format(time.RFC3339),
		BlobField:          base64.StdEncoding.EncodeToString(complexUser.BlobField),
	})
}

func (env *env) deleteComplexUserHandler(w http.ResponseWriter, r *http.Request) {
	auth, err := util.ExtractAuthIDFromRequest(r.Header)
	if err != nil {
		errMsg := util.CreateErrorJSON(fmt.Sprintf("Could not authorize request: %s", err.Error()))
		http.Error(w, errMsg, http.StatusUnauthorized)
		return
	}

	complexUserID, err := util.ExtractIDFromRequest(mux.Vars(r))
	if err != nil {
		http.Error(w, util.CreateErrorJSON(err.Error()), http.StatusBadRequest)
		return
	}

	if auth.ID != complexUserID {
		errMsg := util.CreateErrorJSON("Unauthorized")
		http.Error(w, errMsg, http.StatusUnauthorized)
		return
	}

	err = env.dao.DeleteComplexUser(dao.DeleteComplexUserInput{
		ID: complexUserID,
	})
	if err != nil {
		switch err.(type) {
		case dao.ErrComplexUserNotFound:
			http.Error(w, util.CreateErrorJSON(err.Error()), http.StatusNotFound)
		default:
			errMsg := util.CreateErrorJSON(fmt.Sprintf("Something went wrong: %s", err.Error()))
			http.Error(w, errMsg, http.StatusInternalServerError)
		}
		return
	}

	json.NewEncoder(w).Encode(struct{}{})
}
