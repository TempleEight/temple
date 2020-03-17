func createKongConsumer(hostname string) (*consumerResponse, error) {
	postData := url.Values{}
	postData.Set("username", "auth-service")

	res, err := http.PostForm(fmt.Sprintf("%s/consumers", hostname), postData)
	if err != nil {
		return nil, err
	}
	defer res.Body.Close()

	if res.StatusCode != http.StatusCreated {
		// If we have an error code, the message _should_ be in the body
		bodyBytes, err := ioutil.ReadAll(res.Body)
		if err != nil {
			return nil, errors.New("unable to create JWT consumer")
		}
		return nil, errors.New(string(bodyBytes))
	}

	consumer := consumerResponse{}
	err = json.NewDecoder(res.Body).Decode(&consumer)
	if err != nil {
		return nil, err
	}

	return &consumer, nil
}

func requestCredential(hostname string, consumer *consumerResponse) (*JWTCredential, error) {
	reqUrl := fmt.Sprintf("%s/consumers/%s/jwt", hostname, consumer.Username)
	res, err := http.Post(reqUrl, "application/x-www-form-urlencoded", nil)
	if err != nil {
		return nil, err
	}
	defer res.Body.Close()

	if res.StatusCode != http.StatusCreated {
		// If we have an error code, the message _should_ be in the body
		bodyBytes, err := ioutil.ReadAll(res.Body)
		if err != nil {
			return nil, errors.New("unable to create JWT token")
		}
		return nil, errors.New(string(bodyBytes))
	}

	jwt := JWTCredential{}
	err = json.NewDecoder(res.Body).Decode(&jwt)
	if err != nil {
		return nil, err
	}

	return &jwt, nil
}

// CreateJWTCredential provisions a new HS256 JWT credential
func (comm *Handler) CreateJWTCredential() (*JWTCredential, error) {
	hostname, ok := comm.Services["kong-admin"]
	if !ok {
		return nil, errors.New("service kong-admin's hostname not in config file")
	}

	// Create a consumer
	consumer, err := createKongConsumer(hostname)
	if err != nil {
		return nil, err
	}

	// Use the consumer to request a credential
	return requestCredential(hostname, consumer)
}
