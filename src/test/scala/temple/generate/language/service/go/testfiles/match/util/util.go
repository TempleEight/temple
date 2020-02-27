package util

import (
	"encoding/json"
	"errors"
	"os"
	"strconv"
)

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

// ExtractIDFromRequest extracts the parameter provided under parameter ID and converts it into an integer
func ExtractIDFromRequest(requestParams map[string]string) (int64, error) {
	idStr := requestParams["id"]
	if len(idStr) == 0 {
		return 0, errors.New("No ID provided")
	}

	id, err := strconv.ParseInt(idStr, 10, 64)
	if err != nil {
		return 0, errors.New("Invalid ID provided")
	}

	return id, nil
}
