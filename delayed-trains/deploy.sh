#!/usr/bin/env bash

set -e -x

APP=delayed-trains

oc project myproject

oc new-build --binary --name=${APP} -l app=${APP}
mvn clean package -DskipTests=true
oc start-build ${APP} --from-dir=. --follow
oc new-app ${APP} -l app=${APP},hystrix.enabled=true,project=workshop
oc expose service ${APP}
