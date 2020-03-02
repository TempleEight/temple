package main

import (
	"flag"
	"log"
	"net/http"

	matchComm "github.com/TempleEight/spec-golang/match/comm"
	matchDAO "github.com/TempleEight/spec-golang/match/dao"
	"github.com/TempleEight/spec-golang/match/util"
	valid "github.com/asaskevich/govalidator"
	"github.com/gorilla/mux"
)

var dao matchDAO.DAO
var comm matchComm.Handler

func main() {
	configPtr := flag.String("config", "/etc/match-service/config.json", "configuration filepath")
	flag.Parse()

	// Require all struct fields by default
	valid.SetFieldsRequiredByDefault(true)

	config, err := util.GetConfig(*configPtr)
	if err != nil {
		log.Fatal(err)
	}

	dao = matchDAO.DAO{}
	err = dao.Init(config)
	if err != nil {
		log.Fatal(err)
	}

	comm = matchComm.Handler{}
	comm.Init(config)

	r := mux.NewRouter()
	// Mux directs to first matching route, i.e. the order matters
	r.HandleFunc("/match/all", matchListHandler).Methods(http.MethodGet)
	r.HandleFunc("/match", matchCreateHandler).Methods(http.MethodPost)
	r.HandleFunc("/match/{id}", matchReadHandler).Methods(http.MethodGet)
	r.HandleFunc("/match/{id}", matchUpdateHandler).Methods(http.MethodPut)
	r.HandleFunc("/match/{id}", matchDeleteHandler).Methods(http.MethodDelete)
	r.Use(jsonMiddleware)

	log.Fatal(http.ListenAndServe(":81", r))
}

func jsonMiddleware(next http.Handler) http.Handler {
	return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		// All responses are JSON, set header accordingly
		w.Header().Set("Content-Type", "application/json")
		next.ServeHTTP(w, r)
	})
}

func matchListHandler(w http.ResponseWriter, r *http.Request) {}

func matchCreateHandler(w http.ResponseWriter, r *http.Request) {}

func matchReadHandler(w http.ResponseWriter, r *http.Request) {}

func matchUpdateHandler(w http.ResponseWriter, r *http.Request) {}

func matchDeleteHandler(w http.ResponseWriter, r *http.Request) {}
