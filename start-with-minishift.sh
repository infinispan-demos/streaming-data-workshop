#!/usr/bin/env bash
set -e -x
MINISHIFT_ENABLE_EXPERIMENTAL=on minishift start --iso-url centos \
            --openshift-version v3.7.0 \
            --v 5 --show-libmachine-logs \
            --memory 4Gb \
            --extra-clusterup-flags="--service-catalog" \
            --insecure-registry 172.30.0.0/16 --insecure-registry minishift --insecure-registry 192.168.0.24:53170

oc login -u system:admin
oc adm policy add-cluster-role-to-user cluster-admin developer

oc login -u developer -p developer
oc project openshift
oc adm policy add-cluster-role-to-group system:openshift:templateservicebroker-client system:unauthenticated system:authenticated
oc create -f openshift/infinispan-centos7-imagestream.json || true
oc create -f openshift/infinispan-ephemeral-template.json || true

oc project myproject
