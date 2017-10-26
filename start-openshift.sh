#!/bin/bash

set -e -x

oc cluster down
oc cluster up --service-catalog

oc login -u system:admin
oc adm policy add-cluster-role-to-user cluster-admin developer

oc login -u developer -p developer
oc project openshift
oc adm policy add-cluster-role-to-group system:openshift:templateservicebroker-client system:unauthenticated system:authenticated
oc create -f openshift/infinispan-centos7-imagestream.json || true
oc create -f openshift/infinispan-ephemeral-template.json || true
