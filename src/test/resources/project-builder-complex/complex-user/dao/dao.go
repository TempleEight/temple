package dao

import (
	"database/sql"
	"fmt"
	"time"

	"github.com/squat/and/dab/complex-user/util"
	"github.com/google/uuid"

	// pq acts as the driver for SQL requests
	_ "github.com/lib/pq"
)

// Datastore provides the interface adopted by the DAO, allowing for mocking
type Datastore interface {
	CreateComplexUser(input CreateComplexUserInput) (*ComplexUser, error)
	ReadComplexUser(input ReadComplexUserInput) (*ComplexUser, error)
	UpdateComplexUser(input UpdateComplexUserInput) (*ComplexUser, error)
	DeleteComplexUser(input DeleteComplexUserInput) error
}

// DAO encapsulates access to the datastore
type DAO struct {
	DB *sql.DB
}

// ComplexUser encapsulates the object stored in the datastore
type ComplexUser struct {
	ID                 uuid.UUID
	SmallIntField      uint16
	IntField           uint32
	BigIntField        uint64
	FloatField         float32
	DoubleField        float64
	StringField        string
	BoundedStringField string
	BoolField          bool
	DateField          time.Time
	TimeField          time.Time
	DateTimeField      time.Time
	BlobField          []byte
}

// CreateComplexUserInput encapsulates the information required to create a single complexUser in the datastore
type CreateComplexUserInput struct {
	ID                 uuid.UUID
	SmallIntField      uint16
	IntField           uint32
	BigIntField        uint64
	FloatField         float32
	DoubleField        float64
	StringField        string
	BoundedStringField string
	BoolField          bool
	DateField          time.Time
	TimeField          time.Time
	DateTimeField      time.Time
	BlobField          []byte
}

// ReadComplexUserInput encapsulates the information required to read a single complexUser in the datastore
type ReadComplexUserInput struct {
	ID uuid.UUID
}

// UpdateComplexUserInput encapsulates the information required to update a single complexUser in the datastore
type UpdateComplexUserInput struct {
	ID                 uuid.UUID
	SmallIntField      uint16
	IntField           uint32
	BigIntField        uint64
	FloatField         float32
	DoubleField        float64
	StringField        string
	BoundedStringField string
	BoolField          bool
	DateField          time.Time
	TimeField          time.Time
	DateTimeField      time.Time
	BlobField          []byte
}

// DeleteComplexUserInput encapsulates the information required to delete a single complexUser in the datastore
type DeleteComplexUserInput struct {
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

// CreateComplexUser creates a new complexUser in the datastore, returning the newly created complexUser
func (dao *DAO) CreateComplexUser(input CreateComplexUserInput) (*ComplexUser, error) {
	row := executeQueryWithRowResponse(dao.DB, "INSERT INTO complex_user (id, smallIntField, intField, bigIntField, floatField, doubleField, stringField, boundedStringField, boolField, dateField, timeField, dateTimeField, blobField) VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9, $10, $11, $12, $13) RETURNING id, smallIntField, intField, bigIntField, floatField, doubleField, stringField, boundedStringField, boolField, dateField, timeField, dateTimeField, blobField;", input.ID, input.SmallIntField, input.IntField, input.BigIntField, input.FloatField, input.DoubleField, input.StringField, input.BoundedStringField, input.BoolField, input.DateField, input.TimeField, input.DateTimeField, input.BlobField)

	var complexUser ComplexUser
	err := row.Scan(&complexUser.ID, &complexUser.SmallIntField, &complexUser.IntField, &complexUser.BigIntField, &complexUser.FloatField, &complexUser.DoubleField, &complexUser.StringField, &complexUser.BoundedStringField, &complexUser.BoolField, &complexUser.DateField, &complexUser.TimeField, &complexUser.DateTimeField, &complexUser.BlobField)
	if err != nil {
		return nil, err
	}

	return &complexUser, nil
}

// ReadComplexUser returns the complexUser in the datastore for a given ID
func (dao *DAO) ReadComplexUser(input ReadComplexUserInput) (*ComplexUser, error) {
	row := executeQueryWithRowResponse(dao.DB, "SELECT id, smallIntField, intField, bigIntField, floatField, doubleField, stringField, boundedStringField, boolField, dateField, timeField, dateTimeField, blobField FROM complex_user WHERE id = $1;", input.ID)

	var complexUser ComplexUser
	err := row.Scan(&complexUser.ID, &complexUser.SmallIntField, &complexUser.IntField, &complexUser.BigIntField, &complexUser.FloatField, &complexUser.DoubleField, &complexUser.StringField, &complexUser.BoundedStringField, &complexUser.BoolField, &complexUser.DateField, &complexUser.TimeField, &complexUser.DateTimeField, &complexUser.BlobField)
	if err != nil {
		switch err {
		case sql.ErrNoRows:
			return nil, ErrComplexUserNotFound(input.ID.String())
		default:
			return nil, err
		}
	}

	return &complexUser, nil
}

// UpdateComplexUser updates the complexUser in the datastore for a given ID, returning the newly updated complexUser
func (dao *DAO) UpdateComplexUser(input UpdateComplexUserInput) (*ComplexUser, error) {
	row := executeQueryWithRowResponse(dao.DB, "UPDATE complex_user SET smallIntField = $1, intField = $2, bigIntField = $3, floatField = $4, doubleField = $5, stringField = $6, boundedStringField = $7, boolField = $8, dateField = $9, timeField = $10, dateTimeField = $11, blobField = $12 WHERE id = $13 RETURNING id, smallIntField, intField, bigIntField, floatField, doubleField, stringField, boundedStringField, boolField, dateField, timeField, dateTimeField, blobField;", input.SmallIntField, input.IntField, input.BigIntField, input.FloatField, input.DoubleField, input.StringField, input.BoundedStringField, input.BoolField, input.DateField, input.TimeField, input.DateTimeField, input.BlobField, input.ID)

	var complexUser ComplexUser
	err := row.Scan(&complexUser.ID, &complexUser.SmallIntField, &complexUser.IntField, &complexUser.BigIntField, &complexUser.FloatField, &complexUser.DoubleField, &complexUser.StringField, &complexUser.BoundedStringField, &complexUser.BoolField, &complexUser.DateField, &complexUser.TimeField, &complexUser.DateTimeField, &complexUser.BlobField)
	if err != nil {
		switch err {
		case sql.ErrNoRows:
			return nil, ErrComplexUserNotFound(input.ID.String())
		default:
			return nil, err
		}
	}

	return &complexUser, nil
}

// DeleteComplexUser deletes the complexUser in the datastore for a given ID
func (dao *DAO) DeleteComplexUser(input DeleteComplexUserInput) error {
	rowsAffected, err := executeQuery(dao.DB, "DELETE FROM complex_user WHERE id = $1;", input.ID)
	if err != nil {
		return err
	} else if rowsAffected == 0 {
		return ErrComplexUserNotFound(input.ID.String())
	}

	return nil
}
