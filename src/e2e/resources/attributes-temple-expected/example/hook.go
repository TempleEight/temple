package main

import "github.com/squat/and/dab/example/dao"

// Hook allows additional code to be executed before and after every datastore interaction
// Hooks are executed in the order they are defined, such that if any hook errors, future hooks are not executed and the request is terminated
type Hook struct {
	beforeCreateHooks []*func(env *env, req createExampleRequest, input *dao.CreateExampleInput) *HookError
	beforeReadHooks   []*func(env *env, input *dao.ReadExampleInput) *HookError
	beforeUpdateHooks []*func(env *env, req updateExampleRequest, input *dao.UpdateExampleInput) *HookError
	beforeDeleteHooks []*func(env *env, input *dao.DeleteExampleInput) *HookError

	afterCreateHooks []*func(env *env, example *dao.Example) *HookError
	afterReadHooks   []*func(env *env, example *dao.Example) *HookError
	afterUpdateHooks []*func(env *env, example *dao.Example) *HookError
	afterDeleteHooks []*func(env *env) *HookError
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
func (h *Hook) BeforeCreate(hook func(env *env, req createExampleRequest, input *dao.CreateExampleInput) *HookError) {
	h.beforeCreateHooks = append(h.beforeCreateHooks, &hook)
}

// BeforeRead adds a new hook to be executed before reading an object in the datastore
func (h *Hook) BeforeRead(hook func(env *env, input *dao.ReadExampleInput) *HookError) {
	h.beforeReadHooks = append(h.beforeReadHooks, &hook)
}

// BeforeUpdate adds a new hook to be executed before updating an object in the datastore
func (h *Hook) BeforeUpdate(hook func(env *env, req updateExampleRequest, input *dao.UpdateExampleInput) *HookError) {
	h.beforeUpdateHooks = append(h.beforeUpdateHooks, &hook)
}

// BeforeDelete adds a new hook to be executed before deleting an object in the datastore
func (h *Hook) BeforeDelete(hook func(env *env, input *dao.DeleteExampleInput) *HookError) {
	h.beforeDeleteHooks = append(h.beforeDeleteHooks, &hook)
}

// AfterCreate adds a new hook to be executed after creating an object in the datastore
func (h *Hook) AfterCreate(hook func(env *env, example *dao.Example) *HookError) {
	h.afterCreateHooks = append(h.afterCreateHooks, &hook)
}

// AfterRead adds a new hook to be executed after reading an object in the datastore
func (h *Hook) AfterRead(hook func(env *env, example *dao.Example) *HookError) {
	h.afterReadHooks = append(h.afterReadHooks, &hook)
}

// AfterUpdate adds a new hook to be executed after updating an object in the datastore
func (h *Hook) AfterUpdate(hook func(env *env, example *dao.Example) *HookError) {
	h.afterUpdateHooks = append(h.afterUpdateHooks, &hook)
}

// AfterDelete adds a new hook to be executed after deleting an object in the datastore
func (h *Hook) AfterDelete(hook func(env *env) *HookError) {
	h.afterDeleteHooks = append(h.afterDeleteHooks, &hook)
}
