#!/usr/bin/env sh

BASE_REPO_URL=jaylees/templeeight
SERVICE="shell2http"
SERVICE_VERSION="1.0"
REPO_URL=${BASE_REPO_URL}-${SERVICE}

docker build -t $SERVICE -f $SERVICE.Dockerfile .
docker tag $SERVICE:latest $REPO_URL:$SERVICE_VERSION
# Requires DOCKER_USERNAME and DOCKER_PASSWORD to be present
docker push $REPO_URL:$SERVICE_VERSION
