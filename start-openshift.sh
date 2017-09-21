#!/bin/bash

set -e -x

(cd $TMPDIR
rm -drf infinispan-openshift-templates
git clone https://github.com/infinispan/infinispan-openshift-templates
cd infinispan-openshift-templates
make stop-openshift
make start-openshift-with-catalog install-templates)

(cd ./datagrid; ./deploy.sh)
