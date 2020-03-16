package main

import (
	"encoding/json"
	"flag"
	"fmt"
	"log"
	"net/http"
	"time"

	"golang.org/x/crypto/bcrypt"

	"github.com/TempleEight/spec-golang/auth/comm"
	"github.com/TempleEight/spec-golang/auth/dao"
	"github.com/TempleEight/spec-golang/auth/util"
	valid "github.com/asaskevich/govalidator"
	"github.com/dgrijalva/jwt-go"
	"github.com/google/uuid"
	"github.com/gorilla/mux"
)

// env defines the environment that requests should be executed within
type env struct {
	dao           dao.Datastore
	comm          comm.Comm
	jwtCredential *comm.JWTCredential
}

// registerAuthRequest contains the client-provided information required to create a single auth
type registerAuthRequest struct {
	Email    string `valid:"email,required"`
	Password string `valid:"type(string),required,stringlength(8|64)"`
}

// loginAuthRequest contains the client-provided information required to login an existing auth
type loginAuthRequest struct {
	Email    string `valid:"email,required"`
	Password string `valid:"type(string),required,stringlength(8|64)"`
}

// registerAuthResponse contains an access token
type registerAuthResponse struct {
	AccessToken string
}

// loginAuthResponse contains an access token
type loginAuthResponse struct {
	AccessToken string
}

// router generates a router for this service
func (env *env) router() *mux.Router {
	r := mux.NewRouter()
	r.HandleFunc("/auth/register", env.registerAuthHandler).Methods(http.MethodPost)
	r.HandleFunc("/auth/login", env.loginAuthHandler).Methods(http.MethodPost)
	r.Use(jsonMiddleware)
	return r
}

func main() {

}
