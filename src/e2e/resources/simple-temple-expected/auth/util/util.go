package util

import (
	"encoding/json"
	"os"
)

// Config encapsulates the service configuration options
type Config struct {
	User     string            `json:"user"`
	DBName   string            `json:"dbName"`
	Host     string            `json:"host"`
	SSLMode  string            `json:"sslMode"`
	Services map[string]string `json:"services"`
	Ports    map[string]int    `json:"ports"`
}

// GetConfig returns a configuration object from decoding the given configuration file
func GetConfig(filePath string) (*Config, error) {
	config := Config{}
	file, err := os.Open(filePath)
	if err != nil {
		return nil, err
	}
	defer file.Close()
	decoder := json.NewDecoder(file)
	err = decoder.Decode(&config)
	if err != nil {
		return nil, err
	}
	return &config, nil
}

// CreateErrorJSON returns a JSON string containing the key error associated with provided value
func CreateErrorJSON(message string) string {
	payload := map[string]string{"error": message}
	json, err := json.Marshal(payload)
	if err != nil {
		return err.Error()
	}
	return string(json)
}
