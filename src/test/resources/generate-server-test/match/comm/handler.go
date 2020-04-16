package comm

import (
	"errors"
	"fmt"
	"net/http"

	"github.com/TempleEight/spec-golang/match/util"
	"github.com/google/uuid"
)

// Comm provides the interface adopted by the Handler, allowing for mocking
type Comm interface {
	CheckUser(userID uuid.UUID, token string) (bool, error)
}

// Handler maintains the list of services and their associated hostnames
type Handler struct {
	Services map[string]string
}

// Init sets up the Handler object with a list of services from the config
func Init(config *util.Config) *Handler {
	return &Handler{config.Services}
}

// CheckUser makes a request to the target service to check if a userID exists
func (comm *Handler) CheckUser(userID uuid.UUID, token string) (bool, error) {
	hostname, ok := comm.Services["user"]
	if !ok {
		return false, errors.New("service user's hostname is not in the config file")
	}

	req, err := http.NewRequest(http.MethodGet, fmt.Sprintf("%s/%s", hostname, userID.String()), nil)
	if err != nil {
		return false, err
	}

	// Token should already be in the form `Bearer <token>`
	req.Header.Set("Authorization", token)
	resp, err := new(http.Client).Do(req)
	if err != nil {
		return false, err
	}
	defer resp.Body.Close()

	return resp.StatusCode == http.StatusOK, nil
}
