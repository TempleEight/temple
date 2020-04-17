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

	"github.com/squat/and/dab/complex-user/dao"
	"github.com/squat/and/dab/complex-user/metric"
	"github.com/squat/and/dab/complex-user/util"
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
}

// createComplexUserRequest contains the client-provided information required to create a single complexUser
type createComplexUserRequest struct {
	SmallIntField      *uint16  `json:"smallIntField" valid:"type(*uint16),required,range(10|100)"`
	IntField           *uint32  `json:"intField" valid:"type(*uint32),required,range(10|100)"`
	BigIntField        *uint64  `json:"bigIntField" valid:"type(*uint64),required,range(10|100)"`
	FloatField         *float32 `json:"floatField" valid:"type(*float32),required,range(0.0|300.0)"`
	DoubleField        *float64 `json:"doubleField" valid:"type(*float64),required,range(0.0|123.0)"`
	StringField        *string  `json:"stringField" valid:"type(*string),required"`
	BoundedStringField *string  `json:"boundedStringField" valid:"type(*string),required,stringlength(0|5)"`
	BoolField          *bool    `json:"boolField" valid:"type(*bool),required"`
	DateField          *string  `json:"dateField" valid:"type(*string),required"`
	TimeField          *string  `json:"timeField" valid:"type(*string),required"`
	DateTimeField      *string  `json:"dateTimeField" valid:"type(*string),rfc3339,required"`
	BlobField          *string  `json:"blobField" valid:"type(*string),base64,required"`
}

// updateComplexUserRequest contains the client-provided information required to update a single complexUser
type updateComplexUserRequest struct {
	SmallIntField      *uint16  `json:"smallIntField" valid:"type(*uint16),required,range(10|100)"`
	IntField           *uint32  `json:"intField" valid:"type(*uint32),required,range(10|100)"`
	BigIntField        *uint64  `json:"bigIntField" valid:"type(*uint64),required,range(10|100)"`
	FloatField         *float32 `json:"floatField" valid:"type(*float32),required,range(0.0|300.0)"`
	DoubleField        *float64 `json:"doubleField" valid:"type(*float64),required,range(0.0|123.0)"`
	StringField        *string  `json:"stringField" valid:"type(*string),required"`
	BoundedStringField *string  `json:"boundedStringField" valid:"type(*string),required,stringlength(0|5)"`
	BoolField          *bool    `json:"boolField" valid:"type(*bool),required"`
	DateField          *string  `json:"dateField" valid:"type(*string),required"`
	TimeField          *string  `json:"timeField" valid:"type(*string),required"`
	DateTimeField      *string  `json:"dateTimeField" valid:"type(*string),rfc3339,required"`
	BlobField          *string  `json:"blobField" valid:"type(*string),base64,required"`
}

// createComplexUserResponse contains a newly created complexUser to be returned to the client
type createComplexUserResponse struct {
	ID                 uuid.UUID `json:"id"`
	SmallIntField      uint16    `json:"smallIntField"`
	IntField           uint32    `json:"intField"`
	BigIntField        uint64    `json:"bigIntField"`
	FloatField         float32   `json:"floatField"`
	DoubleField        float64   `json:"doubleField"`
	StringField        string    `json:"stringField"`
	BoundedStringField string    `json:"boundedStringField"`
	BoolField          bool      `json:"boolField"`
	DateField          string    `json:"dateField"`
	TimeField          string    `json:"timeField"`
	DateTimeField      string    `json:"dateTimeField"`
	BlobField          string    `json:"blobField"`
}

// readComplexUserResponse contains a single complexUser to be returned to the client
type readComplexUserResponse struct {
	ID                 uuid.UUID `json:"id"`
	SmallIntField      uint16    `json:"smallIntField"`
	IntField           uint32    `json:"intField"`
	BigIntField        uint64    `json:"bigIntField"`
	FloatField         float32   `json:"floatField"`
	DoubleField        float64   `json:"doubleField"`
	StringField        string    `json:"stringField"`
	BoundedStringField string    `json:"boundedStringField"`
	BoolField          bool      `json:"boolField"`
	DateField          string    `json:"dateField"`
	TimeField          string    `json:"timeField"`
	DateTimeField      string    `json:"dateTimeField"`
	BlobField          string    `json:"blobField"`
}

