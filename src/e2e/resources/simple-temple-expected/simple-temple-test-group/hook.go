package main

import (
	"github.com/squat/and/dab/simple-temple-test-group/dao"
	"github.com/squat/and/dab/simple-temple-test-group/util"
)

// Hook allows additional code to be executed before and after every datastore interaction
// Hooks are executed in the order they are defined, such that if any hook errors, future hooks are not executed and the request is terminated
type Hook struct {
	beforeCreateSimpleTempleTestGroupHooks []*func(env *env, input *dao.CreateSimpleTempleTestGroupInput, auth *util.Auth) *HookError
	beforeReadSimpleTempleTestGroupHooks   []*func(env *env, input *dao.ReadSimpleTempleTestGroupInput, auth *util.Auth) *HookError
	beforeDeleteSimpleTempleTestGroupHooks []*func(env *env, input *dao.DeleteSimpleTempleTestGroupInput, auth *util.Auth) *HookError

	afterCreateSimpleTempleTestGroupHooks []*func(env *env, simpleTempleTestGroup *dao.SimpleTempleTestGroup, auth *util.Auth) *HookError
	afterReadSimpleTempleTestGroupHooks   []*func(env *env, simpleTempleTestGroup *dao.SimpleTempleTestGroup, auth *util.Auth) *HookError
	afterDeleteSimpleTempleTestGroupHooks []*func(env *env, auth *util.Auth) *HookError
}

// HookError wraps an existing error with HTTP status code
type HookError struct {
	statusCode int
	error      error
}

func (e *HookError) Error() string {
	return e.error.Error()
}

// BeforeCreateSimpleTempleTestGroup adds a new hook to be executed before creating an object in the datastore
func (h *Hook) BeforeCreateSimpleTempleTestGroup(hook func(env *env, input *dao.CreateSimpleTempleTestGroupInput, auth *util.Auth) *HookError) {
	h.beforeCreateSimpleTempleTestGroupHooks = append(h.beforeCreateSimpleTempleTestGroupHooks, &hook)
}

// BeforeReadSimpleTempleTestGroup adds a new hook to be executed before reading an object in the datastore
func (h *Hook) BeforeReadSimpleTempleTestGroup(hook func(env *env, input *dao.ReadSimpleTempleTestGroupInput, auth *util.Auth) *HookError) {
	h.beforeReadSimpleTempleTestGroupHooks = append(h.beforeReadSimpleTempleTestGroupHooks, &hook)
}

// BeforeDeleteSimpleTempleTestGroup adds a new hook to be executed before deleting an object in the datastore
func (h *Hook) BeforeDeleteSimpleTempleTestGroup(hook func(env *env, input *dao.DeleteSimpleTempleTestGroupInput, auth *util.Auth) *HookError) {
	h.beforeDeleteSimpleTempleTestGroupHooks = append(h.beforeDeleteSimpleTempleTestGroupHooks, &hook)
}

// AfterCreateSimpleTempleTestGroup adds a new hook to be executed after creating an object in the datastore
func (h *Hook) AfterCreateSimpleTempleTestGroup(hook func(env *env, simpleTempleTestGroup *dao.SimpleTempleTestGroup, auth *util.Auth) *HookError) {
	h.afterCreateSimpleTempleTestGroupHooks = append(h.afterCreateSimpleTempleTestGroupHooks, &hook)
}

// AfterReadSimpleTempleTestGroup adds a new hook to be executed after reading an object in the datastore
func (h *Hook) AfterReadSimpleTempleTestGroup(hook func(env *env, simpleTempleTestGroup *dao.SimpleTempleTestGroup, auth *util.Auth) *HookError) {
	h.afterReadSimpleTempleTestGroupHooks = append(h.afterReadSimpleTempleTestGroupHooks, &hook)
}

// AfterDeleteSimpleTempleTestGroup adds a new hook to be executed after deleting an object in the datastore
func (h *Hook) AfterDeleteSimpleTempleTestGroup(hook func(env *env, auth *util.Auth) *HookError) {
	h.afterDeleteSimpleTempleTestGroupHooks = append(h.afterDeleteSimpleTempleTestGroupHooks, &hook)
}
