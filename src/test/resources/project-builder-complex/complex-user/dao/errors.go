package dao

import "fmt"

// ErrComplexUserNotFound is returned when a complexUser for the provided ID was not found
type ErrComplexUserNotFound string

func (e ErrComplexUserNotFound) Error() string {
	return fmt.Sprintf("complexUser not found with ID %s", string(e))
}
