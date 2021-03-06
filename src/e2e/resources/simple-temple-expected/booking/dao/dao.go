package dao

import (
	"database/sql"
	"fmt"

	"github.com/squat/and/dab/booking/util"
	"github.com/google/uuid"

	// pq acts as the driver for SQL requests
	_ "github.com/lib/pq"
)

// BaseDatastore provides the basic datastore methods
type BaseDatastore interface {
	CreateBooking(input CreateBookingInput) (*Booking, error)
	ReadBooking(input ReadBookingInput) (*Booking, error)
	DeleteBooking(input DeleteBookingInput) error
}

// DAO encapsulates access to the datastore
type DAO struct {
	DB *sql.DB
}

// Booking encapsulates the object stored in the datastore
type Booking struct {
	ID        uuid.UUID
	CreatedBy uuid.UUID
}

// CreateBookingInput encapsulates the information required to create a single booking in the datastore
type CreateBookingInput struct {
	ID     uuid.UUID
	AuthID uuid.UUID
}

// ReadBookingInput encapsulates the information required to read a single booking in the datastore
type ReadBookingInput struct {
	ID uuid.UUID
}

// DeleteBookingInput encapsulates the information required to delete a single booking in the datastore
type DeleteBookingInput struct {
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

// CreateBooking creates a new booking in the datastore, returning the newly created booking
func (dao *DAO) CreateBooking(input CreateBookingInput) (*Booking, error) {
	row := executeQueryWithRowResponse(dao.DB, "INSERT INTO booking (id, created_by) VALUES ($1, $2) RETURNING id, created_by;", input.ID, input.AuthID)

	var booking Booking
	err := row.Scan(&booking.ID, &booking.CreatedBy)
	if err != nil {
		return nil, err
	}

	return &booking, nil
}

// ReadBooking returns the booking in the datastore for a given ID
func (dao *DAO) ReadBooking(input ReadBookingInput) (*Booking, error) {
	row := executeQueryWithRowResponse(dao.DB, "SELECT id, created_by FROM booking WHERE id = $1;", input.ID)

	var booking Booking
	err := row.Scan(&booking.ID, &booking.CreatedBy)
	if err != nil {
		switch err {
		case sql.ErrNoRows:
			return nil, ErrBookingNotFound(input.ID.String())
		default:
			return nil, err
		}
	}

	return &booking, nil
}

// DeleteBooking deletes the booking in the datastore for a given ID
func (dao *DAO) DeleteBooking(input DeleteBookingInput) error {
	rowsAffected, err := executeQuery(dao.DB, "DELETE FROM booking WHERE id = $1;", input.ID)
	if err != nil {
		return err
	} else if rowsAffected == 0 {
		return ErrBookingNotFound(input.ID.String())
	}

	return nil
}
