#!/bin/bash

set -e -x

oc cluster down
oc cluster up

oc login -u developer -p developer https://127.0.0.1:8443
oc project myproject

oc create -f openshift/infinispan-centos7-imagestream.json || true
oc create -f openshift/infinispan-ephemeral-template.json || true
