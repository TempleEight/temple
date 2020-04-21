package metric

import (
	"github.com/prometheus/client_golang/prometheus"
	"github.com/prometheus/client_golang/prometheus/promauto"
)

var (
	RequestCreateUser = "create_user"
	RequestReadUser   = "read_user"
	RequestUpdateUser = "update_user"
	RequestDeleteUser = "delete_user"

	RequestSuccess = promauto.NewCounterVec(prometheus.CounterOpts{
		Name: "user_request_success_total",
		Help: "The total number of successful requests",
	}, []string{"request_type"})

	RequestFailure = promauto.NewCounterVec(prometheus.CounterOpts{
		Name: "user_request_failure_total",
		Help: "The total number of failed requests",
	}, []string{"request_type", "error_code"})

	DatabaseRequestDuration = promauto.NewSummaryVec(prometheus.SummaryOpts{
		Name:       "user_database_request_seconds",
		Help:       "The time spent executing database requests in seconds",
		Objectives: map[float64]float64{0.5: 0.05, 0.9: 0.01, 0.95: 0.005, 0.99: 0.001},
	}, []string{"query_type"})
)
