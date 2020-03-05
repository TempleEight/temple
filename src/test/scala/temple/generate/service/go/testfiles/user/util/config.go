package util

type Config struct {
	User     string            `json:"user"`
	DBName   string            `json:"dbName"`
	Host     string            `json:"host"`
	SSLMode  string            `json:"sslMode"`
	Services map[string]string `json:"services"`
}
