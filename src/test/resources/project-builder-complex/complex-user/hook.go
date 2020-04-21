package main

import (
	"github.com/squat/and/dab/complex-user/dao"
	"github.com/squat/and/dab/complex-user/util"
)

// Hook allows additional code to be executed before and after every datastore interaction
// Hooks are executed in the order they are defined, such that if any hook errors, future hooks are not executed and the request is terminated
type Hook struct {
	beforeCreateComplexUserHooks   []*func(env *env, req createComplexUserRequest, input *dao.CreateComplexUserInput, auth *util.Auth) *HookError
	beforeReadComplexUserHooks     []*func(env *env, input *dao.ReadComplexUserInput, auth *util.Auth) *HookError
	beforeUpdateComplexUserHooks   []*func(env *env, req updateComplexUserRequest, input *dao.UpdateComplexUserInput, auth *util.Auth) *HookError
	beforeDeleteComplexUserHooks   []*func(env *env, input *dao.DeleteComplexUserInput, auth *util.Auth) *HookError
	beforeIdentifyComplexUserHooks []*func(env *env, input *dao.IdentifyComplexUserInput, auth *util.Auth) *HookError

	beforeCreateTempleUserHooks []*func(env *env, req createTempleUserRequest, input *dao.CreateTempleUserInput, auth *util.Auth) *HookError
	beforeReadTempleUserHooks   []*func(env *env, input *dao.ReadTempleUserInput, auth *util.Auth) *HookError
	beforeUpdateTempleUserHooks []*func(env *env, req updateTempleUserRequest, input *dao.UpdateTempleUserInput, auth *util.Auth) *HookError
	beforeDeleteTempleUserHooks []*func(env *env, input *dao.DeleteTempleUserInput, auth *util.Auth) *HookError

	afterCreateComplexUserHooks   []*func(env *env, complexUser *dao.ComplexUser, auth *util.Auth) *HookError
	afterReadComplexUserHooks     []*func(env *env, complexUser *dao.ComplexUser, auth *util.Auth) *HookError
	afterUpdateComplexUserHooks   []*func(env *env, complexUser *dao.ComplexUser, auth *util.Auth) *HookError
	afterDeleteComplexUserHooks   []*func(env *env, auth *util.Auth) *HookError
	afterIdentifyComplexUserHooks []*func(env *env, complexUser *dao.ComplexUser, auth *util.Auth) *HookError

	afterCreateTempleUserHooks []*func(env *env, templeUser *dao.TempleUser, auth *util.Auth) *HookError
	afterReadTempleUserHooks   []*func(env *env, templeUser *dao.TempleUser, auth *util.Auth) *HookError
	afterUpdateTempleUserHooks []*func(env *env, templeUser *dao.TempleUser, auth *util.Auth) *HookError
	afterDeleteTempleUserHooks []*func(env *env, auth *util.Auth) *HookError
}

// HookError wraps an existing error with HTTP status code
type HookError struct {
	statusCode int
	error      error
}

func (e *HookError) Error() string {
	return e.error.Error()
}

// BeforeCreateComplexUser adds a new hook to be executed before creating an object in the datastore
func (h *Hook) BeforeCreateComplexUser(hook func(env *env, req createComplexUserRequest, input *dao.CreateComplexUserInput, auth *util.Auth) *HookError) {
	h.beforeCreateComplexUserHooks = append(h.beforeCreateComplexUserHooks, &hook)
}

// BeforeReadComplexUser adds a new hook to be executed before reading an object in the datastore
func (h *Hook) BeforeReadComplexUser(hook func(env *env, input *dao.ReadComplexUserInput, auth *util.Auth) *HookError) {
	h.beforeReadComplexUserHooks = append(h.beforeReadComplexUserHooks, &hook)
}

// BeforeUpdateComplexUser adds a new hook to be executed before updating an object in the datastore
func (h *Hook) BeforeUpdateComplexUser(hook func(env *env, req updateComplexUserRequest, input *dao.UpdateComplexUserInput, auth *util.Auth) *HookError) {
	h.beforeUpdateComplexUserHooks = append(h.beforeUpdateComplexUserHooks, &hook)
}

// BeforeDeleteComplexUser adds a new hook to be executed before deleting an object in the datastore
func (h *Hook) BeforeDeleteComplexUser(hook func(env *env, input *dao.DeleteComplexUserInput, auth *util.Auth) *HookError) {
	h.beforeDeleteComplexUserHooks = append(h.beforeDeleteComplexUserHooks, &hook)
}

