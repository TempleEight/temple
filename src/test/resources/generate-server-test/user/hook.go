package main

import (
	"github.com/TempleEight/spec-golang/user/dao"
	"github.com/TempleEight/spec-golang/user/util"
)

// Hook allows additional code to be executed before and after every datastore interaction
// Hooks are executed in the order they are defined, such that if any hook errors, future hooks are not executed and the request is terminated
type Hook struct {
	beforeCreateHooks []*func(env *env, req createUserRequest, input *dao.CreateUserInput, auth *util.Auth) *HookError
	beforeReadHooks   []*func(env *env, input *dao.ReadUserInput, auth *util.Auth) *HookError
	beforeUpdateHooks []*func(env *env, req updateUserRequest, input *dao.UpdateUserInput, auth *util.Auth) *HookError
	beforeDeleteHooks []*func(env *env, input *dao.DeleteUserInput, auth *util.Auth) *HookError

	afterCreateHooks []*func(env *env, user *dao.User, auth *util.Auth) *HookError
	afterReadHooks   []*func(env *env, user *dao.User, auth *util.Auth) *HookError
	afterUpdateHooks []*func(env *env, user *dao.User, auth *util.Auth) *HookError
	afterDeleteHooks []*func(env *env, auth *util.Auth) *HookError
}

// HookError wraps an existing error with HTTP status code
type HookError struct {
	statusCode int
	error      error
}

func (e *HookError) Error() string {
	return e.error.Error()
}

// BeforeCreate adds a new hook to be executed before creating an object in the datastore
func (h *Hook) BeforeCreate(hook func(env *env, req createUserRequest, input *dao.CreateUserInput, auth *util.Auth) *HookError) {
	h.beforeCreateHooks = append(h.beforeCreateHooks, &hook)
}

// BeforeRead adds a new hook to be executed before reading an object in the datastore
func (h *Hook) BeforeRead(hook func(env *env, input *dao.ReadUserInput, auth *util.Auth) *HookError) {
	h.beforeReadHooks = append(h.beforeReadHooks, &hook)
}

// BeforeUpdate adds a new hook to be executed before updating an object in the datastore
func (h *Hook) BeforeUpdate(hook func(env *env, req updateUserRequest, input *dao.UpdateUserInput, auth *util.Auth) *HookError) {
	h.beforeUpdateHooks = append(h.beforeUpdateHooks, &hook)
}

// BeforeDelete adds a new hook to be executed before deleting an object in the datastore
func (h *Hook) BeforeDelete(hook func(env *env, input *dao.DeleteUserInput, auth *util.Auth) *HookError) {
	h.beforeDeleteHooks = append(h.beforeDeleteHooks, &hook)
}

// AfterCreate adds a new hook to be executed after creating an object in the datastore
func (h *Hook) AfterCreate(hook func(env *env, user *dao.User, auth *util.Auth) *HookError) {
	h.afterCreateHooks = append(h.afterCreateHooks, &hook)
}

// AfterRead adds a new hook to be executed after reading an object in the datastore
func (h *Hook) AfterRead(hook func(env *env, user *dao.User, auth *util.Auth) *HookError) {
	h.afterReadHooks = append(h.afterReadHooks, &hook)
}

// AfterUpdate adds a new hook to be executed after updating an object in the datastore
func (h *Hook) AfterUpdate(hook func(env *env, user *dao.User, auth *util.Auth) *HookError) {
	h.afterUpdateHooks = append(h.afterUpdateHooks, &hook)
}

// AfterDelete adds a new hook to be executed after deleting an object in the datastore
func (h *Hook) AfterDelete(hook func(env *env, auth *util.Auth) *HookError) {
	h.afterDeleteHooks = append(h.afterDeleteHooks, &hook)
}
