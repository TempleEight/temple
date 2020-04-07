package main

import (
	"encoding/json"
	"flag"
	"fmt"
	"log"
	"net/http"

	"github.com/squat/and/dab/simple-temple-test-group/dao"
	"github.com/squat/and/dab/simple-temple-test-group/util"
	valid "github.com/asaskevich/govalidator"
	"github.com/google/uuid"
	"github.com/gorilla/mux"
)

// env defines the environment that requests should be executed within
type env struct {
	dao dao.Datastore
}

// createSimpleTempleTestGroupResponse contains a newly created simpleTempleTestGroup to be returned to the client
type createSimpleTempleTestGroupResponse struct {
	ID uuid.UUID
}

// readSimpleTempleTestGroupResponse contains a single simpleTempleTestGroup to be returned to the client
type readSimpleTempleTestGroupResponse struct {
	ID uuid.UUID
}

// updateSimpleTempleTestGroupResponse contains a newly updated simpleTempleTestGroup to be returned to the client
type updateSimpleTempleTestGroupResponse struct {
	ID uuid.UUID
}

// router generates a router for this service
func (env *env) router() *mux.Router {
	r := mux.NewRouter()
	// Mux directs to first matching route, i.e. the order matters
	r.HandleFunc("/simple-temple-test-group", env.createSimpleTempleTestGroupHandler).Methods(http.MethodPost)
	r.HandleFunc("/simple-temple-test-group/{id}", env.readSimpleTempleTestGroupHandler).Methods(http.MethodGet)
	r.HandleFunc("/simple-temple-test-group/{id}", env.updateSimpleTempleTestGroupHandler).Methods(http.MethodPut)
	r.HandleFunc("/simple-temple-test-group/{id}", env.deleteSimpleTempleTestGroupHandler).Methods(http.MethodDelete)
	r.Use(jsonMiddleware)
	return r
}

func main() {
	configPtr := flag.String("config", "/etc/simple-temple-test-group-service/config.json", "configuration filepath")
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

	log.Fatal(http.ListenAndServe(":1030", env.router()))
}

func jsonMiddleware(next http.Handler) http.Handler {
	return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		// All responses are JSON, set header accordingly
		w.Header().Set("Content-Type", "application/json")
		next.ServeHTTP(w, r)
	})
}

func (env *env) createSimpleTempleTestGroupHandler(w http.ResponseWriter, r *http.Request) {
	auth, err := util.ExtractAuthIDFromRequest(r.Header)
	if err != nil {
		errMsg := util.CreateErrorJSON(fmt.Sprintf("Could not authorize request: %s", err.Error()))
		http.Error(w, errMsg, http.StatusUnauthorized)
		return
	}

	var req createSimpleTempleTestGroupRequest
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

	simpleTempleTestGroup, err := env.dao.CreateSimpleTempleTestGroup(dao.CreateSimpleTempleTestGroupInput{
		ID:     uuid,
		AuthID: auth.ID,
	})
	if err != nil {
		errMsg := util.CreateErrorJSON(fmt.Sprintf("Something went wrong: %s", err.Error()))
		http.Error(w, errMsg, http.StatusInternalServerError)
		return
	}

	json.NewEncoder(w).Encode(createSimpleTempleTestGroupResponse{
		ID: simpleTempleTestGroup.ID,
	})
}

func (env *env) readSimpleTempleTestGroupHandler(w http.ResponseWriter, r *http.Request) {

}

func (env *env) updateSimpleTempleTestGroupHandler(w http.ResponseWriter, r *http.Request) {

}

func (env *env) deleteSimpleTempleTestGroupHandler(w http.ResponseWriter, r *http.Request) {

}
