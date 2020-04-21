package main

import (
	"github.com/squat/and/dab/simple-temple-test-user/dao"
	"github.com/squat/and/dab/simple-temple-test-user/util"
)

// Hook allows additional code to be executed before and after every datastore interaction
// Hooks are executed in the order they are defined, such that if any hook errors, future hooks are not executed and the request is terminated
type Hook struct {
	beforeListSimpleTempleTestUserHooks     []*func(env *env, auth *util.Auth) *HookError
	beforeCreateSimpleTempleTestUserHooks   []*func(env *env, req createSimpleTempleTestUserRequest, input *dao.CreateSimpleTempleTestUserInput, auth *util.Auth) *HookError
	beforeReadSimpleTempleTestUserHooks     []*func(env *env, input *dao.ReadSimpleTempleTestUserInput, auth *util.Auth) *HookError
	beforeUpdateSimpleTempleTestUserHooks   []*func(env *env, req updateSimpleTempleTestUserRequest, input *dao.UpdateSimpleTempleTestUserInput, auth *util.Auth) *HookError
	beforeIdentifySimpleTempleTestUserHooks []*func(env *env, input *dao.IdentifySimpleTempleTestUserInput, auth *util.Auth) *HookError

	afterListSimpleTempleTestUserHooks     []*func(env *env, simpleTempleTestUserList *[]dao.SimpleTempleTestUser, auth *util.Auth) *HookError
	afterCreateSimpleTempleTestUserHooks   []*func(env *env, simpleTempleTestUser *dao.SimpleTempleTestUser, auth *util.Auth) *HookError
	afterReadSimpleTempleTestUserHooks     []*func(env *env, simpleTempleTestUser *dao.SimpleTempleTestUser, auth *util.Auth) *HookError
	afterUpdateSimpleTempleTestUserHooks   []*func(env *env, simpleTempleTestUser *dao.SimpleTempleTestUser, auth *util.Auth) *HookError
	afterIdentifySimpleTempleTestUserHooks []*func(env *env, simpleTempleTestUser *dao.SimpleTempleTestUser, auth *util.Auth) *HookError
}

// HookError wraps an existing error with HTTP status code
type HookError struct {
	statusCode int
	error      error
}

func (e *HookError) Error() string {
	return e.error.Error()
}

// BeforeListSimpleTempleTestUser adds a new hook to be executed before listing the objects in the datastore
func (h *Hook) BeforeListSimpleTempleTestUser(hook func(env *env, auth *util.Auth) *HookError) {
	h.beforeListSimpleTempleTestUserHooks = append(h.beforeListSimpleTempleTestUserHooks, &hook)
}

// BeforeCreateSimpleTempleTestUser adds a new hook to be executed before creating an object in the datastore
func (h *Hook) BeforeCreateSimpleTempleTestUser(hook func(env *env, req createSimpleTempleTestUserRequest, input *dao.CreateSimpleTempleTestUserInput, auth *util.Auth) *HookError) {
	h.beforeCreateSimpleTempleTestUserHooks = append(h.beforeCreateSimpleTempleTestUserHooks, &hook)
}

// BeforeReadSimpleTempleTestUser adds a new hook to be executed before reading an object in the datastore
func (h *Hook) BeforeReadSimpleTempleTestUser(hook func(env *env, input *dao.ReadSimpleTempleTestUserInput, auth *util.Auth) *HookError) {
	h.beforeReadSimpleTempleTestUserHooks = append(h.beforeReadSimpleTempleTestUserHooks, &hook)
}

// BeforeUpdateSimpleTempleTestUser adds a new hook to be executed before updating an object in the datastore
func (h *Hook) BeforeUpdateSimpleTempleTestUser(hook func(env *env, req updateSimpleTempleTestUserRequest, input *dao.UpdateSimpleTempleTestUserInput, auth *util.Auth) *HookError) {
	h.beforeUpdateSimpleTempleTestUserHooks = append(h.beforeUpdateSimpleTempleTestUserHooks, &hook)
}

// BeforeIdentifySimpleTempleTestUser adds a new hook to be executed before identifying an object in the datastore
func (h *Hook) BeforeIdentifySimpleTempleTestUser(hook func(env *env, input *dao.IdentifySimpleTempleTestUserInput, auth *util.Auth) *HookError) {
	h.beforeIdentifySimpleTempleTestUserHooks = append(h.beforeIdentifySimpleTempleTestUserHooks, &hook)
}

// AfterListSimpleTempleTestUser adds a new hook to be executed after listing the objects in the datastore
func (h *Hook) AfterListSimpleTempleTestUser(hook func(env *env, simpleTempleTestUserList *[]dao.SimpleTempleTestUser, auth *util.Auth) *HookError) {
	h.afterListSimpleTempleTestUserHooks = append(h.afterListSimpleTempleTestUserHooks, &hook)
}

// AfterCreateSimpleTempleTestUser adds a new hook to be executed after creating an object in the datastore
func (h *Hook) AfterCreateSimpleTempleTestUser(hook func(env *env, simpleTempleTestUser *dao.SimpleTempleTestUser, auth *util.Auth) *HookError) {
	h.afterCreateSimpleTempleTestUserHooks = append(h.afterCreateSimpleTempleTestUserHooks, &hook)
}

// AfterReadSimpleTempleTestUser adds a new hook to be executed after reading an object in the datastore
func (h *Hook) AfterReadSimpleTempleTestUser(hook func(env *env, simpleTempleTestUser *dao.SimpleTempleTestUser, auth *util.Auth) *HookError) {
	h.afterReadSimpleTempleTestUserHooks = append(h.afterReadSimpleTempleTestUserHooks, &hook)
}

// AfterUpdateSimpleTempleTestUser adds a new hook to be executed after updating an object in the datastore
func (h *Hook) AfterUpdateSimpleTempleTestUser(hook func(env *env, simpleTempleTestUser *dao.SimpleTempleTestUser, auth *util.Auth) *HookError) {
	h.afterUpdateSimpleTempleTestUserHooks = append(h.afterUpdateSimpleTempleTestUserHooks, &hook)
}

// AfterIdentifySimpleTempleTestUser adds a new hook to be executed after identifying an object in the datastore
func (h *Hook) AfterIdentifySimpleTempleTestUser(hook func(env *env, simpleTempleTestUser *dao.SimpleTempleTestUser, auth *util.Auth) *HookError) {
	h.afterIdentifySimpleTempleTestUserHooks = append(h.afterIdentifySimpleTempleTestUserHooks, &hook)
}
