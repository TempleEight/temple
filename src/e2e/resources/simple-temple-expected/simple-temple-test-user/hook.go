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
  
	afterListHooks     []*func(env *env, simpleTempleTestUserList *[]dao.SimpleTempleTestUser, auth *util.Auth) *HookError
	afterCreateHooks   []*func(env *env, simpleTempleTestUser *dao.SimpleTempleTestUser, auth *util.Auth) *HookError
	afterReadHooks     []*func(env *env, simpleTempleTestUser *dao.SimpleTempleTestUser, auth *util.Auth) *HookError
	afterUpdateHooks   []*func(env *env, simpleTempleTestUser *dao.SimpleTempleTestUser, auth *util.Auth) *HookError
	afterIdentifyHooks []*func(env *env, simpleTempleTestUser *dao.SimpleTempleTestUser, auth *util.Auth) *HookError
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
