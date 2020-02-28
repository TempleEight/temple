package comm

import (
	"fmt"
	"net/http"

	"github.com/TempleEight/spec-golang/match/util"
)

// Handler maintains the list of services and their associated hostnames
type Handler struct {
	Services map[string]string
}

// Init sets up the Handler object with a list of services from the config
func (comm *Handler) Init(config *util.Config) {
	comm.Services = config.Services
}
