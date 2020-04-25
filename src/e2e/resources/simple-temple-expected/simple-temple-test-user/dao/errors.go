package dao

import "fmt"

// ErrSimpleTempleTestUserNotFound is returned when a simpleTempleTestUser for the provided ID was not found
type ErrSimpleTempleTestUserNotFound string

func (e ErrSimpleTempleTestUserNotFound) Error() string {
	return fmt.Sprintf("simpleTempleTestUser not found with ID %s", string(e))
}

// ErrDuplicateSimpleTempleTestUserNotFound is returned when a simpleTempleTestUser already exists for some unique constraint
type ErrDuplicateSimpleTempleTestUser string

func (e ErrDuplicateSimpleTempleTestUser) Error() string {
	return "Duplicate SimpleTempleTestUser found"
}

// ErrFredNotFound is returned when a fred for the provided ID was not found
type ErrFredNotFound string

func (e ErrFredNotFound) Error() string {
	return fmt.Sprintf("fred not found with ID %s", string(e))
}
