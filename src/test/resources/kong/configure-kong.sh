#!/bin/sh

curl -s -X POST \
  --url $KONG_ADMIN/services/ \
  --data 'name=user-service' \
  --data 'url=http://user:80/user'

curl -s -X POST \
  --url $KONG_ADMIN/services/user-service/routes \
  --data "hosts[]=$KONG_ENTRY" \
  --data 'paths[]=/api/user'

curl -s -X POST \
  --url $KONG_ADMIN/services/user-service/plugins \
  --data 'name=jwt' \
  --data 'config.claims_to_verify=exp'
