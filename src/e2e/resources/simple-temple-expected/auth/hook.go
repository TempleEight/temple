package main

import "github.com/squat/and/dab/auth/dao"

// Hook allows additional code to be executed before and after every datastore interaction
// Hooks are executed in the order they are defined, such that if any hook errors, future hooks are not executed and the request is terminated
type Hook struct {
	beforeRegisterHooks []*func(env *env, req registerAuthRequest, input *dao.CreateAuthInput) *HookError
	beforeLoginHooks    []*func(env *env, req loginAuthRequest, input *dao.ReadAuthInput) *HookError

	afterRegisterHooks []*func(env *env, auth *dao.Auth, accessToken string) *HookError
	afterLoginHooks    []*func(env *env, auth *dao.Auth, accessToken string) *HookError
}

// HookError wraps an existing error with HTTP status code
type HookError struct {
	statusCode int
	error      error
}

func (e *HookError) Error() string {
	return e.error.Error()
}

// BeforeRegister adds a new hook to be executed before creating an object in the datastore
func (h *Hook) BeforeRegister(hook func(env *env, req registerAuthRequest, input *dao.CreateAuthInput) *HookError) {
	h.beforeRegisterHooks = append(h.beforeRegisterHooks, &hook)
}

// BeforeLogin adds a new hook to be executed before reading an object in the datastore
func (h *Hook) BeforeLogin(hook func(env *env, req loginAuthRequest, input *dao.ReadAuthInput) *HookError) {
	h.beforeLoginHooks = append(h.beforeLoginHooks, &hook)
}

// AfterRegister adds a new hook to be executed after creating an object in the datastore
func (h *Hook) AfterRegister(hook func(env *env, auth *dao.Auth, accessToken string) *HookError) {
	h.afterRegisterHooks = append(h.afterRegisterHooks, &hook)
}

// AfterLogin adds a new hook to be executed after reading an object in the datastore
func (h *Hook) AfterLogin(hook func(env *env, auth *dao.Auth, accessToken string) *HookError) {
	h.afterLoginHooks = append(h.afterLoginHooks, &hook)
}
