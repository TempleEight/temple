package dao

import (
	"database/sql"
	"fmt"
	"time"

	"github.com/squat/and/dab/temple-user/util"
	"github.com/google/uuid"

	// pq acts as the driver for SQL requests
	_ "github.com/lib/pq"
)

// Datastore provides the interface adopted by the DAO, allowing for mocking
type Datastore interface {
	CreateTempleUser(input CreateTempleUserInput) (*TempleUser, error)
	ReadTempleUser(input ReadTempleUserInput) (*TempleUser, error)
	UpdateTempleUser(input UpdateTempleUserInput) (*TempleUser, error)
	DeleteTempleUser(input DeleteTempleUserInput) error
}

// DAO encapsulates access to the datastore
type DAO struct {
	DB *sql.DB
}

// TempleUser encapsulates the object stored in the datastore
type TempleUser struct {
	ID            uuid.UUID
	IntField      int32
	DoubleField   float64
	StringField   string
	BoolField     bool
	DateField     time.Time
	TimeField     time.Time
	DateTimeField time.Time
	BlobField     []byte
}

// CreateTempleUserInput encapsulates the information required to create a single templeUser in the datastore
type CreateTempleUserInput struct {
	ID            uuid.UUID
	IntField      int32
	DoubleField   float64
	StringField   string
	BoolField     bool
	DateField     time.Time
	TimeField     time.Time
	DateTimeField time.Time
	BlobField     []byte
}

// ReadTempleUserInput encapsulates the information required to read a single templeUser in the datastore
type ReadTempleUserInput struct {
	ID uuid.UUID
}

// UpdateTempleUserInput encapsulates the information required to update a single templeUser in the datastore
type UpdateTempleUserInput struct {
	ID            uuid.UUID
	IntField      int32
	DoubleField   float64
	StringField   string
	BoolField     bool
	DateField     time.Time
	TimeField     time.Time
	DateTimeField time.Time
	BlobField     []byte
}

// DeleteTempleUserInput encapsulates the information required to delete a single templeUser in the datastore
type DeleteTempleUserInput struct {
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

// CreateTempleUser creates a new templeUser in the datastore, returning the newly created templeUser
func (dao *DAO) CreateTempleUser(input CreateTempleUserInput) (*TempleUser, error) {
	row := executeQueryWithRowResponse(dao.DB, "INSERT INTO temple_user (intField, doubleField, stringField, boolField, dateField, timeField, dateTimeField, blobField) VALUES ($1, $2, $3, $4, $5, $6, $7, $8) RETURNING intField, doubleField, stringField, boolField, dateField, timeField, dateTimeField, blobField;", input.ID, input.IntField, input.DoubleField, input.StringField, input.BoolField, input.DateField, input.TimeField, input.DateTimeField, input.BlobField)

	var templeUser TempleUser
	err := row.Scan(&templeUser.ID, &templeUser.IntField, &templeUser.DoubleField, &templeUser.StringField, &templeUser.BoolField, &templeUser.DateField, &templeUser.TimeField, &templeUser.DateTimeField, &templeUser.BlobField)
	if err != nil {
		return nil, err
	}

	return &templeUser, nil
}

// ReadTempleUser returns the templeUser in the datastore for a given ID
func (dao *DAO) ReadTempleUser(input ReadTempleUserInput) (*TempleUser, error) {
	row := executeQueryWithRowResponse(dao.DB, "SELECT intField, doubleField, stringField, boolField, dateField, timeField, dateTimeField, blobField FROM temple_user WHERE id = $1;", input.ID)

	var templeUser TempleUser
	err := row.Scan(&templeUser.ID, &templeUser.IntField, &templeUser.DoubleField, &templeUser.StringField, &templeUser.BoolField, &templeUser.DateField, &templeUser.TimeField, &templeUser.DateTimeField, &templeUser.BlobField)
	if err != nil {
		switch err {
		case sql.ErrNoRows:
			return nil, ErrTempleUserNotFound(input.ID.String())
		default:
			return nil, err
		}
	}

	return &templeUser, nil
}

// UpdateTempleUser updates the templeUser in the datastore for a given ID, returning the newly updated templeUser
func (dao *DAO) UpdateTempleUser(input UpdateTempleUserInput) (*TempleUser, error) {
	row := executeQueryWithRowResponse(dao.DB, "UPDATE temple_user SET intField = $1, doubleField = $2, stringField = $3, boolField = $4, dateField = $5, timeField = $6, dateTimeField = $7, blobField = $8 WHERE id = $9 RETURNING intField, doubleField, stringField, boolField, dateField, timeField, dateTimeField, blobField;", input.IntField, input.DoubleField, input.StringField, input.BoolField, input.DateField, input.TimeField, input.DateTimeField, input.BlobField, input.ID)

	var templeUser TempleUser
	err := row.Scan(&templeUser.ID, &templeUser.IntField, &templeUser.DoubleField, &templeUser.StringField, &templeUser.BoolField, &templeUser.DateField, &templeUser.TimeField, &templeUser.DateTimeField, &templeUser.BlobField)
	if err != nil {
		switch err {
		case sql.ErrNoRows:
			return nil, ErrTempleUserNotFound(input.ID.String())
		default:
			return nil, err
		}
	}

	return &templeUser, nil
}

// DeleteTempleUser deletes the templeUser in the datastore for a given ID
func (dao *DAO) DeleteTempleUser(input DeleteTempleUserInput) error {
	rowsAffected, err := executeQuery(dao.DB, "DELETE FROM temple_user WHERE id = $1;", input.ID)
	if err != nil {
		return err
	} else if rowsAffected == 0 {
		return ErrTempleUserNotFound(input.ID.String())
	}

	return nil
}
