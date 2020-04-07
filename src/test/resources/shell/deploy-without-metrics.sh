#! /bin/sh
# Deployment script for kube - generates configmaps from SQL init files and provisions everything

BASEDIR=$(dirname "$BASH_SOURCE")

GREEN="\033[1;32m"
BLUE="\033[1;34m"
YELLOW="\033[1;33m"
PURPLE="\033[1;34m"
NOCOLOR="\033[0m"

minikube start --vm-driver=virtualbox

echo $GREEN

kubectl create secret docker-registry regcred --docker-server=$REG_URL --docker-username=$REG_USERNAME --docker-password=$REG_PASSWORD --docker-email=$REG_EMAIL

echo
echo Creating ConfigMaps...
echo

# DB init scripts
kubectl create configmap user-db-config --from-file "$BASEDIR/user-db/init.sql" -o=yaml

for file in "$BASEDIR/grafana/provisioning/dashboards/"*
do
  filename=$(basename $file) # Get everything after the final /
  file=$(echo $filename | cut -f 1 -d '.') # Get everything before the . (filename without extension)
  kubectl create configmap "grafana-$file-config" --from-file "$BASEDIR/grafana/provisioning/dashboards/$filename" -o=yaml
done

for dir in "$BASEDIR/kube/"*
do
  kubectl create -f $dir
done

echo $NOCOLOR

# Kube takes a few seconds to create the objects
sleep 5

echo Sleeping until pods have started...
# Until all of the pods have status of either "Running" or "Completed"
until ! kubectl get pod | awk '{ if (NR != 1) printf $3 "\n" }' | grep -s -q -E "Running|Completed" --invert
do
  sleep 1
done

# Get the URLs Minikube has assigned to the endpoints
urls=$(minikube service kong --url | head -n 2)

export KONG_ENTRY=$(echo $urls | head -n 1 | cut -d '/' -f 3-)
export KONG_ADMIN=$(echo $urls | tail -n 1)

echo $BLUE

echo KONG_ENTRY: $KONG_ENTRY
echo KONG_ADMIN: $KONG_ADMIN

echo $YELLOW

echo Configuring Kong...

sleep 1

sh "$BASEDIR/kong/configure-kong-k8s.sh"

echo $PURPLE

echo
echo Done!

echo $NOCOLOR
