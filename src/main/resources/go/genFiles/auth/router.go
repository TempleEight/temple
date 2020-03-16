// router generates a router for this service
func (env *env) router() *mux.Router {
	r := mux.NewRouter()
	r.HandleFunc("/auth/register", env.registerAuthHandler).Methods(http.MethodPost)
	r.HandleFunc("/auth/login", env.loginAuthHandler).Methods(http.MethodPost)
	r.Use(jsonMiddleware)
	return r
}
