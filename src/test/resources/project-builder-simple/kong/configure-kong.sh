#!/bin/sh

curl -X POST \
  --url $KONG_ADMIN/services/ \
  --data 'name=temple-user-service' \
  --data 'url=http://temple-user:1026/temple-user'

curl -X POST \
  --url $KONG_ADMIN/services/temple-user-service/routes \
  --data "hosts[]=$KONG_ENTRY" \
  --data 'paths[]=/api/temple-user'
