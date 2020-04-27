package dao

import (
	"database/sql"
	"fmt"
	"time"

	"github.com/squat/and/dab/simple-temple-test-user/util"
	"github.com/google/uuid"

	// pq acts as the driver for SQL requests
	"github.com/lib/pq"
)

// BaseDatastore provides the basic datastore methods
type BaseDatastore interface {
	ListSimpleTempleTestUser() (*[]SimpleTempleTestUser, error)
	CreateSimpleTempleTestUser(input CreateSimpleTempleTestUserInput) (*SimpleTempleTestUser, error)
	ReadSimpleTempleTestUser(input ReadSimpleTempleTestUserInput) (*SimpleTempleTestUser, error)
	UpdateSimpleTempleTestUser(input UpdateSimpleTempleTestUserInput) (*SimpleTempleTestUser, error)
	IdentifySimpleTempleTestUser(input IdentifySimpleTempleTestUserInput) (*SimpleTempleTestUser, error)

	ListFred(input ListFredInput) (*[]Fred, error)
	CreateFred(input CreateFredInput) (*Fred, error)
	ReadFred(input ReadFredInput) (*Fred, error)
	UpdateFred(input UpdateFredInput) (*Fred, error)
	DeleteFred(input DeleteFredInput) error
}

// DAO encapsulates access to the datastore
type DAO struct {
	DB *sql.DB
}

// SimpleTempleTestUser encapsulates the object stored in the datastore
type SimpleTempleTestUser struct {
	ID                  uuid.UUID
	SimpleTempleTestLog string
	Email               string
	FirstName           string
	LastName            string
	CreatedAt           time.Time
	NumberOfDogs        int32
	Yeets               bool
	CurrentBankBalance  float32
	BirthDate           time.Time
	BreakfastTime       time.Time
}

// Fred encapsulates the object stored in the datastore
type Fred struct {
	ID       uuid.UUID
	ParentID uuid.UUID
	Field    string
	Friend   uuid.UUID
	Image    []byte
}

// CreateSimpleTempleTestUserInput encapsulates the information required to create a single simpleTempleTestUser in the datastore
type CreateSimpleTempleTestUserInput struct {
	ID                  uuid.UUID
	SimpleTempleTestLog string
	Email               string
	FirstName           string
	LastName            string
	CreatedAt           time.Time
	NumberOfDogs        int32
	Yeets               bool
	CurrentBankBalance  float32
	BirthDate           time.Time
	BreakfastTime       time.Time
}

// ReadSimpleTempleTestUserInput encapsulates the information required to read a single simpleTempleTestUser in the datastore
type ReadSimpleTempleTestUserInput struct {
	ID uuid.UUID
}

// UpdateSimpleTempleTestUserInput encapsulates the information required to update a single simpleTempleTestUser in the datastore
type UpdateSimpleTempleTestUserInput struct {
	ID                  uuid.UUID
	SimpleTempleTestLog string
	Email               string
	FirstName           string
	LastName            string
	CreatedAt           time.Time
	NumberOfDogs        int32
	Yeets               bool
	CurrentBankBalance  float32
	BirthDate           time.Time
	BreakfastTime       time.Time
}

// IdentifySimpleTempleTestUserInput encapsulates the information required to identify the current simpleTempleTestUser in the datastore
type IdentifySimpleTempleTestUserInput struct {
	ID uuid.UUID
}

// ListFredInput encapsulates the information required to read a fred list in the datastore
type ListFredInput struct {
	ParentID uuid.UUID
}

// CreateFredInput encapsulates the information required to create a single fred in the datastore
type CreateFredInput struct {
	ID       uuid.UUID
	ParentID uuid.UUID
	Field    string
	Friend   uuid.UUID
	Image    []byte
}

// ReadFredInput encapsulates the information required to read a single fred in the datastore
type ReadFredInput struct {
	ID uuid.UUID
}

// UpdateFredInput encapsulates the information required to update a single fred in the datastore
type UpdateFredInput struct {
	ID     uuid.UUID
	Field  string
	Friend uuid.UUID
	Image  []byte
}

// DeleteFredInput encapsulates the information required to delete a single fred in the datastore
type DeleteFredInput struct {
	ID uuid.UUID
}

// https://www.postgresql.org/docs/9.3/errcodes-appendix.html
const psqlUniqueViolation = "unique_violation"

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

// Executes a query, returning the number of rows affected
func executeQuery(db *sql.DB, query string, args ...interface{}) (int64, error) {
	result, err := db.Exec(query, args...)
	if err != nil {
		return 0, err
	}
	return result.RowsAffected()
}

