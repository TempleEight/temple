package main

import "github.com/squat/and/dab/example/dao"

// Hook allows additional code to be executed before and after every datastore interaction
// Hooks are executed in the order they are defined, such that if any hook errors, future hooks are not executed and the request is terminated
type Hook struct {
	beforeCreateExampleHooks []*func(env *env, req createExampleRequest, input *dao.CreateExampleInput) *HookError
	beforeReadExampleHooks   []*func(env *env, input *dao.ReadExampleInput) *HookError
	beforeUpdateExampleHooks []*func(env *env, req updateExampleRequest, input *dao.UpdateExampleInput) *HookError
	beforeDeleteExampleHooks []*func(env *env, input *dao.DeleteExampleInput) *HookError

	afterCreateExampleHooks []*func(env *env, example *dao.Example) *HookError
	afterReadExampleHooks   []*func(env *env, example *dao.Example) *HookError
	afterUpdateExampleHooks []*func(env *env, example *dao.Example) *HookError
	afterDeleteExampleHooks []*func(env *env) *HookError
}

// HookError wraps an existing error with HTTP status code
type HookError struct {
	statusCode int
	error      error
}

func (e *HookError) Error() string {
	return e.error.Error()
}

// BeforeCreateExample adds a new hook to be executed before creating an object in the datastore
func (h *Hook) BeforeCreateExample(hook func(env *env, req createExampleRequest, input *dao.CreateExampleInput) *HookError) {
	h.beforeCreateExampleHooks = append(h.beforeCreateExampleHooks, &hook)
}

// BeforeReadExample adds a new hook to be executed before reading an object in the datastore
func (h *Hook) BeforeReadExample(hook func(env *env, input *dao.ReadExampleInput) *HookError) {
	h.beforeReadExampleHooks = append(h.beforeReadExampleHooks, &hook)
}

// BeforeUpdateExample adds a new hook to be executed before updating an object in the datastore
func (h *Hook) BeforeUpdateExample(hook func(env *env, req updateExampleRequest, input *dao.UpdateExampleInput) *HookError) {
	h.beforeUpdateExampleHooks = append(h.beforeUpdateExampleHooks, &hook)
}

// BeforeDeleteExample adds a new hook to be executed before deleting an object in the datastore
func (h *Hook) BeforeDeleteExample(hook func(env *env, input *dao.DeleteExampleInput) *HookError) {
	h.beforeDeleteExampleHooks = append(h.beforeDeleteExampleHooks, &hook)
}

// AfterCreateExample adds a new hook to be executed after creating an object in the datastore
func (h *Hook) AfterCreateExample(hook func(env *env, example *dao.Example) *HookError) {
	h.afterCreateExampleHooks = append(h.afterCreateExampleHooks, &hook)
}

// AfterReadExample adds a new hook to be executed after reading an object in the datastore
func (h *Hook) AfterReadExample(hook func(env *env, example *dao.Example) *HookError) {
	h.afterReadExampleHooks = append(h.afterReadExampleHooks, &hook)
}

// AfterUpdateExample adds a new hook to be executed after updating an object in the datastore
func (h *Hook) AfterUpdateExample(hook func(env *env, example *dao.Example) *HookError) {
	h.afterUpdateExampleHooks = append(h.afterUpdateExampleHooks, &hook)
}

// AfterDeleteExample adds a new hook to be executed after deleting an object in the datastore
func (h *Hook) AfterDeleteExample(hook func(env *env) *HookError) {
	h.afterDeleteExampleHooks = append(h.afterDeleteExampleHooks, &hook)
}
