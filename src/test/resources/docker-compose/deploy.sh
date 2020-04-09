#!/bin/sh
docker ps -a -q  | xargs docker rm -f
docker volume prune -f
docker-compose up --build -d
until curl -s localhost:8001 2>&1 1>/dev/null; do
  >&2 echo "Kong is unavailable - sleeping"
  sleep 2
done
export KONG_ENTRY="localhost:8000"
export KONG_ADMIN="localhost:8001"
sh kong/configure-kong-k8s.sh
