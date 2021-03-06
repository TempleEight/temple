package dao

import (
	"database/sql"
	"fmt"

	"github.com/TempleEight/spec-golang/user/util"
	"github.com/google/uuid"

	// pq acts as the driver for SQL requests
	_ "github.com/lib/pq"
)

// BaseDatastore provides the basic datastore methods
type BaseDatastore interface {
	CreateUser(input CreateUserInput) (*User, error)
	ReadUser(input ReadUserInput) (*User, error)
	UpdateUser(input UpdateUserInput) (*User, error)
	DeleteUser(input DeleteUserInput) error
}

// DAO encapsulates access to the datastore
type DAO struct {
	DB *sql.DB
}

// User encapsulates the object stored in the datastore
type User struct {
	ID   uuid.UUID
	Name string
}

// CreateUserInput encapsulates the information required to create a single user in the datastore
type CreateUserInput struct {
	ID   uuid.UUID
	Name string
}

// ReadUserInput encapsulates the information required to read a single user in the datastore
type ReadUserInput struct {
	ID uuid.UUID
}

// UpdateUserInput encapsulates the information required to update a single user in the datastore
type UpdateUserInput struct {
	ID   uuid.UUID
	Name string
}

// DeleteUserInput encapsulates the information required to delete a single user in the datastore
type DeleteUserInput struct {
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

// CreateUser creates a new user in the datastore, returning the newly created user
func (dao *DAO) CreateUser(input CreateUserInput) (*User, error) {
	row := executeQueryWithRowResponse(dao.DB, "INSERT INTO user_temple (id, name) VALUES ($1, $2) RETURNING id, name", input.ID, input.Name)

	var user User
	err := row.Scan(&user.ID, &user.Name)
	if err != nil {
		return nil, err
	}

	return &user, nil
}

// ReadUser returns the user in the datastore for a given ID
func (dao *DAO) ReadUser(input ReadUserInput) (*User, error) {
	row := executeQueryWithRowResponse(dao.DB, "SELECT id, name FROM user_temple WHERE id = $1", input.ID)

	var user User
	err := row.Scan(&user.ID, &user.Name)
	if err != nil {
		switch err {
		case sql.ErrNoRows:
			return nil, ErrUserNotFound(input.ID.String())
		default:
			return nil, err
		}
	}

	return &user, nil
}

// UpdateUser updates the user in the datastore for a given ID, returning the newly updated user
func (dao *DAO) UpdateUser(input UpdateUserInput) (*User, error) {
	row := executeQueryWithRowResponse(dao.DB, "UPDATE user_temple SET name = $1 WHERE id = $2 RETURNING id, name", input.Name, input.ID)

	var user User
	err := row.Scan(&user.ID, &user.Name)
	if err != nil {
		switch err {
		case sql.ErrNoRows:
			return nil, ErrUserNotFound(input.ID.String())
		default:
			return nil, err
		}
	}

	return &user, nil
}

// DeleteUser deletes the user in the datastore for a given ID
func (dao *DAO) DeleteUser(input DeleteUserInput) error {
	rowsAffected, err := executeQuery(dao.DB, "DELETE FROM user_temple WHERE id = $1", input.ID)
	if err != nil {
		return err
	} else if rowsAffected == 0 {
		return ErrUserNotFound(input.ID.String())
	}

	return nil
}
