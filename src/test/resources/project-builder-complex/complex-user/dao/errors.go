package dao

import "fmt"

// ErrComplexUserNotFound is returned when a complexUser for the provided ID was not found
type ErrComplexUserNotFound string

func (e ErrComplexUserNotFound) Error() string {
	return fmt.Sprintf("complexUser not found with ID %s", string(e))
}

// ErrTempleUserNotFound is returned when a templeUser for the provided ID was not found
type ErrTempleUserNotFound string

func (e ErrTempleUserNotFound) Error() string {
	return fmt.Sprintf("templeUser not found with ID %s", string(e))
}
