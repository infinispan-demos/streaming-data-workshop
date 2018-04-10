#!/bin/bash

set -e -x

OPENSHIFT_NAME=$1
OPENSHIFT_MASTER=$2
OPENSHIFT_URL=https://console.${OPENSHIFT_NAME}.${OPENSHIFT_MASTER}.nip.io:8443

oc login -u developer -p developer ${OPENSHIFT_URL}
oc create -f openshift/infinispan-centos7-imagestream.json || true
oc create -f openshift/infinispan-ephemeral-template.json || true
