// Create an access token with a 24 hour lifetime
func createToken(id uuid.UUID, issuer string, secret string) (string, error) {
	token := jwt.NewWithClaims(jwt.SigningMethodHS256, jwt.MapClaims{
		"id":  id.String(),
		"iss": issuer,
		"exp": time.Now().Add(24 * time.Hour).Unix(),
	})

	return token.SignedString([]byte(secret))
}
