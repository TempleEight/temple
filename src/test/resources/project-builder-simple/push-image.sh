#!/bin/sh
REGISTRY_URL="localhost:5000"

for service in "temple-user"; do
  docker build -t "$REGISTRY_URL/sample-project-$service" $service
  docker push "$REGISTRY_URL/sample-project-$service"
done
