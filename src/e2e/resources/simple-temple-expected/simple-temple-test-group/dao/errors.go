package dao

import "fmt"

// ErrSimpleTempleTestGroupNotFound is returned when a simpleTempleTestGroup for the provided ID was not found
type ErrSimpleTempleTestGroupNotFound string

func (e ErrSimpleTempleTestGroupNotFound) Error() string {
	return fmt.Sprintf("simpleTempleTestGroup not found with ID %d", string(e))
}
