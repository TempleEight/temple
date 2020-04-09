#!/bin/sh
# https://docs.docker.com/compose/startup-order/

set -e

host="$1"
shift
cmd="$@"

until curl -s $host 2>&1 1>/dev/null; do
  >&2 echo "Kong is unavailable - sleeping"
  sleep 1
done

>&2 echo "Kong is up - starting service"
exec $cmd