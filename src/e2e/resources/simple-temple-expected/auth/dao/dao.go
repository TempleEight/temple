package dao

import (
	"database/sql"
	"fmt"

	"github.com/squat/and/dab/auth/util"
	"github.com/google/uuid"

	// pq acts as the driver for SQL requests
	"github.com/lib/pq"
)

// https://www.postgresql.org/docs/9.3/errcodes-appendix.html
const psqlUniqueViolation = "unique_violation"

// BaseDatastore provides the basic datastore methods
type BaseDatastore interface {
	CreateAuth(input CreateAuthInput) (*Auth, error)
	ReadAuth(input ReadAuthInput) (*Auth, error)
}

// DAO encapsulates access to the datastore
type DAO struct {
	DB *sql.DB
}

// Auth encapsulates the object stored in the datastore
type Auth struct {
	ID       uuid.UUID
	Email    string
	Password string
}

// CreateAuthInput encapsulates the information required to create a single auth in the datastore
type CreateAuthInput struct {
	ID       uuid.UUID
	Email    string
	Password string
}

// ReadAuthInput encapsulates the information required to read a single auth in the datastore
type ReadAuthInput struct {
	Email string
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

// CreateAuth creates a new auth in the datastore, returning the newly created auth
func (dao *DAO) CreateAuth(input CreateAuthInput) (*Auth, error) {
	row := executeQueryWithRowResponse(dao.DB, "INSERT INTO auth (id, email, password) VALUES ($1, $2, $3) RETURNING id, email, password;", input.ID, input.Email, input.Password)

	var auth Auth
	err := row.Scan(&auth.ID, &auth.Email, &auth.Password)
	if err != nil {
		// PQ specific error
		if err, ok := err.(*pq.Error); ok {
			if err.Code.Name() == psqlUniqueViolation {
				return nil, ErrDuplicateAuth
			}
		}
		return nil, err
	}

	return &auth, nil
}

// ReadAuth returns the auth in the datastore for a given email
func (dao *DAO) ReadAuth(input ReadAuthInput) (*Auth, error) {
	row := executeQueryWithRowResponse(dao.DB, "SELECT id, email, password FROM auth WHERE email = $1;", input.Email)

	var auth Auth
	err := row.Scan(&auth.ID, &auth.Email, &auth.Password)
	if err != nil {
		switch err {
		case sql.ErrNoRows:
			return nil, ErrAuthNotFound
		default:
			return nil, err
		}
	}

	return &auth, nil
}
