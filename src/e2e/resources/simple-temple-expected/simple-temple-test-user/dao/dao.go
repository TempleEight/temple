package dao

import (
	"database/sql"
	"fmt"
	"time"

	"github.com/squat/and/dab/simple-temple-test-user/util"
	"github.com/google/uuid"

	// pq acts as the driver for SQL requests
	_ "github.com/lib/pq"
)

// Datastore provides the interface adopted by the DAO, allowing for mocking
type Datastore interface {
	ListSimpleTempleTestUser() (*[]SimpleTempleTestUser, error)
	CreateSimpleTempleTestUser(input CreateSimpleTempleTestUserInput) (*SimpleTempleTestUser, error)
	ReadSimpleTempleTestUser(input ReadSimpleTempleTestUserInput) (*SimpleTempleTestUser, error)
	UpdateSimpleTempleTestUser(input UpdateSimpleTempleTestUserInput) (*SimpleTempleTestUser, error)
}

// DAO encapsulates access to the datastore
type DAO struct {
	DB *sql.DB
}

// SimpleTempleTestUser encapsulates the object stored in the datastore
type SimpleTempleTestUser struct {
	ID                   uuid.UUID
	CreatedBy            uuid.UUID
	SimpleTempleTestUser string
	Email                string
	FirstName            string
	LastName             string
	CreatedAt            time.Time
	NumberOfDogs         int32
	Yeets                bool
	CurrentBankBalance   float32
	BirthDate            time.Time
	BreakfastTime        time.Time
}

// CreateSimpleTempleTestUserInput encapsulates the information required to create a single simpleTempleTestUser in the datastore
type CreateSimpleTempleTestUserInput struct {
	ID                   uuid.UUID
	AuthID               uuid.UUID
	SimpleTempleTestUser string
	Email                string
	FirstName            string
	LastName             string
	CreatedAt            time.Time
	NumberOfDogs         int32
	Yeets                bool
	CurrentBankBalance   float32
	BirthDate            time.Time
	BreakfastTime        time.Time
}

// ReadSimpleTempleTestUserInput encapsulates the information required to read a single simpleTempleTestUser in the datastore
type ReadSimpleTempleTestUserInput struct {
	ID uuid.UUID
}

