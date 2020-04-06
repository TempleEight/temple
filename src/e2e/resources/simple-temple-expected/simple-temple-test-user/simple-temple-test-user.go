package main

import (
	"encoding/json"
	"flag"
	"fmt"
	"log"
	"net/http"
	"time"

	"github.com/squat/and/dab/simple-temple-test-user/dao"
	"github.com/squat/and/dab/simple-temple-test-user/util"
	valid "github.com/asaskevich/govalidator"
	"github.com/google/uuid"
	"github.com/gorilla/mux"
)

// env defines the environment that requests should be executed within
type env struct {
	dao dao.Datastore
}

// createSimpleTempleTestUserRequest contains the client-provided information required to create a single simpleTempleTestUser
type createSimpleTempleTestUserRequest struct {
	SimpleTempleTestUser *string    `valid:"type(string),required"`
	Email                *string    `valid:"type(string),required,stringlength(5|40)"`
	FirstName            *string    `valid:"type(string),required"`
	LastName             *string    `valid:"type(string),required"`
	CreatedAt            *time.Time `valid:"type(string),rfc3339,required"`
	NumberOfDogs         *int32     `valid:"type(int32),required"`
	CurrentBankBalance   *float32   `valid:"type(float32),required"`
	BirthDate            *time.Time `valid:"type(string),required"`
	BreakfastTime        *time.Time `valid:"type(string),required"`
}

// updateSimpleTempleTestUserRequest contains the client-provided information required to update a single simpleTempleTestUser
type updateSimpleTempleTestUserRequest struct {
	SimpleTempleTestUser *string    `valid:"type(string),required"`
	Email                *string    `valid:"type(string),required,stringlength(5|40)"`
	FirstName            *string    `valid:"type(string),required"`
	LastName             *string    `valid:"type(string),required"`
	CreatedAt            *time.Time `valid:"type(string),rfc3339,required"`
	NumberOfDogs         *int32     `valid:"type(int32),required"`
	CurrentBankBalance   *float32   `valid:"type(float32),required"`
	BirthDate            *time.Time `valid:"type(string),required"`
	BreakfastTime        *time.Time `valid:"type(string),required"`
}

// listSimpleTempleTestUserElement contains a single simpleTempleTestUser list element
type listSimpleTempleTestUserElement struct {
	ID                   uuid.UUID
	SimpleTempleTestUser string
	Email                string
	FirstName            string
	LastName             string
	CreatedAt            string
	NumberOfDogs         int32
	CurrentBankBalance   float32
	BirthDate            string
	BreakfastTime        string
}

// listSimpleTempleTestUserResponse contains a single simpleTempleTestUser list to be returned to the client
type listSimpleTempleTestUserResponse struct {
	SimpleTempleTestUserList []listSimpleTempleTestUserElement
}

// createSimpleTempleTestUserResponse contains a newly created simpleTempleTestUser to be returned to the client
type createSimpleTempleTestUserResponse struct {
	ID                   uuid.UUID
	SimpleTempleTestUser string
	Email                string
	FirstName            string
	LastName             string
	CreatedAt            string
	NumberOfDogs         int32
	CurrentBankBalance   float32
	BirthDate            string
	BreakfastTime        string
}

// readSimpleTempleTestUserResponse contains a single simpleTempleTestUser to be returned to the client
type readSimpleTempleTestUserResponse struct {
	ID                   uuid.UUID
	SimpleTempleTestUser string
	Email                string
	FirstName            string
	LastName             string
	CreatedAt            string
	NumberOfDogs         int32
	CurrentBankBalance   float32
	BirthDate            string
	BreakfastTime        string
}

// updateSimpleTempleTestUserResponse contains a newly updated simpleTempleTestUser to be returned to the client
type updateSimpleTempleTestUserResponse struct {
	ID                   uuid.UUID
	SimpleTempleTestUser string
	Email                string
	FirstName            string
	LastName             string
	CreatedAt            string
	NumberOfDogs         int32
	CurrentBankBalance   float32
	BirthDate            string
	BreakfastTime        string
}

