package comm

import (
	"encoding/json"
	"errors"
	"fmt"
	"io/ioutil"
	"net/http"
	"net/url"

	"github.com/TempleEight/spec-golang/auth/util"
)

// Comm provides the interface adopted by Handler, allowing for mocking
type Comm interface {
	CreateJWTCredential() (*JWTCredential, error)
}

// Handler maintains the list of services and their associated hostnames
type Handler struct {
	Services map[string]string
}

// JWTCredential store the issuer and secret key that must be used to sign requests
type JWTCredential struct {
	Key    string `json:"key"`
	Secret string `json:"secret"`
}

// consumerResponse encapsulates the response from Kong after creating a consumer
type consumerResponse struct {
	ID       string `json:"id"`
	Username string `json:"username"`
}
