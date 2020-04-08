#!/bin/sh
REGISTRY_URL="localhost:5000"
BASEDIR=$(dirname "$BASH_SOURCE")

for service in "simple-temple-test-user" "booking" "simple-temple-test-group" "auth"; do
  docker build -t "$REGISTRY_URL/simple-temple-test-$service" $BASEDIR/$service
  docker push "$REGISTRY_URL/simple-temple-test-$service"
done
