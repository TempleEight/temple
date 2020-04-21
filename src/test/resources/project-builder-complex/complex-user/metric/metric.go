package metric

import (
	"github.com/prometheus/client_golang/prometheus"
	"github.com/prometheus/client_golang/prometheus/promauto"
)

var (
	RequestCreateComplexUser   = "create_complex_user"
	RequestReadComplexUser     = "read_complex_user"
	RequestUpdateComplexUser   = "update_complex_user"
	RequestDeleteComplexUser   = "delete_complex_user"
	RequestIdentifyComplexUser = "identify_complex_user"
	RequestCreateTempleUser    = "create_temple_user"
	RequestReadTempleUser      = "read_temple_user"
	RequestUpdateTempleUser    = "update_temple_user"
	RequestDeleteTempleUser    = "delete_temple_user"

	RequestSuccess = promauto.NewCounterVec(prometheus.CounterOpts{
		Name: "complexuser_request_success_total",
		Help: "The total number of successful requests",
	}, []string{"request_type"})

	RequestFailure = promauto.NewCounterVec(prometheus.CounterOpts{
		Name: "complexuser_request_failure_total",
		Help: "The total number of failed requests",
	}, []string{"request_type", "error_code"})

	DatabaseRequestDuration = promauto.NewSummaryVec(prometheus.SummaryOpts{
		Name:       "complexuser_database_request_seconds",
		Help:       "The time spent executing database requests in seconds",
		Objectives: map[float64]float64{0.5: 0.05, 0.9: 0.01, 0.95: 0.005, 0.99: 0.001},
	}, []string{"query_type"})
)
