#!/bin/sh
REGISTRY_URL="localhost:5000"

for service in "complex-user" "auth"; do
  docker build -t "$REGISTRY_URL/sample-complex-project-$service" $service
  docker push "$REGISTRY_URL/sample-complex-project-$service"
done
