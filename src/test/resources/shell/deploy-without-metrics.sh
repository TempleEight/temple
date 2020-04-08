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

# Create private registry
kubectl create -f kube/deploy
sleep 5

# Forward ports from minikube to localhost
REGISTRY_NAME=$(kubectl get pod -n kube-system | grep kube-registry-v0 | awk '{ print $1 ;}')
echo $REGISTRY_NAME

# Until all of the pods have status of either "Running" or "Completed"
until ! kubectl get pod -n kube-system | awk '{ if (NR != 1) printf $3 "\n" }' | grep -s -q -E "Running|Completed" --invert
do
  sleep 1
done

kubectl port-forward --namespace kube-system $REGISTRY_NAME 5000:5000 2>&1 1>/dev/null &
sleep 5

# Push images to private repo
sh push-image.sh

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

sh "$BASEDIR/kong/configure-kong.sh"

echo $PURPLE

echo
echo Done!

echo $NOCOLOR
