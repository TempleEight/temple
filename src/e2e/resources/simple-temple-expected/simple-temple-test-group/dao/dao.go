package dao

import (
	"database/sql"
	"fmt"

	"github.com/squat/and/dab/simple-temple-test-group/util"
	"github.com/google/uuid"

	// pq acts as the driver for SQL requests
	_ "github.com/lib/pq"
)

// BaseDatastore provides the basic datastore methods
type BaseDatastore interface {
	CreateSimpleTempleTestGroup(input CreateSimpleTempleTestGroupInput) (*SimpleTempleTestGroup, error)
	ReadSimpleTempleTestGroup(input ReadSimpleTempleTestGroupInput) (*SimpleTempleTestGroup, error)
	DeleteSimpleTempleTestGroup(input DeleteSimpleTempleTestGroupInput) error
}

// DAO encapsulates access to the datastore
type DAO struct {
	DB *sql.DB
}

// SimpleTempleTestGroup encapsulates the object stored in the datastore
type SimpleTempleTestGroup struct {
	ID        uuid.UUID
	CreatedBy uuid.UUID
}

// CreateSimpleTempleTestGroupInput encapsulates the information required to create a single simpleTempleTestGroup in the datastore
type CreateSimpleTempleTestGroupInput struct {
	ID     uuid.UUID
	AuthID uuid.UUID
}

// ReadSimpleTempleTestGroupInput encapsulates the information required to read a single simpleTempleTestGroup in the datastore
type ReadSimpleTempleTestGroupInput struct {
	ID uuid.UUID
}

// DeleteSimpleTempleTestGroupInput encapsulates the information required to delete a single simpleTempleTestGroup in the datastore
type DeleteSimpleTempleTestGroupInput struct {
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

// CreateSimpleTempleTestGroup creates a new simpleTempleTestGroup in the datastore, returning the newly created simpleTempleTestGroup
func (dao *DAO) CreateSimpleTempleTestGroup(input CreateSimpleTempleTestGroupInput) (*SimpleTempleTestGroup, error) {
	row := executeQueryWithRowResponse(dao.DB, "INSERT INTO simple_temple_test_group (id, createdBy) VALUES ($1, $2) RETURNING id, createdBy;", input.ID, input.AuthID)

	var simpleTempleTestGroup SimpleTempleTestGroup
	err := row.Scan(&simpleTempleTestGroup.ID, &simpleTempleTestGroup.CreatedBy)
	if err != nil {
		return nil, err
	}

	return &simpleTempleTestGroup, nil
}

// ReadSimpleTempleTestGroup returns the simpleTempleTestGroup in the datastore for a given ID
func (dao *DAO) ReadSimpleTempleTestGroup(input ReadSimpleTempleTestGroupInput) (*SimpleTempleTestGroup, error) {
	row := executeQueryWithRowResponse(dao.DB, "SELECT id, createdBy FROM simple_temple_test_group WHERE id = $1;", input.ID)

	var simpleTempleTestGroup SimpleTempleTestGroup
	err := row.Scan(&simpleTempleTestGroup.ID, &simpleTempleTestGroup.CreatedBy)
	if err != nil {
		switch err {
		case sql.ErrNoRows:
			return nil, ErrSimpleTempleTestGroupNotFound(input.ID.String())
		default:
			return nil, err
		}
	}

	return &simpleTempleTestGroup, nil
}

// DeleteSimpleTempleTestGroup deletes the simpleTempleTestGroup in the datastore for a given ID
func (dao *DAO) DeleteSimpleTempleTestGroup(input DeleteSimpleTempleTestGroupInput) error {
	rowsAffected, err := executeQuery(dao.DB, "DELETE FROM simple_temple_test_group WHERE id = $1;", input.ID)
	if err != nil {
		return err
	} else if rowsAffected == 0 {
		return ErrSimpleTempleTestGroupNotFound(input.ID.String())
	}

	return nil
}
