package dao

import "fmt"

// ErrUserNotFound is returned when a user for the provided ID was not found
type ErrUserNotFound string

func (e ErrUserNotFound) Error() string {
	return fmt.Sprintf("user not found with ID %s", string(e))
}
