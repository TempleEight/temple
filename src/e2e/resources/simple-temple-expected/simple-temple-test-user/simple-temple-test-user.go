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
	dao  dao.Datastore
	hook Hook
}

// createSimpleTempleTestUserRequest contains the client-provided information required to create a single simpleTempleTestUser
type createSimpleTempleTestUserRequest struct {
	SimpleTempleTestUser *string  `json:"simpleTempleTestUser" valid:"type(*string),required"`
	Email                *string  `json:"email" valid:"type(*string),required,stringlength(5|40)"`
	FirstName            *string  `json:"firstName" valid:"type(*string),required"`
	LastName             *string  `json:"lastName" valid:"type(*string),required"`
	CreatedAt            *string  `json:"createdAt" valid:"type(*string),rfc3339,required"`
	NumberOfDogs         *int32   `json:"numberOfDogs" valid:"type(*int32),required"`
	CurrentBankBalance   *float32 `json:"currentBankBalance" valid:"type(*float32),required"`
	BirthDate            *string  `json:"birthDate" valid:"type(*string),required"`
	BreakfastTime        *string  `json:"breakfastTime" valid:"type(*string),required"`
}

// updateSimpleTempleTestUserRequest contains the client-provided information required to update a single simpleTempleTestUser
type updateSimpleTempleTestUserRequest struct {
	SimpleTempleTestUser *string  `json:"simpleTempleTestUser" valid:"type(*string),required"`
	Email                *string  `json:"email" valid:"type(*string),required,stringlength(5|40)"`
	FirstName            *string  `json:"firstName" valid:"type(*string),required"`
	LastName             *string  `json:"lastName" valid:"type(*string),required"`
	CreatedAt            *string  `json:"createdAt" valid:"type(*string),rfc3339,required"`
	NumberOfDogs         *int32   `json:"numberOfDogs" valid:"type(*int32),required"`
	CurrentBankBalance   *float32 `json:"currentBankBalance" valid:"type(*float32),required"`
	BirthDate            *string  `json:"birthDate" valid:"type(*string),required"`
	BreakfastTime        *string  `json:"breakfastTime" valid:"type(*string),required"`
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

// defaultRouter generates a router for this service
func defaultRouter(env *env) *mux.Router {
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
			BirthDate:            simpleTempleTestUser.BirthDate.Format("2006-01-02"),
			BreakfastTime:        simpleTempleTestUser.BreakfastTime.Format("15:04:05.999999999"),
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

	createdAt, err := time.Parse(time.RFC3339Nano, *req.CreatedAt)
	if err != nil {
		errMsg := util.CreateErrorJSON(fmt.Sprintf("Invalid datetime string: %s", err.Error()))
		http.Error(w, errMsg, http.StatusBadRequest)
		return
	}

	birthDate, err := time.Parse("2006-01-02", *req.BirthDate)
	if err != nil {
		errMsg := util.CreateErrorJSON(fmt.Sprintf("Invalid date string: %s", err.Error()))
		http.Error(w, errMsg, http.StatusBadRequest)
		return
	}

	breakfastTime, err := time.Parse("15:04:05.999999999", *req.BreakfastTime)
	if err != nil {
		errMsg := util.CreateErrorJSON(fmt.Sprintf("Invalid time string: %s", err.Error()))
		http.Error(w, errMsg, http.StatusBadRequest)
		return
	}

	simpleTempleTestUser, err := env.dao.CreateSimpleTempleTestUser(dao.CreateSimpleTempleTestUserInput{
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
		BirthDate:            simpleTempleTestUser.BirthDate.Format("2006-01-02"),
		BreakfastTime:        simpleTempleTestUser.BreakfastTime.Format("15:04:05.999999999"),
	})
}

func (env *env) readSimpleTempleTestUserHandler(w http.ResponseWriter, r *http.Request) {
	_, err := util.ExtractAuthIDFromRequest(r.Header)
	if err != nil {
		errMsg := util.CreateErrorJSON(fmt.Sprintf("Could not authorize request: %s", err.Error()))
		http.Error(w, errMsg, http.StatusUnauthorized)
		return
	}

	simpleTempleTestUserID, err := util.ExtractIDFromRequest(mux.Vars(r))
	if err != nil {
		http.Error(w, util.CreateErrorJSON(err.Error()), http.StatusBadRequest)
		return
	}

	simpleTempleTestUser, err := env.dao.ReadSimpleTempleTestUser(dao.ReadSimpleTempleTestUserInput{
		ID: simpleTempleTestUserID,
	})
	if err != nil {
		switch err.(type) {
		case dao.ErrSimpleTempleTestUserNotFound:
			http.Error(w, util.CreateErrorJSON(err.Error()), http.StatusNotFound)
		default:
			errMsg := util.CreateErrorJSON(fmt.Sprintf("Something went wrong: %s", err.Error()))
			http.Error(w, errMsg, http.StatusInternalServerError)
		}
		return
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
}

func (env *env) updateSimpleTempleTestUserHandler(w http.ResponseWriter, r *http.Request) {
	auth, err := util.ExtractAuthIDFromRequest(r.Header)
	if err != nil {
		errMsg := util.CreateErrorJSON(fmt.Sprintf("Could not authorize request: %s", err.Error()))
		http.Error(w, errMsg, http.StatusUnauthorized)
		return
	}

	simpleTempleTestUserID, err := util.ExtractIDFromRequest(mux.Vars(r))
	if err != nil {
		http.Error(w, util.CreateErrorJSON(err.Error()), http.StatusBadRequest)
		return
	}

	if auth.ID != simpleTempleTestUserID {
		errMsg := util.CreateErrorJSON("Unauthorized")
		http.Error(w, errMsg, http.StatusUnauthorized)
		return
	}

	var req updateSimpleTempleTestUserRequest
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

	createdAt, err := time.Parse(time.RFC3339Nano, *req.CreatedAt)
	if err != nil {
		errMsg := util.CreateErrorJSON(fmt.Sprintf("Invalid datetime string: %s", err.Error()))
		http.Error(w, errMsg, http.StatusBadRequest)
		return
	}

	birthDate, err := time.Parse("2006-01-02", *req.BirthDate)
	if err != nil {
		errMsg := util.CreateErrorJSON(fmt.Sprintf("Invalid date string: %s", err.Error()))
		http.Error(w, errMsg, http.StatusBadRequest)
		return
	}

	breakfastTime, err := time.Parse("15:04:05.999999999", *req.BreakfastTime)
	if err != nil {
		errMsg := util.CreateErrorJSON(fmt.Sprintf("Invalid time string: %s", err.Error()))
		http.Error(w, errMsg, http.StatusBadRequest)
		return
	}

	simpleTempleTestUser, err := env.dao.UpdateSimpleTempleTestUser(dao.UpdateSimpleTempleTestUserInput{
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
	})
	if err != nil {
		switch err.(type) {
		case dao.ErrSimpleTempleTestUserNotFound:
			http.Error(w, util.CreateErrorJSON(err.Error()), http.StatusNotFound)
		default:
			errMsg := util.CreateErrorJSON(fmt.Sprintf("Something went wrong: %s", err.Error()))
			http.Error(w, errMsg, http.StatusInternalServerError)
		}
		return
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
}
