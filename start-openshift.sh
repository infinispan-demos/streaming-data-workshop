#!/bin/bash

set -e -x

(cd $TMPDIR
rm -drf infinispan-openshift-templates
git clone https://github.com/infinispan/infinispan-openshift-templates
cd infinispan-openshift-templates
git checkout streaming_data_workshop
make stop-openshift
make start-openshift-with-catalog install-templates)

# TODO: datagrid project not necessary, used to speed up but assistants would do it manually via UI
(cd ./datagrid; ./deploy.sh)

mvn clean install -N
(cd ./data-model; ./install.sh)
(cd ./workshop-main; ./deploy.sh)
(cd ./stations-injector; ./deploy.sh)

#(cd ./delayed-train-positions; ./deploy.sh)
