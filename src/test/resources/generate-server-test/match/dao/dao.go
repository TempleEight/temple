package dao

import (
	"database/sql"
	"fmt"
	"time"

	"github.com/TempleEight/spec-golang/match/util"
	"github.com/google/uuid"

	// pq acts as the driver for SQL requests
	_ "github.com/lib/pq"
)

// BaseDatastore provides the basic datastore methods
type BaseDatastore interface {
	ListMatch(input ListMatchInput) (*[]Match, error)
	CreateMatch(input CreateMatchInput) (*Match, error)
	ReadMatch(input ReadMatchInput) (*Match, error)
	UpdateMatch(input UpdateMatchInput) (*Match, error)
	DeleteMatch(input DeleteMatchInput) error
}

// DAO encapsulates access to the datastore
type DAO struct {
	DB *sql.DB
}

// Match encapsulates the object stored in the datastore
type Match struct {
	ID        uuid.UUID
	CreatedBy uuid.UUID
	UserOne   uuid.UUID
	UserTwo   uuid.UUID
	MatchedOn time.Time
}

// ListMatchInput encapsulates the information required to read a match list in the datastore
type ListMatchInput struct {
	AuthID uuid.UUID
}

// CreateMatchInput encapsulates the information required to create a single match in the datastore
type CreateMatchInput struct {
	ID        uuid.UUID
	AuthID    uuid.UUID
	UserOne   uuid.UUID
	UserTwo   uuid.UUID
	MatchedOn time.Time
}

// ReadMatchInput encapsulates the information required to read a single match in the datastore
type ReadMatchInput struct {
	ID uuid.UUID
}

// UpdateMatchInput encapsulates the information required to update a single match in the datastore
type UpdateMatchInput struct {
	ID        uuid.UUID
	UserOne   uuid.UUID
	UserTwo   uuid.UUID
	MatchedOn time.Time
}

// DeleteMatchInput encapsulates the information required to delete a single match in the datastore
type DeleteMatchInput struct {
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

// ListMatch returns a list containing every match in the datastore for a given ID
func (dao *DAO) ListMatch(input ListMatchInput) (*[]Match, error) {
	rows, err := executeQueryWithRowResponses(dao.DB, "SELECT id, created_by, userOne, userTwo, matchedOn FROM match WHERE created_by = $1", input.AuthID)
	if err != nil {
		return nil, err
	}

	matchList := make([]Match, 0)
	for rows.Next() {
		var match Match
		err = rows.Scan(&match.ID, &match.CreatedBy, &match.UserOne, &match.UserTwo, &match.MatchedOn)
		if err != nil {
			return nil, err
		}
		matchList = append(matchList, match)
	}
	err = rows.Err()
	if err != nil {
		return nil, err
	}

	return &matchList, nil
}

// CreateMatch creates a new match in the datastore, returning the newly created match
func (dao *DAO) CreateMatch(input CreateMatchInput) (*Match, error) {
	row := executeQueryWithRowResponse(dao.DB, "INSERT INTO match (id, created_by, userOne, userTwo, matchedOn) VALUES ($1, $2, $3, $4, $5) RETURNING id, created_by, userOne, userTwo, matchedOn", input.ID, input.AuthID, input.UserOne, input.UserTwo, input.MatchedOn)

	var match Match
	err := row.Scan(&match.ID, &match.CreatedBy, &match.UserOne, &match.UserTwo, &match.MatchedOn)
	if err != nil {
		return nil, err
	}

	return &match, nil
}

// ReadMatch returns the match in the datastore for a given ID
func (dao *DAO) ReadMatch(input ReadMatchInput) (*Match, error) {
	row := executeQueryWithRowResponse(dao.DB, "SELECT id, created_by, userOne, userTwo, matchedOn FROM match WHERE id = $1", input.ID)

	var match Match
	err := row.Scan(&match.ID, &match.CreatedBy, &match.UserOne, &match.UserTwo, &match.MatchedOn)
	if err != nil {
		switch err {
		case sql.ErrNoRows:
			return nil, ErrMatchNotFound(input.ID.String())
		default:
			return nil, err
		}
	}

	return &match, nil
}

// UpdateMatch updates the match in the datastore for a given ID, returning the newly updated match
func (dao *DAO) UpdateMatch(input UpdateMatchInput) (*Match, error) {
	row := executeQueryWithRowResponse(dao.DB, "UPDATE match SET userOne = $1, userTwo = $2, matchedOn = $3 WHERE id = $4 RETURNING id, created_by, userOne, userTwo, matchedOn", input.UserOne, input.UserTwo, input.MatchedOn, input.ID)

	var match Match
	err := row.Scan(&match.ID, &match.CreatedBy, &match.UserOne, &match.UserTwo, &match.MatchedOn)
	if err != nil {
		switch err {
		case sql.ErrNoRows:
			return nil, ErrMatchNotFound(input.ID.String())
		default:
			return nil, err
		}
	}

	return &match, nil
}

// DeleteMatch deletes the match in the datastore for a given ID
func (dao *DAO) DeleteMatch(input DeleteMatchInput) error {
	rowsAffected, err := executeQuery(dao.DB, "DELETE FROM match WHERE id = $1", input.ID)
	if err != nil {
		return err
	} else if rowsAffected == 0 {
		return ErrMatchNotFound(input.ID.String())
	}

	return nil
}
