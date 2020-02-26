package main

import (
	"encoding/json"
	"flag"
	"fmt"
	"log"
	"net/http"

	userDAO "github.com/TempleEight/spec-golang/user/dao"
	"github.com/TempleEight/spec-golang/user/util"
	valid "github.com/asaskevich/govalidator"
	"github.com/gorilla/mux"
)

func main() {
	configPtr := flag.String("config", "/etc/user-service/config.json", "configuration filepath")
	flag.Parse()

	// Require all struct fields by default
	valid.SetFieldsRequiredByDefault(true)

	config, err := utils.GetConfig(*configPtr)
	if err != nil {
		log.Fatal(err)
	}

	dao = userDAO.DAO{}
	err = dao.Init(config)
	if err != nil {
		log.Fatal(err)
	}

	r := mux.NewRouter()
	r.HandleFunc("/user", userCreateHandler).Methods(http.MethodPost)
	r.HandleFunc("/user/{id}", userReadHandler).Methods(http.MethodGet)
	r.HandleFunc("/user/{id}", userUpdateHandler).Methods(http.MethodPut)
	r.HandleFunc("/user/{id}", userDeleteHandler).Methods(http.MethodDelete)
	r.Use(jsonMiddleware)

	log.Fatal(http.ListenAndServe(":80", r))
}

