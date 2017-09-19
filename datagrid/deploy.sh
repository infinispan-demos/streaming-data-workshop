#!/bin/bash

set -e -x

APP=datagrid
USR=developer
PWD=developer

oc process infinispan-ephemeral -p APPLICATION_NAME=${APP} APPLICATION_USER=${USR} APPLICATION_PASSWORD=${PWD} | oc create -f -


TEST_APP=test-datagrid

oc new-build --binary --name=${TEST_APP} -l app=${TEST_APP}
mvn clean package -DskipTests=true
oc start-build ${TEST_APP} --from-dir=. --follow
oc new-app ${TEST_APP} -l app=${TEST_APP},hystrix.enabled=true
oc expose service ${TEST_APP}
