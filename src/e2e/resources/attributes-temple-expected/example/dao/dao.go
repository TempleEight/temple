package dao

import (
	"database/sql"
	"fmt"

	"github.com/squat/and/dab/example/util"
	"github.com/google/uuid"

	// pq acts as the driver for SQL requests
	_ "github.com/lib/pq"
)

// BaseDatastore provides the basic datastore methods
type BaseDatastore interface {
	CreateExample(input CreateExampleInput) (*Example, error)
	ReadExample(input ReadExampleInput) (*Example, error)
	UpdateExample(input UpdateExampleInput) (*Example, error)
	DeleteExample(input DeleteExampleInput) error
}

// DAO encapsulates access to the datastore
type DAO struct {
	DB *sql.DB
}

// Example encapsulates the object stored in the datastore
type Example struct {
	ID                 uuid.UUID
	ServerAttribute    int32
	ServerSetAttribute bool
}

// CreateExampleInput encapsulates the information required to create a single example in the datastore
type CreateExampleInput struct {
	ID                 uuid.UUID
	ServerAttribute    int32
	ServerSetAttribute bool
}

// ReadExampleInput encapsulates the information required to read a single example in the datastore
type ReadExampleInput struct {
	ID uuid.UUID
}

// UpdateExampleInput encapsulates the information required to update a single example in the datastore
type UpdateExampleInput struct {
	ID                 uuid.UUID
	ServerAttribute    int32
	ServerSetAttribute bool
}

// DeleteExampleInput encapsulates the information required to delete a single example in the datastore
type DeleteExampleInput struct {
	ID uuid.UUID
}

// Init opens the datastore connection, returning a DAO
func Init(config *util.Config) (*DAO, error) {
	connStr := fmt.Sprintf("user=%s dbname=%s host=%s sslmode=%s", config.User, config.DBName, config.Host, config.SSLMode)
	db, err := sql.Open("postgres", connStr)
	if err != nil {
		return nil, err
	}
	return &DAO{db}, nil
}

// Executes a query, returning the row
func executeQueryWithRowResponse(db *sql.DB, query string, args ...interface{}) *sql.Row {
	return db.QueryRow(query, args...)
}

// Executes a query, returning the number of rows affected
func executeQuery(db *sql.DB, query string, args ...interface{}) (int64, error) {
	result, err := db.Exec(query, args...)
	if err != nil {
		return 0, err
	}
	return result.RowsAffected()
}

// CreateExample creates a new example in the datastore, returning the newly created example
func (dao *DAO) CreateExample(input CreateExampleInput) (*Example, error) {
	row := executeQueryWithRowResponse(dao.DB, "INSERT INTO example (id, server_attribute, server_set_attribute) VALUES ($1, $2, $3) RETURNING id, server_attribute, server_set_attribute;", input.ID, input.ServerAttribute, input.ServerSetAttribute)

	var example Example
	err := row.Scan(&example.ID, &example.ServerAttribute, &example.ServerSetAttribute)
	if err != nil {
		return nil, err
	}

	return &example, nil
}

// ReadExample returns the example in the datastore for a given ID
func (dao *DAO) ReadExample(input ReadExampleInput) (*Example, error) {
	row := executeQueryWithRowResponse(dao.DB, "SELECT id, server_attribute, server_set_attribute FROM example WHERE id = $1;", input.ID)

	var example Example
	err := row.Scan(&example.ID, &example.ServerAttribute, &example.ServerSetAttribute)
	if err != nil {
		switch err {
		case sql.ErrNoRows:
			return nil, ErrExampleNotFound(input.ID.String())
		default:
			return nil, err
		}
	}

	return &example, nil
}

// UpdateExample updates the example in the datastore for a given ID, returning the newly updated example
func (dao *DAO) UpdateExample(input UpdateExampleInput) (*Example, error) {
	row := executeQueryWithRowResponse(dao.DB, "UPDATE example SET server_attribute = $1, server_set_attribute = $2 WHERE id = $3 RETURNING id, server_attribute, server_set_attribute;", input.ServerAttribute, input.ServerSetAttribute, input.ID)

	var example Example
	err := row.Scan(&example.ID, &example.ServerAttribute, &example.ServerSetAttribute)
	if err != nil {
		switch err {
		case sql.ErrNoRows:
			return nil, ErrExampleNotFound(input.ID.String())
		default:
			return nil, err
		}
	}

	return &example, nil
}

// DeleteExample deletes the example in the datastore for a given ID
func (dao *DAO) DeleteExample(input DeleteExampleInput) error {
	rowsAffected, err := executeQuery(dao.DB, "DELETE FROM example WHERE id = $1;", input.ID)
	if err != nil {
		return err
	} else if rowsAffected == 0 {
		return ErrExampleNotFound(input.ID.String())
	}

	return nil
}
