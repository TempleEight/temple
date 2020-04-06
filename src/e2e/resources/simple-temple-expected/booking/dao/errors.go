package dao

import "fmt"

// ErrBookingNotFound is returned when a booking for the provided ID was not found
type ErrBookingNotFound string

func (e ErrBookingNotFound) Error() string {
	return fmt.Sprintf("booking not found with ID %d", string(e))
}
