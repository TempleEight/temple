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
	r.HandleFunc("/complex-user", env.createComplexUserHandler).Methods(http.MethodPost)
	r.HandleFunc("/complex-user/{id}", env.readComplexUserHandler).Methods(http.MethodGet)
	r.HandleFunc("/complex-user/{id}", env.updateComplexUserHandler).Methods(http.MethodPut)
	r.HandleFunc("/complex-user/{id}", env.deleteComplexUserHandler).Methods(http.MethodDelete)
	r.HandleFunc("/complex-user", env.identifyComplexUserHandler).Methods(http.MethodGet)
	r.HandleFunc("/complex-user/{parent_id}/temple-user", env.createTempleUserHandler).Methods(http.MethodPost)
	r.HandleFunc("/complex-user/{parent_id}/temple-user/{id}", env.readTempleUserHandler).Methods(http.MethodGet)
	r.HandleFunc("/complex-user/{parent_id}/temple-user/{id}", env.updateTempleUserHandler).Methods(http.MethodPut)
	r.HandleFunc("/complex-user/{parent_id}/temple-user/{id}", env.deleteTempleUserHandler).Methods(http.MethodDelete)
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

func checkAuthorization(env *env, complexUserID uuid.UUID, auth *util.Auth) (bool, error) {
	input := dao.ReadComplexUserInput{
		ID: complexUserID,
	}
	complexUser, err := env.dao.ReadComplexUser(input)
	if err != nil {
		return false, err
	}
	return complexUser.ID == auth.ID, nil
}

