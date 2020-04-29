#!/bin/sh
REG_URL="${REG_URL:-localhost:5000}"
BASEDIR=$(dirname "$BASH_SOURCE")

for service in "complex-user" "auth"; do
  docker build -t "$REG_URL/sample-complex-project-$service" "$BASEDIR/$service"
  docker push "$REG_URL/sample-complex-project-$service"
done
