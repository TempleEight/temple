package main

import (
	"github.com/squat/and/dab/simple-temple-test-user/dao"
	"github.com/squat/and/dab/simple-temple-test-user/util"
)

// Hook allows additional code to be executed before and after every datastore interaction
// Hooks are executed in the order they are defined, such that if any hook errors, future hooks are not executed and the request is terminated
type Hook struct {
	beforeListHooks     []*func(env *env, auth *util.Auth) *HookError
	beforeCreateHooks   []*func(env *env, req createSimpleTempleTestUserRequest, input *dao.CreateSimpleTempleTestUserInput, auth *util.Auth) *HookError
	beforeReadHooks     []*func(env *env, input *dao.ReadSimpleTempleTestUserInput, auth *util.Auth) *HookError
	beforeUpdateHooks   []*func(env *env, req updateSimpleTempleTestUserRequest, input *dao.UpdateSimpleTempleTestUserInput, auth *util.Auth) *HookError
	beforeIdentifyHooks []*func(env *env, input *dao.IdentifySimpleTempleTestUserInput, auth *util.Auth) *HookError

	beforeListFredHooks   []*func(env *env, auth *util.Auth) *HookError
	beforeCreateFredHooks []*func(env *env, req createFredRequest, input *dao.CreateFredInput, auth *util.Auth) *HookError
	beforeReadFredHooks   []*func(env *env, input *dao.ReadFredInput, auth *util.Auth) *HookError
	beforeUpdateFredHooks []*func(env *env, req updateFredRequest, input *dao.UpdateFredInput, auth *util.Auth) *HookError
	beforeDeleteFredHooks []*func(env *env, input *dao.DeleteFredInput, auth *util.Auth) *HookError

	afterListHooks     []*func(env *env, simpleTempleTestUserList *[]dao.SimpleTempleTestUser, auth *util.Auth) *HookError
	afterCreateHooks   []*func(env *env, simpleTempleTestUser *dao.SimpleTempleTestUser, auth *util.Auth) *HookError
	afterReadHooks     []*func(env *env, simpleTempleTestUser *dao.SimpleTempleTestUser, auth *util.Auth) *HookError
	afterUpdateHooks   []*func(env *env, simpleTempleTestUser *dao.SimpleTempleTestUser, auth *util.Auth) *HookError
	afterIdentifyHooks []*func(env *env, simpleTempleTestUser *dao.SimpleTempleTestUser, auth *util.Auth) *HookError

	afterListFredHooks   []*func(env *env, fredList *[]dao.Fred, auth *util.Auth) *HookError
	afterCreateFredHooks []*func(env *env, fred *dao.Fred, auth *util.Auth) *HookError
	afterReadFredHooks   []*func(env *env, fred *dao.Fred, auth *util.Auth) *HookError
	afterUpdateFredHooks []*func(env *env, fred *dao.Fred, auth *util.Auth) *HookError
	afterDeleteFredHooks []*func(env *env, auth *util.Auth) *HookError
}

// HookError wraps an existing error with HTTP status code
type HookError struct {
	statusCode int
	error      error
}

func (e *HookError) Error() string {
	return e.error.Error()
}

// BeforeList adds a new hook to be executed before listing the objects in the datastore
func (h *Hook) BeforeList(hook func(env *env, auth *util.Auth) *HookError) {
	h.beforeListHooks = append(h.beforeListHooks, &hook)
}

// BeforeCreate adds a new hook to be executed before creating an object in the datastore
func (h *Hook) BeforeCreate(hook func(env *env, req createSimpleTempleTestUserRequest, input *dao.CreateSimpleTempleTestUserInput, auth *util.Auth) *HookError) {
	h.beforeCreateHooks = append(h.beforeCreateHooks, &hook)
}

// BeforeRead adds a new hook to be executed before reading an object in the datastore
func (h *Hook) BeforeRead(hook func(env *env, input *dao.ReadSimpleTempleTestUserInput, auth *util.Auth) *HookError) {
	h.beforeReadHooks = append(h.beforeReadHooks, &hook)
}

// BeforeUpdate adds a new hook to be executed before updating an object in the datastore
func (h *Hook) BeforeUpdate(hook func(env *env, req updateSimpleTempleTestUserRequest, input *dao.UpdateSimpleTempleTestUserInput, auth *util.Auth) *HookError) {
	h.beforeUpdateHooks = append(h.beforeUpdateHooks, &hook)
}

// BeforeIdentify adds a new hook to be executed before identifying an object in the datastore
func (h *Hook) BeforeIdentify(hook func(env *env, input *dao.IdentifySimpleTempleTestUserInput, auth *util.Auth) *HookError) {
	h.beforeIdentifyHooks = append(h.beforeIdentifyHooks, &hook)
}

