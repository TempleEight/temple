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
	"github.com/prometheus/client_golang/prometheus/promhttp"
)

// env defines the environment that requests should be executed within
type env struct {
	dao  dao.Datastore
	hook Hook
}

// createSimpleTempleTestGroupResponse contains a newly created simpleTempleTestGroup to be returned to the client
type createSimpleTempleTestGroupResponse struct {
	ID uuid.UUID `json:"id"`
}

// readSimpleTempleTestGroupResponse contains a single simpleTempleTestGroup to be returned to the client
type readSimpleTempleTestGroupResponse struct {
	ID uuid.UUID `json:"id"`
}

// defaultRouter generates a router for this service
func defaultRouter(env *env) *mux.Router {
	r := mux.NewRouter()
	// Mux directs to first matching route, i.e. the order matters
	r.HandleFunc("/simple-temple-test-group", env.createSimpleTempleTestGroupHandler).Methods(http.MethodPost)
	r.HandleFunc("/simple-temple-test-group/{id}", env.readSimpleTempleTestGroupHandler).Methods(http.MethodGet)
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

	log.Fatal(http.ListenAndServe(":1030", router))
}

func jsonMiddleware(next http.Handler) http.Handler {
	return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		// All responses are JSON, set header accordingly
		w.Header().Set("Content-Type", "application/json")
		next.ServeHTTP(w, r)
	})
}

func checkAuthorization(env *env, simpleTempleTestGroupID uuid.UUID, auth *util.Auth) (bool, error) {
	input := dao.ReadSimpleTempleTestGroupInput{
		ID: simpleTempleTestGroupID,
	}
	simpleTempleTestGroup, err := env.dao.ReadSimpleTempleTestGroup(input)
	if err != nil {
		return false, err
	}
	return simpleTempleTestGroup.CreatedBy == auth.ID, nil
}

func (env *env) createSimpleTempleTestGroupHandler(w http.ResponseWriter, r *http.Request) {
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

	input := dao.CreateSimpleTempleTestGroupInput{
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

	simpleTempleTestGroup, err := env.dao.CreateSimpleTempleTestGroup(input)
	if err != nil {
		errMsg := util.CreateErrorJSON(fmt.Sprintf("Something went wrong: %s", err.Error()))
		http.Error(w, errMsg, http.StatusInternalServerError)
		return
	}

	for _, hook := range env.hook.afterCreateHooks {
		err := (*hook)(env, simpleTempleTestGroup)
		if err != nil {
			// TODO
			return
		}
	}

	json.NewEncoder(w).Encode(createSimpleTempleTestGroupResponse{
		ID: simpleTempleTestGroup.ID,
	})
}

func (env *env) readSimpleTempleTestGroupHandler(w http.ResponseWriter, r *http.Request) {
	auth, err := util.ExtractAuthIDFromRequest(r.Header)
	if err != nil {
		errMsg := util.CreateErrorJSON(fmt.Sprintf("Could not authorize request: %s", err.Error()))
		http.Error(w, errMsg, http.StatusUnauthorized)
		return
	}

	simpleTempleTestGroupID, err := util.ExtractIDFromRequest(mux.Vars(r))
	if err != nil {
		http.Error(w, util.CreateErrorJSON(err.Error()), http.StatusBadRequest)
		return
	}

	authorized, err := checkAuthorization(env, simpleTempleTestGroupID, auth)
	if err != nil {
		switch err.(type) {
		case dao.ErrSimpleTempleTestGroupNotFound:
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

	input := dao.ReadSimpleTempleTestGroupInput{
		ID: simpleTempleTestGroupID,
	}

	for _, hook := range env.hook.beforeReadHooks {
		err := (*hook)(env, &input)
		if err != nil {
			// TODO
			return
		}
	}

	simpleTempleTestGroup, err := env.dao.ReadSimpleTempleTestGroup(input)
	if err != nil {
		switch err.(type) {
		case dao.ErrSimpleTempleTestGroupNotFound:
			http.Error(w, util.CreateErrorJSON(err.Error()), http.StatusNotFound)
		default:
			errMsg := util.CreateErrorJSON(fmt.Sprintf("Something went wrong: %s", err.Error()))
			http.Error(w, errMsg, http.StatusInternalServerError)
		}
		return
	}

	for _, hook := range env.hook.afterReadHooks {
		err := (*hook)(env, simpleTempleTestGroup)
		if err != nil {
			// TODO
			return
		}
	}

	json.NewEncoder(w).Encode(readSimpleTempleTestGroupResponse{
		ID: simpleTempleTestGroup.ID,
	})
}

func (env *env) deleteSimpleTempleTestGroupHandler(w http.ResponseWriter, r *http.Request) {
	auth, err := util.ExtractAuthIDFromRequest(r.Header)
	if err != nil {
		errMsg := util.CreateErrorJSON(fmt.Sprintf("Could not authorize request: %s", err.Error()))
		http.Error(w, errMsg, http.StatusUnauthorized)
		return
	}

	simpleTempleTestGroupID, err := util.ExtractIDFromRequest(mux.Vars(r))
	if err != nil {
		http.Error(w, util.CreateErrorJSON(err.Error()), http.StatusBadRequest)
		return
	}

	authorized, err := checkAuthorization(env, simpleTempleTestGroupID, auth)
	if err != nil {
		switch err.(type) {
		case dao.ErrSimpleTempleTestGroupNotFound:
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

	input := dao.DeleteSimpleTempleTestGroupInput{
		ID: simpleTempleTestGroupID,
	}

	for _, hook := range env.hook.beforeDeleteHooks {
		err := (*hook)(env, &input)
		if err != nil {
			// TODO
			return
		}
	}

	err = env.dao.DeleteSimpleTempleTestGroup(input)
	if err != nil {
		switch err.(type) {
		case dao.ErrSimpleTempleTestGroupNotFound:
			http.Error(w, util.CreateErrorJSON(err.Error()), http.StatusNotFound)
		default:
			errMsg := util.CreateErrorJSON(fmt.Sprintf("Something went wrong: %s", err.Error()))
			http.Error(w, errMsg, http.StatusInternalServerError)
		}
		return
	}

	for _, hook := range env.hook.afterDeleteHooks {
		err := (*hook)(env)
		if err != nil {
			// TODO
			return
		}
	}

	json.NewEncoder(w).Encode(struct{}{})
}
