package main

import "github.com/TempleEight/spec-golang/match/dao"

// Hook allows additional code to be executed before and after every datastore interaction
// Hooks are executed in the order they are defined, such that if any hook errors, future hooks are not executed and the request is terminated
type Hook struct {
	beforeListHooks   []*func(env *env, input *dao.ListMatchInput) *HookError
	beforeCreateHooks []*func(env *env, req createMatchRequest, input *dao.CreateMatchInput) *HookError
	beforeReadHooks   []*func(env *env, input *dao.ReadMatchInput) *HookError
	beforeUpdateHooks []*func(env *env, req updateMatchRequest, input *dao.UpdateMatchInput) *HookError
	beforeDeleteHooks []*func(env *env, input *dao.DeleteMatchInput) *HookError

	afterListHooks   []*func(env *env, matchList *[]dao.Match) *HookError
	afterCreateHooks []*func(env *env, match *dao.Match) *HookError
	afterReadHooks   []*func(env *env, match *dao.Match) *HookError
	afterUpdateHooks []*func(env *env, match *dao.Match) *HookError
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

// BeforeList adds a new hook to be executed before listing the objects in the datastore
func (h *Hook) BeforeList(hook func(env *env, input *dao.ListMatchInput) *HookError) {
	h.beforeListHooks = append(h.beforeListHooks, &hook)
}

// BeforeCreate adds a new hook to be executed before creating an object in the datastore
func (h *Hook) BeforeCreate(hook func(env *env, req createMatchRequest, input *dao.CreateMatchInput) *HookError) {
	h.beforeCreateHooks = append(h.beforeCreateHooks, &hook)
}

// BeforeRead adds a new hook to be executed before reading an object in the datastore
func (h *Hook) BeforeRead(hook func(env *env, input *dao.ReadMatchInput) *HookError) {
	h.beforeReadHooks = append(h.beforeReadHooks, &hook)
}

// BeforeUpdate adds a new hook to be executed before updating an object in the datastore
func (h *Hook) BeforeUpdate(hook func(env *env, req updateMatchRequest, input *dao.UpdateMatchInput) *HookError) {
	h.beforeUpdateHooks = append(h.beforeUpdateHooks, &hook)
}

// BeforeDelete adds a new hook to be executed before deleting an object in the datastore
func (h *Hook) BeforeDelete(hook func(env *env, input *dao.DeleteMatchInput) *HookError) {
	h.beforeDeleteHooks = append(h.beforeDeleteHooks, &hook)
}

// AfterList adds a new hook to be executed after listing the objects in the datastore
func (h *Hook) AfterList(hook func(env *env, matchList *[]dao.Match) *HookError) {
	h.afterListHooks = append(h.afterListHooks, &hook)
}

// AfterCreate adds a new hook to be executed after creating an object in the datastore
func (h *Hook) AfterCreate(hook func(env *env, match *dao.Match) *HookError) {
	h.afterCreateHooks = append(h.afterCreateHooks, &hook)
}

// AfterRead adds a new hook to be executed after reading an object in the datastore
func (h *Hook) AfterRead(hook func(env *env, match *dao.Match) *HookError) {
	h.afterReadHooks = append(h.afterReadHooks, &hook)
}

// AfterUpdate adds a new hook to be executed after updating an object in the datastore
func (h *Hook) AfterUpdate(hook func(env *env, match *dao.Match) *HookError) {
	h.afterUpdateHooks = append(h.afterUpdateHooks, &hook)
}

// AfterDelete adds a new hook to be executed after deleting an object in the datastore
func (h *Hook) AfterDelete(hook func(env *env) *HookError) {
	h.afterDeleteHooks = append(h.afterDeleteHooks, &hook)
}
