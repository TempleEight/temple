package dao

import "fmt"

// ErrExampleNotFound is returned when a example for the provided ID was not found
type ErrExampleNotFound string

func (e ErrExampleNotFound) Error() string {
	return fmt.Sprintf("example not found with ID %s", string(e))
}
