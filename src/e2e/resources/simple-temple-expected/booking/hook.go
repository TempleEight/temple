package main

import (
	"github.com/squat/and/dab/booking/dao"
	"github.com/squat/and/dab/booking/util"
)

// Hook allows additional code to be executed before and after every datastore interaction
// Hooks are executed in the order they are defined, such that if any hook errors, future hooks are not executed and the request is terminated
type Hook struct {
	beforeCreateBookingHooks []*func(env *env, input *dao.CreateBookingInput, auth *util.Auth) *HookError
	beforeReadBookingHooks   []*func(env *env, input *dao.ReadBookingInput, auth *util.Auth) *HookError
	beforeDeleteBookingHooks []*func(env *env, input *dao.DeleteBookingInput, auth *util.Auth) *HookError

	afterCreateBookingHooks []*func(env *env, booking *dao.Booking, auth *util.Auth) *HookError
	afterReadBookingHooks   []*func(env *env, booking *dao.Booking, auth *util.Auth) *HookError
	afterDeleteBookingHooks []*func(env *env, auth *util.Auth) *HookError
}

// HookError wraps an existing error with HTTP status code
type HookError struct {
	statusCode int
	error      error
}

func (e *HookError) Error() string {
	return e.error.Error()
}

// BeforeCreateBooking adds a new hook to be executed before creating an object in the datastore
func (h *Hook) BeforeCreateBooking(hook func(env *env, input *dao.CreateBookingInput, auth *util.Auth) *HookError) {
	h.beforeCreateBookingHooks = append(h.beforeCreateBookingHooks, &hook)
}

// BeforeReadBooking adds a new hook to be executed before reading an object in the datastore
func (h *Hook) BeforeReadBooking(hook func(env *env, input *dao.ReadBookingInput, auth *util.Auth) *HookError) {
	h.beforeReadBookingHooks = append(h.beforeReadBookingHooks, &hook)
}

// BeforeDeleteBooking adds a new hook to be executed before deleting an object in the datastore
func (h *Hook) BeforeDeleteBooking(hook func(env *env, input *dao.DeleteBookingInput, auth *util.Auth) *HookError) {
	h.beforeDeleteBookingHooks = append(h.beforeDeleteBookingHooks, &hook)
}

// AfterCreateBooking adds a new hook to be executed after creating an object in the datastore
func (h *Hook) AfterCreateBooking(hook func(env *env, booking *dao.Booking, auth *util.Auth) *HookError) {
	h.afterCreateBookingHooks = append(h.afterCreateBookingHooks, &hook)
}

// AfterReadBooking adds a new hook to be executed after reading an object in the datastore
func (h *Hook) AfterReadBooking(hook func(env *env, booking *dao.Booking, auth *util.Auth) *HookError) {
	h.afterReadBookingHooks = append(h.afterReadBookingHooks, &hook)
}

// AfterDeleteBooking adds a new hook to be executed after deleting an object in the datastore
func (h *Hook) AfterDeleteBooking(hook func(env *env, auth *util.Auth) *HookError) {
	h.afterDeleteBookingHooks = append(h.afterDeleteBookingHooks, &hook)
}
