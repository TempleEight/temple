package main

import (
	"github.com/TempleEight/spec-golang/match/dao"
	"github.com/TempleEight/spec-golang/match/util"
)

// Hook allows additional code to be executed before and after every datastore interaction
// Hooks are executed in the order they are defined, such that if any hook errors, future hooks are not executed and the request is terminated
type Hook struct {
	beforeListMatchHooks   []*func(env *env, input *dao.ListMatchInput, auth *util.Auth) *HookError
	beforeCreateMatchHooks []*func(env *env, req createMatchRequest, input *dao.CreateMatchInput, auth *util.Auth) *HookError
	beforeReadMatchHooks   []*func(env *env, input *dao.ReadMatchInput, auth *util.Auth) *HookError
	beforeUpdateMatchHooks []*func(env *env, req updateMatchRequest, input *dao.UpdateMatchInput, auth *util.Auth) *HookError
	beforeDeleteMatchHooks []*func(env *env, input *dao.DeleteMatchInput, auth *util.Auth) *HookError

	afterListMatchHooks   []*func(env *env, matchList *[]dao.Match, auth *util.Auth) *HookError
	afterCreateMatchHooks []*func(env *env, match *dao.Match, auth *util.Auth) *HookError
	afterReadMatchHooks   []*func(env *env, match *dao.Match, auth *util.Auth) *HookError
	afterUpdateMatchHooks []*func(env *env, match *dao.Match, auth *util.Auth) *HookError
	afterDeleteMatchHooks []*func(env *env, auth *util.Auth) *HookError
}

// HookError wraps an existing error with HTTP status code
type HookError struct {
	statusCode int
	error      error
}

func (e *HookError) Error() string {
	return e.error.Error()
}

// BeforeListMatch adds a new hook to be executed before listing the objects in the datastore
func (h *Hook) BeforeListMatch(hook func(env *env, input *dao.ListMatchInput, auth *util.Auth) *HookError) {
	h.beforeListMatchHooks = append(h.beforeListMatchHooks, &hook)
}

// BeforeCreateMatch adds a new hook to be executed before creating an object in the datastore
func (h *Hook) BeforeCreateMatch(hook func(env *env, req createMatchRequest, input *dao.CreateMatchInput, auth *util.Auth) *HookError) {
	h.beforeCreateMatchHooks = append(h.beforeCreateMatchHooks, &hook)
}

// BeforeReadMatch adds a new hook to be executed before reading an object in the datastore
func (h *Hook) BeforeReadMatch(hook func(env *env, input *dao.ReadMatchInput, auth *util.Auth) *HookError) {
	h.beforeReadMatchHooks = append(h.beforeReadMatchHooks, &hook)
}

// BeforeUpdateMatch adds a new hook to be executed before updating an object in the datastore
func (h *Hook) BeforeUpdateMatch(hook func(env *env, req updateMatchRequest, input *dao.UpdateMatchInput, auth *util.Auth) *HookError) {
	h.beforeUpdateMatchHooks = append(h.beforeUpdateMatchHooks, &hook)
}

// BeforeDeleteMatch adds a new hook to be executed before deleting an object in the datastore
func (h *Hook) BeforeDeleteMatch(hook func(env *env, input *dao.DeleteMatchInput, auth *util.Auth) *HookError) {
	h.beforeDeleteMatchHooks = append(h.beforeDeleteMatchHooks, &hook)
}

// AfterListMatch adds a new hook to be executed after listing the objects in the datastore
func (h *Hook) AfterListMatch(hook func(env *env, matchList *[]dao.Match, auth *util.Auth) *HookError) {
	h.afterListMatchHooks = append(h.afterListMatchHooks, &hook)
}

// AfterCreateMatch adds a new hook to be executed after creating an object in the datastore
func (h *Hook) AfterCreateMatch(hook func(env *env, match *dao.Match, auth *util.Auth) *HookError) {
	h.afterCreateMatchHooks = append(h.afterCreateMatchHooks, &hook)
}

// AfterReadMatch adds a new hook to be executed after reading an object in the datastore
func (h *Hook) AfterReadMatch(hook func(env *env, match *dao.Match, auth *util.Auth) *HookError) {
	h.afterReadMatchHooks = append(h.afterReadMatchHooks, &hook)
}

// AfterUpdateMatch adds a new hook to be executed after updating an object in the datastore
func (h *Hook) AfterUpdateMatch(hook func(env *env, match *dao.Match, auth *util.Auth) *HookError) {
	h.afterUpdateMatchHooks = append(h.afterUpdateMatchHooks, &hook)
}

// AfterDeleteMatch adds a new hook to be executed after deleting an object in the datastore
func (h *Hook) AfterDeleteMatch(hook func(env *env, auth *util.Auth) *HookError) {
	h.afterDeleteMatchHooks = append(h.afterDeleteMatchHooks, &hook)
}
