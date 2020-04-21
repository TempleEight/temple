package main

import "github.com/squat/and/dab/temple-user/dao"

// Hook allows additional code to be executed before and after every datastore interaction
// Hooks are executed in the order they are defined, such that if any hook errors, future hooks are not executed and the request is terminated
type Hook struct {
	beforeCreateTempleUserHooks []*func(env *env, req createTempleUserRequest, input *dao.CreateTempleUserInput) *HookError
	beforeReadTempleUserHooks   []*func(env *env, input *dao.ReadTempleUserInput) *HookError
	beforeUpdateTempleUserHooks []*func(env *env, req updateTempleUserRequest, input *dao.UpdateTempleUserInput) *HookError
	beforeDeleteTempleUserHooks []*func(env *env, input *dao.DeleteTempleUserInput) *HookError

	afterCreateTempleUserHooks []*func(env *env, templeUser *dao.TempleUser) *HookError
	afterReadTempleUserHooks   []*func(env *env, templeUser *dao.TempleUser) *HookError
	afterUpdateTempleUserHooks []*func(env *env, templeUser *dao.TempleUser) *HookError
	afterDeleteTempleUserHooks []*func(env *env) *HookError
}

// HookError wraps an existing error with HTTP status code
type HookError struct {
	statusCode int
	error      error
}

func (e *HookError) Error() string {
	return e.error.Error()
}

// BeforeCreateTempleUser adds a new hook to be executed before creating an object in the datastore
func (h *Hook) BeforeCreateTempleUser(hook func(env *env, req createTempleUserRequest, input *dao.CreateTempleUserInput) *HookError) {
	h.beforeCreateTempleUserHooks = append(h.beforeCreateTempleUserHooks, &hook)
}

// BeforeReadTempleUser adds a new hook to be executed before reading an object in the datastore
func (h *Hook) BeforeReadTempleUser(hook func(env *env, input *dao.ReadTempleUserInput) *HookError) {
	h.beforeReadTempleUserHooks = append(h.beforeReadTempleUserHooks, &hook)
}

// BeforeUpdateTempleUser adds a new hook to be executed before updating an object in the datastore
func (h *Hook) BeforeUpdateTempleUser(hook func(env *env, req updateTempleUserRequest, input *dao.UpdateTempleUserInput) *HookError) {
	h.beforeUpdateTempleUserHooks = append(h.beforeUpdateTempleUserHooks, &hook)
}

// BeforeDeleteTempleUser adds a new hook to be executed before deleting an object in the datastore
func (h *Hook) BeforeDeleteTempleUser(hook func(env *env, input *dao.DeleteTempleUserInput) *HookError) {
	h.beforeDeleteTempleUserHooks = append(h.beforeDeleteTempleUserHooks, &hook)
}

// AfterCreateTempleUser adds a new hook to be executed after creating an object in the datastore
func (h *Hook) AfterCreateTempleUser(hook func(env *env, templeUser *dao.TempleUser) *HookError) {
	h.afterCreateTempleUserHooks = append(h.afterCreateTempleUserHooks, &hook)
}

// AfterReadTempleUser adds a new hook to be executed after reading an object in the datastore
func (h *Hook) AfterReadTempleUser(hook func(env *env, templeUser *dao.TempleUser) *HookError) {
	h.afterReadTempleUserHooks = append(h.afterReadTempleUserHooks, &hook)
}

// AfterUpdateTempleUser adds a new hook to be executed after updating an object in the datastore
func (h *Hook) AfterUpdateTempleUser(hook func(env *env, templeUser *dao.TempleUser) *HookError) {
	h.afterUpdateTempleUserHooks = append(h.afterUpdateTempleUserHooks, &hook)
}

// AfterDeleteTempleUser adds a new hook to be executed after deleting an object in the datastore
func (h *Hook) AfterDeleteTempleUser(hook func(env *env) *HookError) {
	h.afterDeleteTempleUserHooks = append(h.afterDeleteTempleUserHooks, &hook)
}