// UpdateSimpleTempleTestUserInput encapsulates the information required to update a single simpleTempleTestUser in the datastore
type UpdateSimpleTempleTestUserInput struct {
	ID                   uuid.UUID
	SimpleTempleTestUser string
	Email                string
	FirstName            string
	LastName             string
	CreatedAt            time.Time
	NumberOfDogs         int32
	Yeets                bool
	CurrentBankBalance   float32
	BirthDate            time.Time
	BreakfastTime        time.Time
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

// Executes a query, returning the rows
func executeQueryWithRowResponses(db *sql.DB, query string, args ...interface{}) (*sql.Rows, error) {
	return db.Query(query, args...)
}

// Executes a query, returning the row
func executeQueryWithRowResponse(db *sql.DB, query string, args ...interface{}) *sql.Row {
	return db.QueryRow(query, args...)
}

// CreateSimpleTempleTestUser creates a new simpleTempleTestUser in the datastore, returning the newly created simpleTempleTestUser
func (dao *DAO) CreateSimpleTempleTestUser(input CreateSimpleTempleTestUserInput) (*SimpleTempleTestUser, error) {
	row := executeQueryWithRowResponse(dao.DB, "INSERT INTO simple_temple_test_user (id, simpleTempleTestUser, email, firstName, lastName, createdAt, numberOfDogs, yeets, currentBankBalance, birthDate, breakfastTime) VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9, $10, $11) RETURNING id, simpleTempleTestUser, email, firstName, lastName, createdAt, numberOfDogs, yeets, currentBankBalance, birthDate, breakfastTime;", input.ID, input.SimpleTempleTestUser, input.Email, input.FirstName, input.LastName, input.CreatedAt, input.NumberOfDogs, input.Yeets, input.CurrentBankBalance, input.BirthDate, input.BreakfastTime)

	var simpleTempleTestUser SimpleTempleTestUser
	err := row.Scan(&simpleTempleTestUser.ID, &simpleTempleTestUser.CreatedBy, &simpleTempleTestUser.SimpleTempleTestUser, &simpleTempleTestUser.Email, &simpleTempleTestUser.FirstName, &simpleTempleTestUser.LastName, &simpleTempleTestUser.CreatedAt, &simpleTempleTestUser.NumberOfDogs, &simpleTempleTestUser.Yeets, &simpleTempleTestUser.CurrentBankBalance, &simpleTempleTestUser.BirthDate, &simpleTempleTestUser.BreakfastTime)
	if err != nil {
		return nil, err
	}

	return &simpleTempleTestUser, nil
}

// ReadSimpleTempleTestUser returns the simpleTempleTestUser in the datastore for a given ID
func (dao *DAO) ReadSimpleTempleTestUser(input ReadSimpleTempleTestUserInput) (*SimpleTempleTestUser, error) {
	row := executeQueryWithRowResponse(dao.DB, "SELECT id, simpleTempleTestUser, email, firstName, lastName, createdAt, numberOfDogs, yeets, currentBankBalance, birthDate, breakfastTime FROM simple_temple_test_user WHERE id = $1;", input.ID)

	var simpleTempleTestUser SimpleTempleTestUser
	err := row.Scan(&simpleTempleTestUser.ID, &simpleTempleTestUser.CreatedBy, &simpleTempleTestUser.SimpleTempleTestUser, &simpleTempleTestUser.Email, &simpleTempleTestUser.FirstName, &simpleTempleTestUser.LastName, &simpleTempleTestUser.CreatedAt, &simpleTempleTestUser.NumberOfDogs, &simpleTempleTestUser.Yeets, &simpleTempleTestUser.CurrentBankBalance, &simpleTempleTestUser.BirthDate, &simpleTempleTestUser.BreakfastTime)
	if err != nil {
		switch err {
		case sql.ErrNoRows:
			return nil, ErrSimpleTempleTestUserNotFound(input.ID.String())
		default:
			return nil, err
		}
	}

	return &simpleTempleTestUser, nil
}

// UpdateSimpleTempleTestUser updates the simpleTempleTestUser in the datastore for a given ID, returning the newly updated simpleTempleTestUser
func (dao *DAO) UpdateSimpleTempleTestUser(input UpdateSimpleTempleTestUserInput) (*SimpleTempleTestUser, error) {
	row := executeQueryWithRowResponse(dao.DB, "UPDATE simple_temple_test_user SET simpleTempleTestUser = $1, email = $2, firstName = $3, lastName = $4, createdAt = $5, numberOfDogs = $6, yeets = $7, currentBankBalance = $8, birthDate = $9, breakfastTime = $10 WHERE id = $11 RETURNING id, simpleTempleTestUser, email, firstName, lastName, createdAt, numberOfDogs, yeets, currentBankBalance, birthDate, breakfastTime;", input.SimpleTempleTestUser, input.Email, input.FirstName, input.LastName, input.CreatedAt, input.NumberOfDogs, input.Yeets, input.CurrentBankBalance, input.BirthDate, input.BreakfastTime, input.ID)

	var simpleTempleTestUser SimpleTempleTestUser
	err := row.Scan(&simpleTempleTestUser.ID, &simpleTempleTestUser.CreatedBy, &simpleTempleTestUser.SimpleTempleTestUser, &simpleTempleTestUser.Email, &simpleTempleTestUser.FirstName, &simpleTempleTestUser.LastName, &simpleTempleTestUser.CreatedAt, &simpleTempleTestUser.NumberOfDogs, &simpleTempleTestUser.Yeets, &simpleTempleTestUser.CurrentBankBalance, &simpleTempleTestUser.BirthDate, &simpleTempleTestUser.BreakfastTime)
	if err != nil {
		switch err {
		case sql.ErrNoRows:
			return nil, ErrSimpleTempleTestUserNotFound(input.ID.String())
		default:
			return nil, err
		}
	}

	return &simpleTempleTestUser, nil
}

// ListSimpleTempleTestUser returns a list containing every simpleTempleTestUser in the datastore
func (dao *DAO) ListSimpleTempleTestUser() (*[]SimpleTempleTestUser, error) {
	rows, err := executeQueryWithRowResponses(dao.DB, "SELECT id, simpleTempleTestUser, email, firstName, lastName, createdAt, numberOfDogs, yeets, currentBankBalance, birthDate, breakfastTime FROM simple_temple_test_user;")
	if err != nil {
		return nil, err
	}

	simpleTempleTestUserList := make([]SimpleTempleTestUser, 0)
	for rows.Next() {
		var simpleTempleTestUser SimpleTempleTestUser
		err = rows.Scan(&simpleTempleTestUser.ID, &simpleTempleTestUser.CreatedBy, &simpleTempleTestUser.SimpleTempleTestUser, &simpleTempleTestUser.Email, &simpleTempleTestUser.FirstName, &simpleTempleTestUser.LastName, &simpleTempleTestUser.CreatedAt, &simpleTempleTestUser.NumberOfDogs, &simpleTempleTestUser.Yeets, &simpleTempleTestUser.CurrentBankBalance, &simpleTempleTestUser.BirthDate, &simpleTempleTestUser.BreakfastTime)
		if err != nil {
			return nil, err
		}
		simpleTempleTestUserList = append(simpleTempleTestUserList, simpleTempleTestUser)
	}
	err = rows.Err()
	if err != nil {
		return nil, err
	}

	return &simpleTempleTestUserList, nil
}
