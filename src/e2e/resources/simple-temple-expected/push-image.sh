#!/bin/sh
REG_URL="${REG_URL:-localhost:5000}"
BASEDIR=$(dirname "$BASH_SOURCE")

for service in "simple-temple-test-user" "booking" "simple-temple-test-group" "auth"; do
  docker build -t "$REG_URL/simple-temple-test-$service" "$BASEDIR/$service"
  docker push "$REG_URL/simple-temple-test-$service"
done
