#!/usr/bin/env bash

set -e -x

if [[ $1 = "--solution" ]]; then
  MVN_PROFILE="-P solution"
  APP_LABEL=",solution=true"
fi

APP=simple-web-app

oc project myproject

oc new-build --binary --name=${APP} -l app=${APP}
mvn clean package -DskipTests=true
oc start-build ${APP} --from-dir=. --follow
oc new-app ${APP} -l app=${APP},hystrix.enabled=true,project=workshop
oc expose service ${APP}
