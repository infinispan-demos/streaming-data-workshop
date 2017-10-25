#!/bin/bash

set -e -x

APP=datagrid
USR=developer
PASS=developer
NUM_NODES=3
NS=myproject

oc project ${NS}

oc process -n openshift infinispan-ephemeral -p \
  NUMBER_OF_INSTANCES=${NUM_NODES} \
  NAMESPACE=${NS} \
  APPLICATION_NAME=${APP} \
  MANAGEMENT_USER=${USR} \
  MANAGEMENT_PASSWORD=${PASS} | oc create -f -
