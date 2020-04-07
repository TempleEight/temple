#!/bin/sh
REGISTRY_URL="localhost:5000"

for service in "complex-user" "auth"; do
  docker build -t "$REGISTRY_URL/temple-$service-service" $service
  docker push "$REGISTRY_URL/temple-$service-service"
done
