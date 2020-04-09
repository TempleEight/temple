#!/bin/sh

curl -s -X POST \
  --url $KONG_ADMIN/services/ \
  --data 'name=complex-user-service' \
  --data 'url=http://complex-user:1026/complex-user'

curl -s -X POST \
  --url $KONG_ADMIN/services/ \
  --data 'name=auth-service' \
  --data 'url=http://auth:1024/auth'

curl -s -X POST \
  --url $KONG_ADMIN/services/complex-user-service/routes \
  --data "hosts[]=$KONG_ENTRY" \
  --data 'paths[]=/api/complex-user'

curl -s -X POST \
  --url $KONG_ADMIN/services/auth-service/routes \
  --data "hosts[]=$KONG_ENTRY" \
  --data 'paths[]=/api/auth'

curl -s -X POST \
  --url $KONG_ADMIN/services/complex-user-service/plugins \
  --data 'name=jwt' \
  --data 'config.claims_to_verify=exp'
