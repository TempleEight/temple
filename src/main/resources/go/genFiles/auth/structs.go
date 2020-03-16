// env defines the environment that requests should be executed within
type env struct {
	dao           dao.Datastore
	comm          comm.Comm
	jwtCredential *comm.JWTCredential
}

// registerAuthRequest contains the client-provided information required to create a single auth
type registerAuthRequest struct {
	Email    string `valid:"email,required"`
	Password string `valid:"type(string),required,stringlength(8|64)"`
}

// loginAuthRequest contains the client-provided information required to login an existing auth
type loginAuthRequest struct {
	Email    string `valid:"email,required"`
	Password string `valid:"type(string),required,stringlength(8|64)"`
}

// registerAuthResponse contains an access token
type registerAuthResponse struct {
	AccessToken string
}

// loginAuthResponse contains an access token
type loginAuthResponse struct {
	AccessToken string
}