// updateComplexUserResponse contains a newly updated complexUser to be returned to the client
type updateComplexUserResponse struct {
	ID                 uuid.UUID `json:"id"`
	SmallIntField      uint16    `json:"smallIntField"`
	IntField           uint32    `json:"intField"`
	BigIntField        uint64    `json:"bigIntField"`
	FloatField         float32   `json:"floatField"`
	DoubleField        float64   `json:"doubleField"`
	StringField        string    `json:"stringField"`
	BoundedStringField string    `json:"boundedStringField"`
	BoolField          bool      `json:"boolField"`
	DateField          string    `json:"dateField"`
	TimeField          string    `json:"timeField"`
	DateTimeField      string    `json:"dateTimeField"`
	BlobField          string    `json:"blobField"`
}

// defaultRouter generates a router for this service
func defaultRouter(env *env) *mux.Router {
	r := mux.NewRouter()
	// Mux directs to first matching route, i.e. the order matters
	r.HandleFunc("/complex-user", env.createComplexUserHandler).Methods(http.MethodPost)
	r.HandleFunc("/complex-user/{id}", env.readComplexUserHandler).Methods(http.MethodGet)
	r.HandleFunc("/complex-user/{id}", env.updateComplexUserHandler).Methods(http.MethodPut)
	r.HandleFunc("/complex-user/{id}", env.deleteComplexUserHandler).Methods(http.MethodDelete)
	r.HandleFunc("/complex-user", env.identifyComplexUserHandler).Methods(http.MethodGet)
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

	env := env{d, Hook{}}

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
func respondWithError(w http.ResponseWriter, err string, statusCode int, requestType string) {
	w.WriteHeader(statusCode)
	fmt.Fprintln(w, util.CreateErrorJSON(err))
	metric.RequestFailure.WithLabelValues(requestType, strconv.Itoa(statusCode)).Inc()
}

func (env *env) createComplexUserHandler(w http.ResponseWriter, r *http.Request) {
	auth, err := util.ExtractAuthIDFromRequest(r.Header)
	if err != nil {
		respondWithError(w, fmt.Sprintf("Could not authorize request: %s", err.Error()), http.StatusUnauthorized, metric.RequestCreate)
		return
	}

	var req createComplexUserRequest
	err = json.NewDecoder(r.Body).Decode(&req)
	if err != nil {
		respondWithError(w, fmt.Sprintf("Invalid request parameters: %s", err.Error()), http.StatusBadRequest, metric.RequestCreate)
		return
	}

	if req.SmallIntField == nil || req.IntField == nil || req.BigIntField == nil || req.FloatField == nil || req.DoubleField == nil || req.StringField == nil || req.BoundedStringField == nil || req.BoolField == nil || req.DateField == nil || req.TimeField == nil || req.DateTimeField == nil || req.BlobField == nil {
		respondWithError(w, "Missing request parameter(s)", http.StatusBadRequest, metric.RequestCreate)
		return
	}

	_, err = valid.ValidateStruct(req)
	if err != nil {
		respondWithError(w, fmt.Sprintf("Invalid request parameters: %s", err.Error()), http.StatusBadRequest, metric.RequestCreate)
		return
	}

	dateField, err := time.Parse("2006-01-02", *req.DateField)
	if err != nil {
		respondWithError(w, fmt.Sprintf("Invalid date string: %s", err.Error()), http.StatusBadRequest, metric.RequestCreate)
		return
	}

	timeField, err := time.Parse("15:04:05.999999999", *req.TimeField)
	if err != nil {
		respondWithError(w, fmt.Sprintf("Invalid time string: %s", err.Error()), http.StatusBadRequest, metric.RequestCreate)
		return
	}

	dateTimeField, err := time.Parse(time.RFC3339Nano, *req.DateTimeField)
	if err != nil {
		respondWithError(w, fmt.Sprintf("Invalid datetime string: %s", err.Error()), http.StatusBadRequest, metric.RequestCreate)
		return
	}

	blobField, err := base64.StdEncoding.DecodeString(*req.BlobField)
	if err != nil {
		respondWithError(w, fmt.Sprintf("Invalid request parameters: %s", err.Error()), http.StatusBadRequest, metric.RequestCreate)
		return
	}

	input := dao.CreateComplexUserInput{
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
		BlobField:          blobField,
	}

	for _, hook := range env.hook.beforeCreateHooks {
		err := (*hook)(env, req, &input)
		if err != nil {
			respondWithError(w, err.Error(), err.statusCode, metric.RequestCreate)
			return
		}
	}

	timer := prometheus.NewTimer(metric.DatabaseRequestDuration.WithLabelValues(metric.RequestCreate))
	complexUser, err := env.dao.CreateComplexUser(input)
	timer.ObserveDuration()
	if err != nil {
		respondWithError(w, fmt.Sprintf("Something went wrong: %s", err.Error()), http.StatusInternalServerError, metric.RequestCreate)
		return
	}

	for _, hook := range env.hook.afterCreateHooks {
		err := (*hook)(env, complexUser)
		if err != nil {
			respondWithError(w, err.Error(), err.statusCode, metric.RequestCreate)
			return
		}
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

	metric.RequestSuccess.WithLabelValues(metric.RequestCreate).Inc()
}

func (env *env) readComplexUserHandler(w http.ResponseWriter, r *http.Request) {
	auth, err := util.ExtractAuthIDFromRequest(r.Header)
	if err != nil {
		respondWithError(w, fmt.Sprintf("Could not authorize request: %s", err.Error()), http.StatusUnauthorized, metric.RequestRead)
		return
	}

	complexUserID, err := util.ExtractIDFromRequest(mux.Vars(r))
	if err != nil {
		respondWithError(w, err.Error(), http.StatusBadRequest, metric.RequestRead)
		return
	}

	if auth.ID != complexUserID {
		respondWithError(w, "Unauthorized", http.StatusUnauthorized, metric.RequestRead)
		return
	}

	input := dao.ReadComplexUserInput{
		ID: complexUserID,
	}

	for _, hook := range env.hook.beforeReadHooks {
		err := (*hook)(env, &input)
		if err != nil {
			respondWithError(w, err.Error(), err.statusCode, metric.RequestRead)
			return
		}
	}

	timer := prometheus.NewTimer(metric.DatabaseRequestDuration.WithLabelValues(metric.RequestRead))
	complexUser, err := env.dao.ReadComplexUser(input)
	timer.ObserveDuration()
	if err != nil {
		switch err.(type) {
		case dao.ErrComplexUserNotFound:
			respondWithError(w, err.Error(), http.StatusNotFound, metric.RequestRead)
		default:
			respondWithError(w, fmt.Sprintf("Something went wrong: %s", err.Error()), http.StatusInternalServerError, metric.RequestRead)
		}
		return
	}

	for _, hook := range env.hook.afterReadHooks {
		err := (*hook)(env, complexUser)
		if err != nil {
			respondWithError(w, err.Error(), err.statusCode, metric.RequestRead)
			return
		}
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

	metric.RequestSuccess.WithLabelValues(metric.RequestRead).Inc()
}

func (env *env) updateComplexUserHandler(w http.ResponseWriter, r *http.Request) {
	auth, err := util.ExtractAuthIDFromRequest(r.Header)
	if err != nil {
		respondWithError(w, fmt.Sprintf("Could not authorize request: %s", err.Error()), http.StatusUnauthorized, metric.RequestUpdate)
		return
	}

	complexUserID, err := util.ExtractIDFromRequest(mux.Vars(r))
	if err != nil {
		respondWithError(w, err.Error(), http.StatusBadRequest, metric.RequestUpdate)
		return
	}

	if auth.ID != complexUserID {
		respondWithError(w, "Unauthorized", http.StatusUnauthorized, metric.RequestUpdate)
		return
	}

	var req updateComplexUserRequest
	err = json.NewDecoder(r.Body).Decode(&req)
	if err != nil {
		respondWithError(w, fmt.Sprintf("Invalid request parameters: %s", err.Error()), http.StatusBadRequest, metric.RequestUpdate)
		return
	}

	if req.SmallIntField == nil || req.IntField == nil || req.BigIntField == nil || req.FloatField == nil || req.DoubleField == nil || req.StringField == nil || req.BoundedStringField == nil || req.BoolField == nil || req.DateField == nil || req.TimeField == nil || req.DateTimeField == nil || req.BlobField == nil {
		respondWithError(w, "Missing request parameter(s)", http.StatusBadRequest, metric.RequestUpdate)
		return
	}

	_, err = valid.ValidateStruct(req)
	if err != nil {
		respondWithError(w, fmt.Sprintf("Invalid request parameters: %s", err.Error()), http.StatusBadRequest, metric.RequestUpdate)
		return
	}

	dateField, err := time.Parse("2006-01-02", *req.DateField)
	if err != nil {
		respondWithError(w, fmt.Sprintf("Invalid date string: %s", err.Error()), http.StatusBadRequest, metric.RequestUpdate)
		return
	}

	timeField, err := time.Parse("15:04:05.999999999", *req.TimeField)
	if err != nil {
		respondWithError(w, fmt.Sprintf("Invalid time string: %s", err.Error()), http.StatusBadRequest, metric.RequestUpdate)
		return
	}

	dateTimeField, err := time.Parse(time.RFC3339Nano, *req.DateTimeField)
	if err != nil {
		respondWithError(w, fmt.Sprintf("Invalid datetime string: %s", err.Error()), http.StatusBadRequest, metric.RequestUpdate)
		return
	}

	blobField, err := base64.StdEncoding.DecodeString(*req.BlobField)
	if err != nil {
		respondWithError(w, fmt.Sprintf("Invalid request parameters: %s", err.Error()), http.StatusBadRequest, metric.RequestUpdate)
		return
	}

	input := dao.UpdateComplexUserInput{
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
		BlobField:          blobField,
	}

	for _, hook := range env.hook.beforeUpdateHooks {
		err := (*hook)(env, req, &input)
		if err != nil {
			respondWithError(w, err.Error(), err.statusCode, metric.RequestUpdate)
			return
		}
	}

	timer := prometheus.NewTimer(metric.DatabaseRequestDuration.WithLabelValues(metric.RequestUpdate))
	complexUser, err := env.dao.UpdateComplexUser(input)
	timer.ObserveDuration()
	if err != nil {
		switch err.(type) {
		case dao.ErrComplexUserNotFound:
			respondWithError(w, err.Error(), http.StatusNotFound, metric.RequestUpdate)
		default:
			respondWithError(w, fmt.Sprintf("Something went wrong: %s", err.Error()), http.StatusInternalServerError, metric.RequestUpdate)
		}
		return
	}

	for _, hook := range env.hook.afterUpdateHooks {
		err := (*hook)(env, complexUser)
		if err != nil {
			respondWithError(w, err.Error(), err.statusCode, metric.RequestUpdate)
			return
		}
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

	metric.RequestSuccess.WithLabelValues(metric.RequestUpdate).Inc()
}

func (env *env) deleteComplexUserHandler(w http.ResponseWriter, r *http.Request) {
	auth, err := util.ExtractAuthIDFromRequest(r.Header)
	if err != nil {
		respondWithError(w, fmt.Sprintf("Could not authorize request: %s", err.Error()), http.StatusUnauthorized, metric.RequestDelete)
		return
	}

	complexUserID, err := util.ExtractIDFromRequest(mux.Vars(r))
	if err != nil {
		respondWithError(w, err.Error(), http.StatusBadRequest, metric.RequestDelete)
		return
	}

	if auth.ID != complexUserID {
		respondWithError(w, "Unauthorized", http.StatusUnauthorized, metric.RequestDelete)
		return
	}

	input := dao.DeleteComplexUserInput{
		ID: complexUserID,
	}

	for _, hook := range env.hook.beforeDeleteHooks {
		err := (*hook)(env, &input)
		if err != nil {
			respondWithError(w, err.Error(), err.statusCode, metric.RequestDelete)
			return
		}
	}

	timer := prometheus.NewTimer(metric.DatabaseRequestDuration.WithLabelValues(metric.RequestDelete))
	err = env.dao.DeleteComplexUser(input)
	timer.ObserveDuration()
	if err != nil {
		switch err.(type) {
		case dao.ErrComplexUserNotFound:
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

func (env *env) identifyComplexUserHandler(w http.ResponseWriter, r *http.Request) {
	auth, err := util.ExtractAuthIDFromRequest(r.Header)
	if err != nil {
		respondWithError(w, fmt.Sprintf("Could not authorize request: %s", err.Error()), http.StatusUnauthorized, metric.RequestIdentify)
		return
	}

	input := dao.IdentifyComplexUserInput{
		ID: auth.ID,
	}

	for _, hook := range env.hook.beforeIdentifyHooks {
		err := (*hook)(env, &input)
		if err != nil {
			respondWithError(w, err.Error(), err.statusCode, metric.RequestIdentify)
			return
		}
	}

	timer := prometheus.NewTimer(metric.DatabaseRequestDuration.WithLabelValues(metric.RequestIdentify))
	complexUser, err := env.dao.IdentifyComplexUser(input)
	timer.ObserveDuration()
	if err != nil {
		switch err.(type) {
		case dao.ErrComplexUserNotFound:
			respondWithError(w, err.Error(), http.StatusNotFound, metric.RequestIdentify)
		default:
			respondWithError(w, fmt.Sprintf("Something went wrong: %s", err.Error()), http.StatusInternalServerError, metric.RequestIdentify)
		}
		return
	}

	for _, hook := range env.hook.afterIdentifyHooks {
		err := (*hook)(env, complexUser)
		if err != nil {
			respondWithError(w, err.Error(), err.statusCode, metric.RequestIdentify)
			return
		}
	}

	url := fmt.Sprintf("%s:%s/api%s/%s", r.Header.Get("X-Forwarded-Host"), r.Header.Get("X-Forwarded-Port"), r.URL.Path, complexUser.ID)
	w.Header().Set("Location", url)
	w.WriteHeader(http.StatusFound)

	metric.RequestSuccess.WithLabelValues(metric.RequestIdentify).Inc()
}