// ListSimpleTempleTestUser returns a list containing every simpleTempleTestUser in the datastore
func (dao *DAO) ListSimpleTempleTestUser() (*[]SimpleTempleTestUser, error) {
	rows, err := executeQueryWithRowResponses(dao.DB, "SELECT id, simple_temple_test_log, email, first_name, last_name, created_at, number_of_dogs, yeets, current_bank_balance, birth_date, breakfast_time FROM simple_temple_test_user;")
	if err != nil {
		return nil, err
	}

	simpleTempleTestUserList := make([]SimpleTempleTestUser, 0)
	for rows.Next() {
		var simpleTempleTestUser SimpleTempleTestUser
		err = rows.Scan(&simpleTempleTestUser.ID, &simpleTempleTestUser.SimpleTempleTestLog, &simpleTempleTestUser.Email, &simpleTempleTestUser.FirstName, &simpleTempleTestUser.LastName, &simpleTempleTestUser.CreatedAt, &simpleTempleTestUser.NumberOfDogs, &simpleTempleTestUser.Yeets, &simpleTempleTestUser.CurrentBankBalance, &simpleTempleTestUser.BirthDate, &simpleTempleTestUser.BreakfastTime)
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

// CreateSimpleTempleTestUser creates a new simpleTempleTestUser in the datastore, returning the newly created simpleTempleTestUser
func (dao *DAO) CreateSimpleTempleTestUser(input CreateSimpleTempleTestUserInput) (*SimpleTempleTestUser, error) {
	row := executeQueryWithRowResponse(dao.DB, "INSERT INTO simple_temple_test_user (id, simple_temple_test_log, email, first_name, last_name, created_at, number_of_dogs, yeets, current_bank_balance, birth_date, breakfast_time) VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9, $10, $11) RETURNING id, simple_temple_test_log, email, first_name, last_name, created_at, number_of_dogs, yeets, current_bank_balance, birth_date, breakfast_time;", input.ID, input.SimpleTempleTestLog, input.Email, input.FirstName, input.LastName, input.CreatedAt, input.NumberOfDogs, input.Yeets, input.CurrentBankBalance, input.BirthDate, input.BreakfastTime)

	var simpleTempleTestUser SimpleTempleTestUser
	err := row.Scan(&simpleTempleTestUser.ID, &simpleTempleTestUser.SimpleTempleTestLog, &simpleTempleTestUser.Email, &simpleTempleTestUser.FirstName, &simpleTempleTestUser.LastName, &simpleTempleTestUser.CreatedAt, &simpleTempleTestUser.NumberOfDogs, &simpleTempleTestUser.Yeets, &simpleTempleTestUser.CurrentBankBalance, &simpleTempleTestUser.BirthDate, &simpleTempleTestUser.BreakfastTime)
	if err != nil {
		// pq specific error
		if err, ok := err.(*pq.Error); ok {
			if err.Code.Name() == psqlUniqueViolation {
				return nil, ErrDuplicateSimpleTempleTestUser(err.Detail)
			}
		}

		return nil, err
	}

	return &simpleTempleTestUser, nil
}

// ReadSimpleTempleTestUser returns the simpleTempleTestUser in the datastore for a given ID
func (dao *DAO) ReadSimpleTempleTestUser(input ReadSimpleTempleTestUserInput) (*SimpleTempleTestUser, error) {
	row := executeQueryWithRowResponse(dao.DB, "SELECT id, simple_temple_test_log, email, first_name, last_name, created_at, number_of_dogs, yeets, current_bank_balance, birth_date, breakfast_time FROM simple_temple_test_user WHERE id = $1;", input.ID)

	var simpleTempleTestUser SimpleTempleTestUser
	err := row.Scan(&simpleTempleTestUser.ID, &simpleTempleTestUser.SimpleTempleTestLog, &simpleTempleTestUser.Email, &simpleTempleTestUser.FirstName, &simpleTempleTestUser.LastName, &simpleTempleTestUser.CreatedAt, &simpleTempleTestUser.NumberOfDogs, &simpleTempleTestUser.Yeets, &simpleTempleTestUser.CurrentBankBalance, &simpleTempleTestUser.BirthDate, &simpleTempleTestUser.BreakfastTime)
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
	row := executeQueryWithRowResponse(dao.DB, "UPDATE simple_temple_test_user SET simple_temple_test_log = $1, email = $2, first_name = $3, last_name = $4, created_at = $5, number_of_dogs = $6, yeets = $7, current_bank_balance = $8, birth_date = $9, breakfast_time = $10 WHERE id = $11 RETURNING id, simple_temple_test_log, email, first_name, last_name, created_at, number_of_dogs, yeets, current_bank_balance, birth_date, breakfast_time;", input.SimpleTempleTestLog, input.Email, input.FirstName, input.LastName, input.CreatedAt, input.NumberOfDogs, input.Yeets, input.CurrentBankBalance, input.BirthDate, input.BreakfastTime, input.ID)

	var simpleTempleTestUser SimpleTempleTestUser
	err := row.Scan(&simpleTempleTestUser.ID, &simpleTempleTestUser.SimpleTempleTestLog, &simpleTempleTestUser.Email, &simpleTempleTestUser.FirstName, &simpleTempleTestUser.LastName, &simpleTempleTestUser.CreatedAt, &simpleTempleTestUser.NumberOfDogs, &simpleTempleTestUser.Yeets, &simpleTempleTestUser.CurrentBankBalance, &simpleTempleTestUser.BirthDate, &simpleTempleTestUser.BreakfastTime)
	if err != nil {
		// pq specific error
		if err, ok := err.(*pq.Error); ok {
			if err.Code.Name() == psqlUniqueViolation {
				return nil, ErrDuplicateSimpleTempleTestUser(err.Detail)
			}
		}

		switch err {
		case sql.ErrNoRows:
			return nil, ErrSimpleTempleTestUserNotFound(input.ID.String())
		default:
			return nil, err
		}
	}

	return &simpleTempleTestUser, nil
}

// IdentifySimpleTempleTestUser returns the simpleTempleTestUser in the datastore for a given ID
func (dao *DAO) IdentifySimpleTempleTestUser(input IdentifySimpleTempleTestUserInput) (*SimpleTempleTestUser, error) {
	row := executeQueryWithRowResponse(dao.DB, "SELECT id, simple_temple_test_log, email, first_name, last_name, created_at, number_of_dogs, yeets, current_bank_balance, birth_date, breakfast_time FROM simple_temple_test_user WHERE id = $1;", input.ID)

	var simpleTempleTestUser SimpleTempleTestUser
	err := row.Scan(&simpleTempleTestUser.ID, &simpleTempleTestUser.SimpleTempleTestLog, &simpleTempleTestUser.Email, &simpleTempleTestUser.FirstName, &simpleTempleTestUser.LastName, &simpleTempleTestUser.CreatedAt, &simpleTempleTestUser.NumberOfDogs, &simpleTempleTestUser.Yeets, &simpleTempleTestUser.CurrentBankBalance, &simpleTempleTestUser.BirthDate, &simpleTempleTestUser.BreakfastTime)
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

// ListFred returns a list containing every fred in the datastore
func (dao *DAO) ListFred(input ListFredInput) (*[]Fred, error) {
	rows, err := executeQueryWithRowResponses(dao.DB, "SELECT id, parent_id, field, friend, image FROM fred WHERE parent_id = $1;", input.ParentID)
	if err != nil {
		return nil, err
	}

	fredList := make([]Fred, 0)
	for rows.Next() {
		var fred Fred
		err = rows.Scan(&fred.ID, &fred.ParentID, &fred.Field, &fred.Friend, &fred.Image)
		if err != nil {
			return nil, err
		}
		fredList = append(fredList, fred)
	}
	err = rows.Err()
	if err != nil {
		return nil, err
	}

	return &fredList, nil
}

// CreateFred creates a new fred in the datastore, returning the newly created fred
func (dao *DAO) CreateFred(input CreateFredInput) (*Fred, error) {
	row := executeQueryWithRowResponse(dao.DB, "INSERT INTO fred (id, parent_id, field, friend, image) VALUES ($1, $2, $3, $4, $5) RETURNING id, parent_id, field, friend, image;", input.ID, input.ParentID, input.Field, input.Friend, input.Image)

	var fred Fred
	err := row.Scan(&fred.ID, &fred.ParentID, &fred.Field, &fred.Friend, &fred.Image)
	if err != nil {
		return nil, err
	}

	return &fred, nil
}

// ReadFred returns the fred in the datastore for a given ID
func (dao *DAO) ReadFred(input ReadFredInput) (*Fred, error) {
	row := executeQueryWithRowResponse(dao.DB, "SELECT id, parent_id, field, friend, image FROM fred WHERE id = $1;", input.ID)

	var fred Fred
	err := row.Scan(&fred.ID, &fred.ParentID, &fred.Field, &fred.Friend, &fred.Image)
	if err != nil {
		switch err {
		case sql.ErrNoRows:
			return nil, ErrFredNotFound(input.ID.String())
		default:
			return nil, err
		}
	}

	return &fred, nil
}

// UpdateFred updates the fred in the datastore for a given ID, returning the newly updated fred
func (dao *DAO) UpdateFred(input UpdateFredInput) (*Fred, error) {
	row := executeQueryWithRowResponse(dao.DB, "UPDATE fred SET parent_id = $1, field = $2, friend = $3, image = $4 WHERE id = $5 RETURNING id, parent_id, field, friend, image;", input.Field, input.Friend, input.Image, input.ID)

	var fred Fred
	err := row.Scan(&fred.ID, &fred.ParentID, &fred.Field, &fred.Friend, &fred.Image)
	if err != nil {
		switch err {
		case sql.ErrNoRows:
			return nil, ErrFredNotFound(input.ID.String())
		default:
			return nil, err
		}
	}

	return &fred, nil
}

// DeleteFred deletes the fred in the datastore for a given ID
func (dao *DAO) DeleteFred(input DeleteFredInput) error {
	rowsAffected, err := executeQuery(dao.DB, "DELETE FROM fred WHERE id = $1;", input.ID)
	if err != nil {
		return err
	} else if rowsAffected == 0 {
		return ErrFredNotFound(input.ID.String())
	}

	return nil
}
