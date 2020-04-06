#!/bin/sh

curl -X POST \
  --url $KONG_ADMIN/services/ \
  --data 'name=simple-temple-test-user-service' \
  --data 'url=http://simple-temple-test-user:1026/simple-temple-test-user'

curl -X POST \
  --url $KONG_ADMIN/services/ \
  --data 'name=booking-service' \
  --data 'url=http://booking:1028/booking'

curl -X POST \
  --url $KONG_ADMIN/services/ \
  --data 'name=simple-temple-test-group-service' \
  --data 'url=http://simple-temple-test-group:1030/simple-temple-test-group'

curl -X POST \
  --url $KONG_ADMIN/services/ \
  --data 'name=auth-service' \
  --data 'url=http://auth:1024/auth'

curl -X POST \
  --url $KONG_ADMIN/services/simple-temple-test-user-service/routes \
  --data "hosts[]=$KONG_ENTRY" \
  --data 'paths[]=/api/simple-temple-test-user'

curl -X POST \
  --url $KONG_ADMIN/services/booking-service/routes \
  --data "hosts[]=$KONG_ENTRY" \
  --data 'paths[]=/api/booking'

curl -X POST \
  --url $KONG_ADMIN/services/simple-temple-test-group-service/routes \
  --data "hosts[]=$KONG_ENTRY" \
  --data 'paths[]=/api/simple-temple-test-group'

curl -X POST \
  --url $KONG_ADMIN/services/auth-service/routes \
  --data "hosts[]=$KONG_ENTRY" \
  --data 'paths[]=/api/auth'

curl -X POST \
  --url $KONG_ADMIN/services/simple-temple-test-user-service/plugins \
  --data 'name=jwt' \
  --data 'config.claims_to_verify=exp'

curl -X POST \
  --url $KONG_ADMIN/services/booking-service/plugins \
  --data 'name=jwt' \
  --data 'config.claims_to_verify=exp'

curl -X POST \
  --url $KONG_ADMIN/services/simple-temple-test-group-service/plugins \
  --data 'name=jwt' \
  --data 'config.claims_to_verify=exp'