// BeforeListFred adds a new hook to be executed before listing the objects in the datastore
func (h *Hook) BeforeListFred(hook func(env *env, auth *util.Auth) *HookError) {
	h.beforeListFredHooks = append(h.beforeListFredHooks, &hook)
}

// BeforeCreateFred adds a new hook to be executed before creating an object in the datastore
func (h *Hook) BeforeCreateFred(hook func(env *env, req createFredRequest, input *dao.CreateFredInput, auth *util.Auth) *HookError) {
	h.beforeCreateFredHooks = append(h.beforeCreateFredHooks, &hook)
}

// BeforeReadFred adds a new hook to be executed before reading an object in the datastore
func (h *Hook) BeforeReadFred(hook func(env *env, input *dao.ReadFredInput, auth *util.Auth) *HookError) {
	h.beforeReadFredHooks = append(h.beforeReadFredHooks, &hook)
}

// BeforeUpdateFred adds a new hook to be executed before updating an object in the datastore
func (h *Hook) BeforeUpdateFred(hook func(env *env, req updateFredRequest, input *dao.UpdateFredInput, auth *util.Auth) *HookError) {
	h.beforeUpdateFredHooks = append(h.beforeUpdateFredHooks, &hook)
}

// BeforeDeleteFred adds a new hook to be executed before deleting an object in the datastore
func (h *Hook) BeforeDeleteFred(hook func(env *env, input *dao.DeleteFredInput, auth *util.Auth) *HookError) {
	h.beforeDeleteFredHooks = append(h.beforeDeleteFredHooks, &hook)
}

// AfterList adds a new hook to be executed after listing the objects in the datastore
func (h *Hook) AfterList(hook func(env *env, simpleTempleTestUserList *[]dao.SimpleTempleTestUser, auth *util.Auth) *HookError) {
	h.afterListHooks = append(h.afterListHooks, &hook)
}

// AfterCreate adds a new hook to be executed after creating an object in the datastore
func (h *Hook) AfterCreate(hook func(env *env, simpleTempleTestUser *dao.SimpleTempleTestUser, auth *util.Auth) *HookError) {
	h.afterCreateHooks = append(h.afterCreateHooks, &hook)
}

// AfterRead adds a new hook to be executed after reading an object in the datastore
func (h *Hook) AfterRead(hook func(env *env, simpleTempleTestUser *dao.SimpleTempleTestUser, auth *util.Auth) *HookError) {
	h.afterReadHooks = append(h.afterReadHooks, &hook)
}

// AfterUpdate adds a new hook to be executed after updating an object in the datastore
func (h *Hook) AfterUpdate(hook func(env *env, simpleTempleTestUser *dao.SimpleTempleTestUser, auth *util.Auth) *HookError) {
	h.afterUpdateHooks = append(h.afterUpdateHooks, &hook)
}

// AfterIdentify adds a new hook to be executed after identifying an object in the datastore
func (h *Hook) AfterIdentify(hook func(env *env, simpleTempleTestUser *dao.SimpleTempleTestUser, auth *util.Auth) *HookError) {
	h.afterIdentifyHooks = append(h.afterIdentifyHooks, &hook)
}

// AfterListFred adds a new hook to be executed after listing the objects in the datastore
func (h *Hook) AfterListFred(hook func(env *env, fredList *[]dao.Fred, auth *util.Auth) *HookError) {
	h.afterListFredHooks = append(h.afterListFredHooks, &hook)
}

// AfterCreateFred adds a new hook to be executed after creating an object in the datastore
func (h *Hook) AfterCreateFred(hook func(env *env, fred *dao.Fred, auth *util.Auth) *HookError) {
	h.afterCreateFredHooks = append(h.afterCreateFredHooks, &hook)
}

// AfterReadFred adds a new hook to be executed after reading an object in the datastore
func (h *Hook) AfterReadFred(hook func(env *env, fred *dao.Fred, auth *util.Auth) *HookError) {
	h.afterReadFredHooks = append(h.afterReadFredHooks, &hook)
}

// AfterUpdateFred adds a new hook to be executed after updating an object in the datastore
func (h *Hook) AfterUpdateFred(hook func(env *env, fred *dao.Fred, auth *util.Auth) *HookError) {
	h.afterUpdateFredHooks = append(h.afterUpdateFredHooks, &hook)
}

// AfterDeleteFred adds a new hook to be executed after deleting an object in the datastore
func (h *Hook) AfterDeleteFred(hook func(env *env, auth *util.Auth) *HookError) {
	h.afterDeleteFredHooks = append(h.afterDeleteFredHooks, &hook)
}
