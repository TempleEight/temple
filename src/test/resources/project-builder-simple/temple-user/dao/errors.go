package dao

import "fmt"

// ErrTempleUserNotFound is returned when a templeUser for the provided ID was not found
type ErrTempleUserNotFound string

func (e ErrTempleUserNotFound) Error() string {
	return fmt.Sprintf("templeUser not found with ID %d", string(e))
}