func checkTempleUserParent(env *env, templeUserID uuid.UUID, parentID uuid.UUID) (bool, error) {
	input := dao.ReadTempleUserInput{
		ID: templeUserID,
	}
	templeUser, err := env.dao.ReadTempleUser(input)
	if err != nil {
		return false, err
	}
	return templeUser.ParentID == parentID, nil
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
		respondWithError(w, fmt.Sprintf("Could not authorize request: %s", err.Error()), http.StatusUnauthorized, metric.RequestCreateComplexUser)
		return
	}

	var req createComplexUserRequest
	err = json.NewDecoder(r.Body).Decode(&req)
	if err != nil {
		respondWithError(w, fmt.Sprintf("Invalid request parameters: %s", err.Error()), http.StatusBadRequest, metric.RequestCreateComplexUser)
		return
	}

	if req.SmallIntField == nil || req.IntField == nil || req.BigIntField == nil || req.FloatField == nil || req.DoubleField == nil || req.StringField == nil || req.BoundedStringField == nil || req.BoolField == nil || req.DateField == nil || req.TimeField == nil || req.DateTimeField == nil || req.BlobField == nil {
		respondWithError(w, "Missing request parameter(s)", http.StatusBadRequest, metric.RequestCreateComplexUser)
		return
	}

	_, err = valid.ValidateStruct(req)
	if err != nil {
		respondWithError(w, fmt.Sprintf("Invalid request parameters: %s", err.Error()), http.StatusBadRequest, metric.RequestCreateComplexUser)
		return
	}

	dateField, err := time.Parse("2006-01-02", *req.DateField)
	if err != nil {
		respondWithError(w, fmt.Sprintf("Invalid date string: %s", err.Error()), http.StatusBadRequest, metric.RequestCreateComplexUser)
		return
	}

	timeField, err := time.Parse("15:04:05.999999999", *req.TimeField)
	if err != nil {
		respondWithError(w, fmt.Sprintf("Invalid time string: %s", err.Error()), http.StatusBadRequest, metric.RequestCreateComplexUser)
		return
	}

	dateTimeField, err := time.Parse(time.RFC3339Nano, *req.DateTimeField)
	if err != nil {
		respondWithError(w, fmt.Sprintf("Invalid datetime string: %s", err.Error()), http.StatusBadRequest, metric.RequestCreateComplexUser)
		return
	}

	blobField, err := base64.StdEncoding.DecodeString(*req.BlobField)
	if err != nil {
		respondWithError(w, fmt.Sprintf("Invalid request parameters: %s", err.Error()), http.StatusBadRequest, metric.RequestCreateComplexUser)
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

	for _, hook := range env.hook.beforeCreateComplexUserHooks {
		err := (*hook)(env, req, &input, auth)
		if err != nil {
			respondWithError(w, err.Error(), err.statusCode, metric.RequestCreateComplexUser)
			return
		}
	}

	timer := prometheus.NewTimer(metric.DatabaseRequestDuration.WithLabelValues(metric.RequestCreateComplexUser))
	complexUser, err := env.dao.CreateComplexUser(input)
	timer.ObserveDuration()
	if err != nil {
		respondWithError(w, fmt.Sprintf("Something went wrong: %s", err.Error()), http.StatusInternalServerError, metric.RequestCreateComplexUser)
		return
	}

	for _, hook := range env.hook.afterCreateComplexUserHooks {
		err := (*hook)(env, complexUser, auth)
		if err != nil {
			respondWithError(w, err.Error(), err.statusCode, metric.RequestCreateComplexUser)
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

	metric.RequestSuccess.WithLabelValues(metric.RequestCreateComplexUser).Inc()
}

func (env *env) readComplexUserHandler(w http.ResponseWriter, r *http.Request) {
	auth, err := util.ExtractAuthIDFromRequest(r.Header)
	if err != nil {
		respondWithError(w, fmt.Sprintf("Could not authorize request: %s", err.Error()), http.StatusUnauthorized, metric.RequestReadComplexUser)
		return
	}

	complexUserID, err := util.ExtractIDFromRequest(mux.Vars(r))
	if err != nil {
		respondWithError(w, err.Error(), http.StatusBadRequest, metric.RequestReadComplexUser)
		return
	}

	if auth.ID != complexUserID {
		respondWithError(w, "Unauthorized", http.StatusUnauthorized, metric.RequestReadComplexUser)
		return
	}

	input := dao.ReadComplexUserInput{
		ID: complexUserID,
	}

	for _, hook := range env.hook.beforeReadComplexUserHooks {
		err := (*hook)(env, &input, auth)
		if err != nil {
			respondWithError(w, err.Error(), err.statusCode, metric.RequestReadComplexUser)
			return
		}
	}

	timer := prometheus.NewTimer(metric.DatabaseRequestDuration.WithLabelValues(metric.RequestReadComplexUser))
	complexUser, err := env.dao.ReadComplexUser(input)
	timer.ObserveDuration()
	if err != nil {
		switch err.(type) {
		case dao.ErrComplexUserNotFound:
			respondWithError(w, err.Error(), http.StatusNotFound, metric.RequestReadComplexUser)
		default:
			respondWithError(w, fmt.Sprintf("Something went wrong: %s", err.Error()), http.StatusInternalServerError, metric.RequestReadComplexUser)
		}
		return
	}

	for _, hook := range env.hook.afterReadComplexUserHooks {
		err := (*hook)(env, complexUser, auth)
		if err != nil {
			respondWithError(w, err.Error(), err.statusCode, metric.RequestReadComplexUser)
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

	metric.RequestSuccess.WithLabelValues(metric.RequestReadComplexUser).Inc()
}

func (env *env) updateComplexUserHandler(w http.ResponseWriter, r *http.Request) {
	auth, err := util.ExtractAuthIDFromRequest(r.Header)
	if err != nil {
		respondWithError(w, fmt.Sprintf("Could not authorize request: %s", err.Error()), http.StatusUnauthorized, metric.RequestUpdateComplexUser)
		return
	}

	complexUserID, err := util.ExtractIDFromRequest(mux.Vars(r))
	if err != nil {
		respondWithError(w, err.Error(), http.StatusBadRequest, metric.RequestUpdateComplexUser)
		return
	}

	if auth.ID != complexUserID {
		respondWithError(w, "Unauthorized", http.StatusUnauthorized, metric.RequestUpdateComplexUser)
		return
	}

	var req updateComplexUserRequest
	err = json.NewDecoder(r.Body).Decode(&req)
	if err != nil {
		respondWithError(w, fmt.Sprintf("Invalid request parameters: %s", err.Error()), http.StatusBadRequest, metric.RequestUpdateComplexUser)
		return
	}

	if req.SmallIntField == nil || req.IntField == nil || req.BigIntField == nil || req.FloatField == nil || req.DoubleField == nil || req.StringField == nil || req.BoundedStringField == nil || req.BoolField == nil || req.DateField == nil || req.TimeField == nil || req.DateTimeField == nil || req.BlobField == nil {
		respondWithError(w, "Missing request parameter(s)", http.StatusBadRequest, metric.RequestUpdateComplexUser)
		return
	}

	_, err = valid.ValidateStruct(req)
	if err != nil {
		respondWithError(w, fmt.Sprintf("Invalid request parameters: %s", err.Error()), http.StatusBadRequest, metric.RequestUpdateComplexUser)
		return
	}

	dateField, err := time.Parse("2006-01-02", *req.DateField)
	if err != nil {
		respondWithError(w, fmt.Sprintf("Invalid date string: %s", err.Error()), http.StatusBadRequest, metric.RequestUpdateComplexUser)
		return
	}

	timeField, err := time.Parse("15:04:05.999999999", *req.TimeField)
	if err != nil {
		respondWithError(w, fmt.Sprintf("Invalid time string: %s", err.Error()), http.StatusBadRequest, metric.RequestUpdateComplexUser)
		return
	}

	dateTimeField, err := time.Parse(time.RFC3339Nano, *req.DateTimeField)
	if err != nil {
		respondWithError(w, fmt.Sprintf("Invalid datetime string: %s", err.Error()), http.StatusBadRequest, metric.RequestUpdateComplexUser)
		return
	}

	blobField, err := base64.StdEncoding.DecodeString(*req.BlobField)
	if err != nil {
		respondWithError(w, fmt.Sprintf("Invalid request parameters: %s", err.Error()), http.StatusBadRequest, metric.RequestUpdateComplexUser)
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

	for _, hook := range env.hook.beforeUpdateComplexUserHooks {
		err := (*hook)(env, req, &input, auth)
		if err != nil {
			respondWithError(w, err.Error(), err.statusCode, metric.RequestUpdateComplexUser)
			return
		}
	}

	timer := prometheus.NewTimer(metric.DatabaseRequestDuration.WithLabelValues(metric.RequestUpdateComplexUser))
	complexUser, err := env.dao.UpdateComplexUser(input)
	timer.ObserveDuration()
	if err != nil {
		switch err.(type) {
		case dao.ErrComplexUserNotFound:
			respondWithError(w, err.Error(), http.StatusNotFound, metric.RequestUpdateComplexUser)
		default:
			respondWithError(w, fmt.Sprintf("Something went wrong: %s", err.Error()), http.StatusInternalServerError, metric.RequestUpdateComplexUser)
		}
		return
	}

	for _, hook := range env.hook.afterUpdateComplexUserHooks {
		err := (*hook)(env, complexUser, auth)
		if err != nil {
			respondWithError(w, err.Error(), err.statusCode, metric.RequestUpdateComplexUser)
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

	metric.RequestSuccess.WithLabelValues(metric.RequestUpdateComplexUser).Inc()
}

func (env *env) deleteComplexUserHandler(w http.ResponseWriter, r *http.Request) {
	auth, err := util.ExtractAuthIDFromRequest(r.Header)
	if err != nil {
		respondWithError(w, fmt.Sprintf("Could not authorize request: %s", err.Error()), http.StatusUnauthorized, metric.RequestDeleteComplexUser)
		return
	}

	complexUserID, err := util.ExtractIDFromRequest(mux.Vars(r))
	if err != nil {
		respondWithError(w, err.Error(), http.StatusBadRequest, metric.RequestDeleteComplexUser)
		return
	}

	if auth.ID != complexUserID {
		respondWithError(w, "Unauthorized", http.StatusUnauthorized, metric.RequestDeleteComplexUser)
		return
	}

	input := dao.DeleteComplexUserInput{
		ID: complexUserID,
	}

	for _, hook := range env.hook.beforeDeleteComplexUserHooks {
		err := (*hook)(env, &input, auth)
		if err != nil {
			respondWithError(w, err.Error(), err.statusCode, metric.RequestDeleteComplexUser)
			return
		}
	}

	timer := prometheus.NewTimer(metric.DatabaseRequestDuration.WithLabelValues(metric.RequestDeleteComplexUser))
	err = env.dao.DeleteComplexUser(input)
	timer.ObserveDuration()
	if err != nil {
		switch err.(type) {
		case dao.ErrComplexUserNotFound:
			respondWithError(w, err.Error(), http.StatusNotFound, metric.RequestDeleteComplexUser)
		default:
			respondWithError(w, fmt.Sprintf("Something went wrong: %s", err.Error()), http.StatusInternalServerError, metric.RequestDeleteComplexUser)
		}
		return
	}

	for _, hook := range env.hook.afterDeleteComplexUserHooks {
		err := (*hook)(env, auth)
		if err != nil {
			respondWithError(w, err.Error(), err.statusCode, metric.RequestDeleteComplexUser)
			return
		}
	}

	json.NewEncoder(w).Encode(struct{}{})

	metric.RequestSuccess.WithLabelValues(metric.RequestDeleteComplexUser).Inc()
}

func (env *env) identifyComplexUserHandler(w http.ResponseWriter, r *http.Request) {
	auth, err := util.ExtractAuthIDFromRequest(r.Header)
	if err != nil {
		respondWithError(w, fmt.Sprintf("Could not authorize request: %s", err.Error()), http.StatusUnauthorized, metric.RequestIdentifyComplexUser)
		return
	}

	input := dao.IdentifyComplexUserInput{
		ID: auth.ID,
	}

	for _, hook := range env.hook.beforeIdentifyComplexUserHooks {
		err := (*hook)(env, &input, auth)
		if err != nil {
			respondWithError(w, err.Error(), err.statusCode, metric.RequestIdentifyComplexUser)
			return
		}
	}

	timer := prometheus.NewTimer(metric.DatabaseRequestDuration.WithLabelValues(metric.RequestIdentifyComplexUser))
	complexUser, err := env.dao.IdentifyComplexUser(input)
	timer.ObserveDuration()
	if err != nil {
		switch err.(type) {
		case dao.ErrComplexUserNotFound:
			respondWithError(w, err.Error(), http.StatusNotFound, metric.RequestIdentifyComplexUser)
		default:
			respondWithError(w, fmt.Sprintf("Something went wrong: %s", err.Error()), http.StatusInternalServerError, metric.RequestIdentifyComplexUser)
		}
		return
	}

	for _, hook := range env.hook.afterIdentifyComplexUserHooks {
		err := (*hook)(env, complexUser, auth)
		if err != nil {
			respondWithError(w, err.Error(), err.statusCode, metric.RequestIdentifyComplexUser)
			return
		}
	}

	url := fmt.Sprintf("http://%s:%s/api%s/%s", r.Header.Get("X-Forwarded-Host"), r.Header.Get("X-Forwarded-Port"), r.URL.Path, complexUser.ID)
	w.Header().Set("Location", url)
	w.WriteHeader(http.StatusFound)

	metric.RequestSuccess.WithLabelValues(metric.RequestIdentifyComplexUser).Inc()
}

func (env *env) createTempleUserHandler(w http.ResponseWriter, r *http.Request) {
	auth, err := util.ExtractAuthIDFromRequest(r.Header)
	if err != nil {
		respondWithError(w, fmt.Sprintf("Could not authorize request: %s", err.Error()), http.StatusUnauthorized, metric.RequestCreateTempleUser)
		return
	}

	complexUserID, err := util.ExtractParentIDFromRequest(mux.Vars(r))
	if err != nil {
		respondWithError(w, err.Error(), http.StatusBadRequest, metric.RequestCreateTempleUser)
		return
	}

	authorized, err := checkAuthorization(env, complexUserID, auth)
	if err != nil {
		switch err.(type) {
		case dao.ErrComplexUserNotFound:
			respondWithError(w, "Unauthorized", http.StatusUnauthorized, metric.RequestCreateTempleUser)
		default:
			respondWithError(w, fmt.Sprintf("Something went wrong: %s", err.Error()), http.StatusInternalServerError, metric.RequestCreateTempleUser)
		}
		return
	}
	if !authorized {
		respondWithError(w, "Unauthorized", http.StatusUnauthorized, metric.RequestCreateTempleUser)
		return
	}

	var req createTempleUserRequest
	err = json.NewDecoder(r.Body).Decode(&req)
	if err != nil {
		respondWithError(w, fmt.Sprintf("Invalid request parameters: %s", err.Error()), http.StatusBadRequest, metric.RequestCreateTempleUser)
		return
	}

	if req.IntField == nil || req.DoubleField == nil || req.StringField == nil || req.BoolField == nil || req.DateField == nil || req.TimeField == nil || req.DateTimeField == nil || req.BlobField == nil {
		respondWithError(w, "Missing request parameter(s)", http.StatusBadRequest, metric.RequestCreateTempleUser)
		return
	}

	_, err = valid.ValidateStruct(req)
	if err != nil {
		respondWithError(w, fmt.Sprintf("Invalid request parameters: %s", err.Error()), http.StatusBadRequest, metric.RequestCreateTempleUser)
		return
	}

	dateField, err := time.Parse("2006-01-02", *req.DateField)
	if err != nil {
		respondWithError(w, fmt.Sprintf("Invalid date string: %s", err.Error()), http.StatusBadRequest, metric.RequestCreateTempleUser)
		return
	}

	timeField, err := time.Parse("15:04:05.999999999", *req.TimeField)
	if err != nil {
		respondWithError(w, fmt.Sprintf("Invalid time string: %s", err.Error()), http.StatusBadRequest, metric.RequestCreateTempleUser)
		return
	}

	dateTimeField, err := time.Parse(time.RFC3339Nano, *req.DateTimeField)
	if err != nil {
		respondWithError(w, fmt.Sprintf("Invalid datetime string: %s", err.Error()), http.StatusBadRequest, metric.RequestCreateTempleUser)
		return
	}

	blobField, err := base64.StdEncoding.DecodeString(*req.BlobField)
	if err != nil {
		respondWithError(w, fmt.Sprintf("Invalid request parameters: %s", err.Error()), http.StatusBadRequest, metric.RequestCreateTempleUser)
		return
	}

	uuid, err := uuid.NewUUID()
	if err != nil {
		respondWithError(w, fmt.Sprintf("Could not create UUID: %s", err.Error()), http.StatusInternalServerError, metric.RequestCreateTempleUser)
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
		err := (*hook)(env, req, &input, auth)
		if err != nil {
			respondWithError(w, err.Error(), err.statusCode, metric.RequestCreateTempleUser)
			return
		}
	}

	timer := prometheus.NewTimer(metric.DatabaseRequestDuration.WithLabelValues(metric.RequestCreateTempleUser))
	templeUser, err := env.dao.CreateTempleUser(input)
	timer.ObserveDuration()
	if err != nil {
		respondWithError(w, fmt.Sprintf("Something went wrong: %s", err.Error()), http.StatusInternalServerError, metric.RequestCreateTempleUser)
		return
	}

	for _, hook := range env.hook.afterCreateTempleUserHooks {
		err := (*hook)(env, templeUser, auth)
		if err != nil {
			respondWithError(w, err.Error(), err.statusCode, metric.RequestCreateTempleUser)
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

	metric.RequestSuccess.WithLabelValues(metric.RequestCreateTempleUser).Inc()
}

func (env *env) readTempleUserHandler(w http.ResponseWriter, r *http.Request) {
	auth, err := util.ExtractAuthIDFromRequest(r.Header)
	if err != nil {
		respondWithError(w, fmt.Sprintf("Could not authorize request: %s", err.Error()), http.StatusUnauthorized, metric.RequestReadTempleUser)
		return
	}

	templeUserID, err := util.ExtractIDFromRequest(mux.Vars(r))
	if err != nil {
		respondWithError(w, err.Error(), http.StatusBadRequest, metric.RequestReadTempleUser)
		return
	}

	complexUserID, err := util.ExtractParentIDFromRequest(mux.Vars(r))
	if err != nil {
		respondWithError(w, err.Error(), http.StatusBadRequest, metric.RequestReadTempleUser)
		return
	}

	correctParent, err := checkTempleUserParent(env, templeUserID, complexUserID)
	if err != nil {
		switch err.(type) {
		case dao.ErrTempleUserNotFound:
			respondWithError(w, "Not Found", http.StatusNotFound, metric.RequestReadTempleUser)
		default:
			respondWithError(w, fmt.Sprintf("Something went wrong: %s", err.Error()), http.StatusInternalServerError, metric.RequestReadTempleUser)
		}
		return
	}
	if !correctParent {
		respondWithError(w, "Not Found", http.StatusNotFound, metric.RequestReadTempleUser)
		return
	}

	authorized, err := checkAuthorization(env, complexUserID, auth)
	if err != nil {
		switch err.(type) {
		case dao.ErrComplexUserNotFound:
			respondWithError(w, "Unauthorized", http.StatusUnauthorized, metric.RequestReadTempleUser)
		default:
			respondWithError(w, fmt.Sprintf("Something went wrong: %s", err.Error()), http.StatusInternalServerError, metric.RequestReadTempleUser)
		}
		return
	}
	if !authorized {
		respondWithError(w, "Unauthorized", http.StatusUnauthorized, metric.RequestReadTempleUser)
		return
	}

	input := dao.ReadTempleUserInput{
		ID: templeUserID,
	}

	for _, hook := range env.hook.beforeReadTempleUserHooks {
		err := (*hook)(env, &input, auth)
		if err != nil {
			respondWithError(w, err.Error(), err.statusCode, metric.RequestReadTempleUser)
			return
		}
	}

	timer := prometheus.NewTimer(metric.DatabaseRequestDuration.WithLabelValues(metric.RequestReadTempleUser))
	templeUser, err := env.dao.ReadTempleUser(input)
	timer.ObserveDuration()
	if err != nil {
		switch err.(type) {
		case dao.ErrTempleUserNotFound:
			respondWithError(w, err.Error(), http.StatusNotFound, metric.RequestReadTempleUser)
		default:
			respondWithError(w, fmt.Sprintf("Something went wrong: %s", err.Error()), http.StatusInternalServerError, metric.RequestReadTempleUser)
		}
		return
	}

	for _, hook := range env.hook.afterReadTempleUserHooks {
		err := (*hook)(env, templeUser, auth)
		if err != nil {
			respondWithError(w, err.Error(), err.statusCode, metric.RequestReadTempleUser)
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

	metric.RequestSuccess.WithLabelValues(metric.RequestReadTempleUser).Inc()
}

func (env *env) updateTempleUserHandler(w http.ResponseWriter, r *http.Request) {
	auth, err := util.ExtractAuthIDFromRequest(r.Header)
	if err != nil {
		respondWithError(w, fmt.Sprintf("Could not authorize request: %s", err.Error()), http.StatusUnauthorized, metric.RequestUpdateTempleUser)
		return
	}

	templeUserID, err := util.ExtractIDFromRequest(mux.Vars(r))
	if err != nil {
		respondWithError(w, err.Error(), http.StatusBadRequest, metric.RequestUpdateTempleUser)
		return
	}

	complexUserID, err := util.ExtractParentIDFromRequest(mux.Vars(r))
	if err != nil {
		respondWithError(w, err.Error(), http.StatusBadRequest, metric.RequestUpdateTempleUser)
		return
	}

	correctParent, err := checkTempleUserParent(env, templeUserID, complexUserID)
	if err != nil {
		switch err.(type) {
		case dao.ErrTempleUserNotFound:
			respondWithError(w, "Not Found", http.StatusNotFound, metric.RequestUpdateTempleUser)
		default:
			respondWithError(w, fmt.Sprintf("Something went wrong: %s", err.Error()), http.StatusInternalServerError, metric.RequestUpdateTempleUser)
		}
		return
	}
	if !correctParent {
		respondWithError(w, "Not Found", http.StatusNotFound, metric.RequestUpdateTempleUser)
		return
	}

	authorized, err := checkAuthorization(env, complexUserID, auth)
	if err != nil {
		switch err.(type) {
		case dao.ErrComplexUserNotFound:
			respondWithError(w, "Unauthorized", http.StatusUnauthorized, metric.RequestUpdateTempleUser)
		default:
			respondWithError(w, fmt.Sprintf("Something went wrong: %s", err.Error()), http.StatusInternalServerError, metric.RequestUpdateTempleUser)
		}
		return
	}
	if !authorized {
		respondWithError(w, "Unauthorized", http.StatusUnauthorized, metric.RequestUpdateTempleUser)
		return
	}

	var req updateTempleUserRequest
	err = json.NewDecoder(r.Body).Decode(&req)
	if err != nil {
		respondWithError(w, fmt.Sprintf("Invalid request parameters: %s", err.Error()), http.StatusBadRequest, metric.RequestUpdateTempleUser)
		return
	}

	if req.IntField == nil || req.DoubleField == nil || req.StringField == nil || req.BoolField == nil || req.DateField == nil || req.TimeField == nil || req.DateTimeField == nil || req.BlobField == nil {
		respondWithError(w, "Missing request parameter(s)", http.StatusBadRequest, metric.RequestUpdateTempleUser)
		return
	}

	_, err = valid.ValidateStruct(req)
	if err != nil {
		respondWithError(w, fmt.Sprintf("Invalid request parameters: %s", err.Error()), http.StatusBadRequest, metric.RequestUpdateTempleUser)
		return
	}

	dateField, err := time.Parse("2006-01-02", *req.DateField)
	if err != nil {
		respondWithError(w, fmt.Sprintf("Invalid date string: %s", err.Error()), http.StatusBadRequest, metric.RequestUpdateTempleUser)
		return
	}

	timeField, err := time.Parse("15:04:05.999999999", *req.TimeField)
	if err != nil {
		respondWithError(w, fmt.Sprintf("Invalid time string: %s", err.Error()), http.StatusBadRequest, metric.RequestUpdateTempleUser)
		return
	}

	dateTimeField, err := time.Parse(time.RFC3339Nano, *req.DateTimeField)
	if err != nil {
		respondWithError(w, fmt.Sprintf("Invalid datetime string: %s", err.Error()), http.StatusBadRequest, metric.RequestUpdateTempleUser)
		return
	}

	blobField, err := base64.StdEncoding.DecodeString(*req.BlobField)
	if err != nil {
		respondWithError(w, fmt.Sprintf("Invalid request parameters: %s", err.Error()), http.StatusBadRequest, metric.RequestUpdateTempleUser)
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
		err := (*hook)(env, req, &input, auth)
		if err != nil {
			respondWithError(w, err.Error(), err.statusCode, metric.RequestUpdateTempleUser)
			return
		}
	}

	timer := prometheus.NewTimer(metric.DatabaseRequestDuration.WithLabelValues(metric.RequestUpdateTempleUser))
	templeUser, err := env.dao.UpdateTempleUser(input)
	timer.ObserveDuration()
	if err != nil {
		switch err.(type) {
		case dao.ErrTempleUserNotFound:
			respondWithError(w, err.Error(), http.StatusNotFound, metric.RequestUpdateTempleUser)
		default:
			respondWithError(w, fmt.Sprintf("Something went wrong: %s", err.Error()), http.StatusInternalServerError, metric.RequestUpdateTempleUser)
		}
		return
	}

	for _, hook := range env.hook.afterUpdateTempleUserHooks {
		err := (*hook)(env, templeUser, auth)
		if err != nil {
			respondWithError(w, err.Error(), err.statusCode, metric.RequestUpdateTempleUser)
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

	metric.RequestSuccess.WithLabelValues(metric.RequestUpdateTempleUser).Inc()
}

func (env *env) deleteTempleUserHandler(w http.ResponseWriter, r *http.Request) {
	auth, err := util.ExtractAuthIDFromRequest(r.Header)
	if err != nil {
		respondWithError(w, fmt.Sprintf("Could not authorize request: %s", err.Error()), http.StatusUnauthorized, metric.RequestDeleteTempleUser)
		return
	}

	templeUserID, err := util.ExtractIDFromRequest(mux.Vars(r))
	if err != nil {
		respondWithError(w, err.Error(), http.StatusBadRequest, metric.RequestDeleteTempleUser)
		return
	}

	complexUserID, err := util.ExtractParentIDFromRequest(mux.Vars(r))
	if err != nil {
		respondWithError(w, err.Error(), http.StatusBadRequest, metric.RequestDeleteTempleUser)
		return
	}

	correctParent, err := checkTempleUserParent(env, templeUserID, complexUserID)
	if err != nil {
		switch err.(type) {
		case dao.ErrTempleUserNotFound:
			respondWithError(w, "Not Found", http.StatusNotFound, metric.RequestDeleteTempleUser)
		default:
			respondWithError(w, fmt.Sprintf("Something went wrong: %s", err.Error()), http.StatusInternalServerError, metric.RequestDeleteTempleUser)
		}
		return
	}
	if !correctParent {
		respondWithError(w, "Not Found", http.StatusNotFound, metric.RequestDeleteTempleUser)
		return
	}

	authorized, err := checkAuthorization(env, complexUserID, auth)
	if err != nil {
		switch err.(type) {
		case dao.ErrComplexUserNotFound:
			respondWithError(w, "Unauthorized", http.StatusUnauthorized, metric.RequestDeleteTempleUser)
		default:
			respondWithError(w, fmt.Sprintf("Something went wrong: %s", err.Error()), http.StatusInternalServerError, metric.RequestDeleteTempleUser)
		}
		return
	}
	if !authorized {
		respondWithError(w, "Unauthorized", http.StatusUnauthorized, metric.RequestDeleteTempleUser)
		return
	}

	input := dao.DeleteTempleUserInput{
		ID: templeUserID,
	}

	for _, hook := range env.hook.beforeDeleteTempleUserHooks {
		err := (*hook)(env, &input, auth)
		if err != nil {
			respondWithError(w, err.Error(), err.statusCode, metric.RequestDeleteTempleUser)
			return
		}
	}

	timer := prometheus.NewTimer(metric.DatabaseRequestDuration.WithLabelValues(metric.RequestDeleteTempleUser))
	err = env.dao.DeleteTempleUser(input)
	timer.ObserveDuration()
	if err != nil {
		switch err.(type) {
		case dao.ErrTempleUserNotFound:
			respondWithError(w, err.Error(), http.StatusNotFound, metric.RequestDeleteTempleUser)
		default:
			respondWithError(w, fmt.Sprintf("Something went wrong: %s", err.Error()), http.StatusInternalServerError, metric.RequestDeleteTempleUser)
		}
		return
	}

	for _, hook := range env.hook.afterDeleteTempleUserHooks {
		err := (*hook)(env, auth)
		if err != nil {
			respondWithError(w, err.Error(), err.statusCode, metric.RequestDeleteTempleUser)
			return
		}
	}

	json.NewEncoder(w).Encode(struct{}{})

	metric.RequestSuccess.WithLabelValues(metric.RequestDeleteTempleUser).Inc()
}
