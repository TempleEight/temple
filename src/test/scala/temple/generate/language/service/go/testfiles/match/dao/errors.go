package dao

import "fmt"

// ErrMatchNotFound is returned when a match for the provided ID was not found
type ErrMatchNotFound int64

func (e ErrMatchNotFound) Error() string {
	return fmt.Sprintf("match not found with ID %d", e)
}
