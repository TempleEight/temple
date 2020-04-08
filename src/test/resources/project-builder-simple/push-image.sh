#!/bin/sh
REGISTRY_URL="localhost:5000"
BASEDIR=$(dirname "$BASH_SOURCE")

for service in "temple-user"; do
  docker build -t "$REGISTRY_URL/sample-project-$service" $BASEDIR/$service
  docker push "$REGISTRY_URL/sample-project-$service"
done
