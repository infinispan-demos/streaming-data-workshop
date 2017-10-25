#!/bin/bash

set -e -x

#(cd $TMPDIR
#rm -drf infinispan-openshift-templates
#git clone https://github.com/infinispan/infinispan-openshift-templates
#cd infinispan-openshift-templates
#git checkout streaming_data_workshop
#make stop-openshift
#make start-openshift-with-catalog install-templates)

oc cluster down
oc cluster up --service-catalog

oc login -u system:admin
oc adm policy add-cluster-role-to-user cluster-admin developer

oc login -u developer -p developer
oc project openshift
oc adm policy add-cluster-role-to-group system:openshift:templateservicebroker-client system:unauthenticated system:authenticated
oc create -f openshift/infinispan-centos7-imagestream.json || true
oc create -f openshift/infinispan-ephemeral-template.json || true


## TODO: datagrid project not necessary, used to speed up but assistants would do it manually via UI
#(cd ./datagrid; ./deploy.sh)


#(cd ./delayed-train-positions; ./deploy.sh)