// BeforeIdentifyComplexUser adds a new hook to be executed before identifying an object in the datastore
func (h *Hook) BeforeIdentifyComplexUser(hook func(env *env, input *dao.IdentifyComplexUserInput, auth *util.Auth) *HookError) {
	h.beforeIdentifyComplexUserHooks = append(h.beforeIdentifyComplexUserHooks, &hook)
}

// BeforeCreateTempleUser adds a new hook to be executed before creating an object in the datastore
func (h *Hook) BeforeCreateTempleUser(hook func(env *env, req createTempleUserRequest, input *dao.CreateTempleUserInput, auth *util.Auth) *HookError) {
	h.beforeCreateTempleUserHooks = append(h.beforeCreateTempleUserHooks, &hook)
}

// BeforeReadTempleUser adds a new hook to be executed before reading an object in the datastore
func (h *Hook) BeforeReadTempleUser(hook func(env *env, input *dao.ReadTempleUserInput, auth *util.Auth) *HookError) {
	h.beforeReadTempleUserHooks = append(h.beforeReadTempleUserHooks, &hook)
}

// BeforeUpdateTempleUser adds a new hook to be executed before updating an object in the datastore
func (h *Hook) BeforeUpdateTempleUser(hook func(env *env, req updateTempleUserRequest, input *dao.UpdateTempleUserInput, auth *util.Auth) *HookError) {
	h.beforeUpdateTempleUserHooks = append(h.beforeUpdateTempleUserHooks, &hook)
}

// BeforeDeleteTempleUser adds a new hook to be executed before deleting an object in the datastore
func (h *Hook) BeforeDeleteTempleUser(hook func(env *env, input *dao.DeleteTempleUserInput, auth *util.Auth) *HookError) {
	h.beforeDeleteTempleUserHooks = append(h.beforeDeleteTempleUserHooks, &hook)
}

// AfterCreateComplexUser adds a new hook to be executed after creating an object in the datastore
func (h *Hook) AfterCreateComplexUser(hook func(env *env, complexUser *dao.ComplexUser, auth *util.Auth) *HookError) {
	h.afterCreateComplexUserHooks = append(h.afterCreateComplexUserHooks, &hook)
}

// AfterReadComplexUser adds a new hook to be executed after reading an object in the datastore
func (h *Hook) AfterReadComplexUser(hook func(env *env, complexUser *dao.ComplexUser, auth *util.Auth) *HookError) {
	h.afterReadComplexUserHooks = append(h.afterReadComplexUserHooks, &hook)
}

// AfterUpdateComplexUser adds a new hook to be executed after updating an object in the datastore
func (h *Hook) AfterUpdateComplexUser(hook func(env *env, complexUser *dao.ComplexUser, auth *util.Auth) *HookError) {
	h.afterUpdateComplexUserHooks = append(h.afterUpdateComplexUserHooks, &hook)
}

// AfterDeleteComplexUser adds a new hook to be executed after deleting an object in the datastore
func (h *Hook) AfterDeleteComplexUser(hook func(env *env, auth *util.Auth) *HookError) {
	h.afterDeleteComplexUserHooks = append(h.afterDeleteComplexUserHooks, &hook)
}

// AfterIdentifyComplexUser adds a new hook to be executed after identifying an object in the datastore
func (h *Hook) AfterIdentifyComplexUser(hook func(env *env, complexUser *dao.ComplexUser, auth *util.Auth) *HookError) {
	h.afterIdentifyComplexUserHooks = append(h.afterIdentifyComplexUserHooks, &hook)
}

// AfterCreateTempleUser adds a new hook to be executed after creating an object in the datastore
func (h *Hook) AfterCreateTempleUser(hook func(env *env, templeUser *dao.TempleUser, auth *util.Auth) *HookError) {
	h.afterCreateTempleUserHooks = append(h.afterCreateTempleUserHooks, &hook)
}

// AfterReadTempleUser adds a new hook to be executed after reading an object in the datastore
func (h *Hook) AfterReadTempleUser(hook func(env *env, templeUser *dao.TempleUser, auth *util.Auth) *HookError) {
	h.afterReadTempleUserHooks = append(h.afterReadTempleUserHooks, &hook)
}

// AfterUpdateTempleUser adds a new hook to be executed after updating an object in the datastore
func (h *Hook) AfterUpdateTempleUser(hook func(env *env, templeUser *dao.TempleUser, auth *util.Auth) *HookError) {
	h.afterUpdateTempleUserHooks = append(h.afterUpdateTempleUserHooks, &hook)
}

// AfterDeleteTempleUser adds a new hook to be executed after deleting an object in the datastore
func (h *Hook) AfterDeleteTempleUser(hook func(env *env, auth *util.Auth) *HookError) {
	h.afterDeleteTempleUserHooks = append(h.afterDeleteTempleUserHooks, &hook)
}
