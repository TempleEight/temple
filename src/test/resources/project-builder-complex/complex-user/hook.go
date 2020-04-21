package main

import (
	"github.com/squat/and/dab/complex-user/dao"
	"github.com/squat/and/dab/complex-user/util"
)

// Hook allows additional code to be executed before and after every datastore interaction
// Hooks are executed in the order they are defined, such that if any hook errors, future hooks are not executed and the request is terminated
type Hook struct {
	beforeCreateHooks   []*func(env *env, req createComplexUserRequest, input *dao.CreateComplexUserInput, auth *util.Auth) *HookError
	beforeReadHooks     []*func(env *env, input *dao.ReadComplexUserInput, auth *util.Auth) *HookError
	beforeUpdateHooks   []*func(env *env, req updateComplexUserRequest, input *dao.UpdateComplexUserInput, auth *util.Auth) *HookError
	beforeDeleteHooks   []*func(env *env, input *dao.DeleteComplexUserInput, auth *util.Auth) *HookError
	beforeIdentifyHooks []*func(env *env, input *dao.IdentifyComplexUserInput, auth *util.Auth) *HookError

	beforeCreateTempleUserHooks []*func(env *env, req createTempleUserRequest, input *dao.CreateTempleUserInput, auth *util.Auth) *HookError
	beforeReadTempleUserHooks   []*func(env *env, input *dao.ReadTempleUserInput, auth *util.Auth) *HookError
	beforeUpdateTempleUserHooks []*func(env *env, req updateTempleUserRequest, input *dao.UpdateTempleUserInput, auth *util.Auth) *HookError
	beforeDeleteTempleUserHooks []*func(env *env, input *dao.DeleteTempleUserInput, auth *util.Auth) *HookError

	afterCreateHooks   []*func(env *env, complexUser *dao.ComplexUser, auth *util.Auth) *HookError
	afterReadHooks     []*func(env *env, complexUser *dao.ComplexUser, auth *util.Auth) *HookError
	afterUpdateHooks   []*func(env *env, complexUser *dao.ComplexUser, auth *util.Auth) *HookError
	afterDeleteHooks   []*func(env *env, auth *util.Auth) *HookError
	afterIdentifyHooks []*func(env *env, complexUser *dao.ComplexUser, auth *util.Auth) *HookError

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

// BeforeCreate adds a new hook to be executed before creating an object in the datastore
func (h *Hook) BeforeCreate(hook func(env *env, req createComplexUserRequest, input *dao.CreateComplexUserInput, auth *util.Auth) *HookError) {
	h.beforeCreateHooks = append(h.beforeCreateHooks, &hook)
}

// BeforeRead adds a new hook to be executed before reading an object in the datastore
func (h *Hook) BeforeRead(hook func(env *env, input *dao.ReadComplexUserInput, auth *util.Auth) *HookError) {
	h.beforeReadHooks = append(h.beforeReadHooks, &hook)
}

// BeforeUpdate adds a new hook to be executed before updating an object in the datastore
func (h *Hook) BeforeUpdate(hook func(env *env, req updateComplexUserRequest, input *dao.UpdateComplexUserInput, auth *util.Auth) *HookError) {
	h.beforeUpdateHooks = append(h.beforeUpdateHooks, &hook)
}

// BeforeDelete adds a new hook to be executed before deleting an object in the datastore
func (h *Hook) BeforeDelete(hook func(env *env, input *dao.DeleteComplexUserInput, auth *util.Auth) *HookError) {
	h.beforeDeleteHooks = append(h.beforeDeleteHooks, &hook)
}

// BeforeIdentify adds a new hook to be executed before identifying an object in the datastore
func (h *Hook) BeforeIdentify(hook func(env *env, input *dao.IdentifyComplexUserInput, auth *util.Auth) *HookError) {
	h.beforeIdentifyHooks = append(h.beforeIdentifyHooks, &hook)
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

// AfterCreate adds a new hook to be executed after creating an object in the datastore
func (h *Hook) AfterCreate(hook func(env *env, complexUser *dao.ComplexUser, auth *util.Auth) *HookError) {
	h.afterCreateHooks = append(h.afterCreateHooks, &hook)
}

// AfterRead adds a new hook to be executed after reading an object in the datastore
func (h *Hook) AfterRead(hook func(env *env, complexUser *dao.ComplexUser, auth *util.Auth) *HookError) {
	h.afterReadHooks = append(h.afterReadHooks, &hook)
}

// AfterUpdate adds a new hook to be executed after updating an object in the datastore
func (h *Hook) AfterUpdate(hook func(env *env, complexUser *dao.ComplexUser, auth *util.Auth) *HookError) {
	h.afterUpdateHooks = append(h.afterUpdateHooks, &hook)
}

// AfterDelete adds a new hook to be executed after deleting an object in the datastore
func (h *Hook) AfterDelete(hook func(env *env, auth *util.Auth) *HookError) {
	h.afterDeleteHooks = append(h.afterDeleteHooks, &hook)
}

// AfterIdentify adds a new hook to be executed after identifying an object in the datastore
func (h *Hook) AfterIdentify(hook func(env *env, complexUser *dao.ComplexUser, auth *util.Auth) *HookError) {
	h.afterIdentifyHooks = append(h.afterIdentifyHooks, &hook)
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