// router generates a router for this service
func (env *env) router() *mux.Router {
	r := mux.NewRouter()
	// Mux directs to first matching route, i.e. the order matters
	r.HandleFunc("/simple-temple-test-user/all", env.listSimpleTempleTestUserHandler).Methods(http.MethodGet)
	r.HandleFunc("/simple-temple-test-user", env.createSimpleTempleTestUserHandler).Methods(http.MethodPost)
	r.HandleFunc("/simple-temple-test-user/{id}", env.readSimpleTempleTestUserHandler).Methods(http.MethodGet)
	r.HandleFunc("/simple-temple-test-user/{id}", env.updateSimpleTempleTestUserHandler).Methods(http.MethodPut)
	r.Use(jsonMiddleware)
	return r
}

func main() {
	configPtr := flag.String("config", "/etc/simple-temple-test-user-service/config.json", "configuration filepath")
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

func (env *env) listSimpleTempleTestUserHandler(w http.ResponseWriter, r *http.Request) {
	simpleTempleTestUserList, err := env.dao.ListSimpleTempleTestUser()
	if err != nil {
		errMsg := util.CreateErrorJSON(fmt.Sprintf("Something went wrong: %s", err.Error()))
		http.Error(w, errMsg, http.StatusInternalServerError)
		return
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
			BirthDate:            simpleTempleTestUser.BirthDate,
			BreakfastTime:        simpleTempleTestUser.BreakfastTime,
		})
	}

	json.NewEncoder(w).Encode(simpleTempleTestUserListResp)
}

func (env *env) createSimpleTempleTestUserHandler(w http.ResponseWriter, r *http.Request) {
	auth, err := util.ExtractAuthIDFromRequest(r.Header)
	if err != nil {
		errMsg := util.CreateErrorJSON(fmt.Sprintf("Could not authorize request: %s", err.Error()))
		http.Error(w, errMsg, http.StatusUnauthorized)
		return
	}

	var req createSimpleTempleTestUserRequest
	err = json.NewDecoder(r.Body).Decode(&req)
	if err != nil {
		errMsg := util.CreateErrorJSON(fmt.Sprintf("Invalid request parameters: %s", err.Error()))
		http.Error(w, errMsg, http.StatusBadRequest)
		return
	}

	if req.SimpleTempleTestUser == nil || req.Email == nil || req.FirstName == nil || req.LastName == nil || req.CreatedAt == nil || req.NumberOfDogs == nil || req.CurrentBankBalance == nil || req.BirthDate == nil || req.BreakfastTime == nil {
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

	simpleTempleTestUser, err := env.dao.CreateSimpleTempleTestUser(dao.CreateSimpleTempleTestUserInput{
		ID:                   auth.ID,
		SimpleTempleTestUser: *req.SimpleTempleTestUser,
		Email:                *req.Email,
		FirstName:            *req.FirstName,
		LastName:             *req.LastName,
		CreatedAt:            *req.CreatedAt,
		NumberOfDogs:         *req.NumberOfDogs,
		CurrentBankBalance:   *req.CurrentBankBalance,
		BirthDate:            *req.BirthDate,
		BreakfastTime:        *req.BreakfastTime,
	})
	if err != nil {
		errMsg := util.CreateErrorJSON(fmt.Sprintf("Something went wrong: %s", err.Error()))
		http.Error(w, errMsg, http.StatusInternalServerError)
		return
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
		BirthDate:            simpleTempleTestUser.BirthDate,
		BreakfastTime:        simpleTempleTestUser.BreakfastTime,
	})
}

func (env *env) readSimpleTempleTestUserHandler(w http.ResponseWriter, r *http.Request) {

}

func (env *env) updateSimpleTempleTestUserHandler(w http.ResponseWriter, r *http.Request) {

}
