package main

import (
	"github.com/TempleEight/spec-golang/user/dao"
	"github.com/TempleEight/spec-golang/user/util"
)

// Hook allows additional code to be executed before and after every datastore interaction
// Hooks are executed in the order they are defined, such that if any hook errors, future hooks are not executed and the request is terminated
type Hook struct {
	beforeCreateUserHooks []*func(env *env, req createUserRequest, input *dao.CreateUserInput, auth *util.Auth) *HookError
	beforeReadUserHooks   []*func(env *env, input *dao.ReadUserInput, auth *util.Auth) *HookError
	beforeUpdateUserHooks []*func(env *env, req updateUserRequest, input *dao.UpdateUserInput, auth *util.Auth) *HookError
	beforeDeleteUserHooks []*func(env *env, input *dao.DeleteUserInput, auth *util.Auth) *HookError

	afterCreateUserHooks []*func(env *env, user *dao.User, auth *util.Auth) *HookError
	afterReadUserHooks   []*func(env *env, user *dao.User, auth *util.Auth) *HookError
	afterUpdateUserHooks []*func(env *env, user *dao.User, auth *util.Auth) *HookError
	afterDeleteUserHooks []*func(env *env, auth *util.Auth) *HookError
}

// HookError wraps an existing error with HTTP status code
type HookError struct {
	statusCode int
	error      error
}

func (e *HookError) Error() string {
	return e.error.Error()
}

// BeforeCreateUser adds a new hook to be executed before creating an object in the datastore
func (h *Hook) BeforeCreateUser(hook func(env *env, req createUserRequest, input *dao.CreateUserInput, auth *util.Auth) *HookError) {
	h.beforeCreateUserHooks = append(h.beforeCreateUserHooks, &hook)
}

// BeforeReadUser adds a new hook to be executed before reading an object in the datastore
func (h *Hook) BeforeReadUser(hook func(env *env, input *dao.ReadUserInput, auth *util.Auth) *HookError) {
	h.beforeReadUserHooks = append(h.beforeReadUserHooks, &hook)
}

// BeforeUpdateUser adds a new hook to be executed before updating an object in the datastore
func (h *Hook) BeforeUpdateUser(hook func(env *env, req updateUserRequest, input *dao.UpdateUserInput, auth *util.Auth) *HookError) {
	h.beforeUpdateUserHooks = append(h.beforeUpdateUserHooks, &hook)
}

// BeforeDeleteUser adds a new hook to be executed before deleting an object in the datastore
func (h *Hook) BeforeDeleteUser(hook func(env *env, input *dao.DeleteUserInput, auth *util.Auth) *HookError) {
	h.beforeDeleteUserHooks = append(h.beforeDeleteUserHooks, &hook)
}

// AfterCreateUser adds a new hook to be executed after creating an object in the datastore
func (h *Hook) AfterCreateUser(hook func(env *env, user *dao.User, auth *util.Auth) *HookError) {
	h.afterCreateUserHooks = append(h.afterCreateUserHooks, &hook)
}

// AfterReadUser adds a new hook to be executed after reading an object in the datastore
func (h *Hook) AfterReadUser(hook func(env *env, user *dao.User, auth *util.Auth) *HookError) {
	h.afterReadUserHooks = append(h.afterReadUserHooks, &hook)
}

// AfterUpdateUser adds a new hook to be executed after updating an object in the datastore
func (h *Hook) AfterUpdateUser(hook func(env *env, user *dao.User, auth *util.Auth) *HookError) {
	h.afterUpdateUserHooks = append(h.afterUpdateUserHooks, &hook)
}

// AfterDeleteUser adds a new hook to be executed after deleting an object in the datastore
func (h *Hook) AfterDeleteUser(hook func(env *env, auth *util.Auth) *HookError) {
	h.afterDeleteUserHooks = append(h.afterDeleteUserHooks, &hook)
}
