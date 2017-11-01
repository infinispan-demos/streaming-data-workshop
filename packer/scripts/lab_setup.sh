#!/bin/bash

set -e
set -x

WORKSHOP=streaming-data-workshop
WORKSHOP_DIR=${HOME}/${WORKSHOP}

git clone https://github.com/infinispan-demos/${WORKSHOP} ${WORKSHOP_DIR}

cd ${WORKSHOP_DIR}
mvn install dependency:go-offline

cd ${WORKSHOP_DIR}/web-viewer
nvm use 4.2
npm install
npm run build

cd ${WORKSHOP_DIR}
./start-openshift.sh
./start-datagrid.sh

(cd ./data-model; ./install.sh)
(cd ./workshop-main; ./deploy.sh)
(cd ./stations-injector; ./deploy.sh)
(cd ./positions-injector; ./deploy.sh)
(cd ./delayed-listener; ./deploy.sh)
(cd ./delayed-trains; ./deploy.sh)
(cd ./datagrid-visualizer; ./deploy.sh)

# Expecting 6 app pods plus 3 ISPN pods
EXPECTED_PODS=9
START=$(date -u +%s)
until [ "$(oc get pods -a=false -o=name | wc -l)" -eq "${EXPECTED_PODS}" ]; do
    printf '.'
    sleep 5
    if [ "$(bc <<< "$(date -u +%s)-${START}")" -gt "15" ]; then
        # Timeout
        exit 128
    fi
done

oc cluster down
