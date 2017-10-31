#!/usr/bin/env bash

set -e -x

if [[ $1 = "-am" ]]; then
  (cd ../data-model; ./install.sh)
fi

if [[ $1 = "--solution" ]]; then
  MVN_PROFILE="-P solution"
fi

APP=simple-web-app

mvn clean package -DskipTests=true ${MVN_PROFILE}
oc start-build ${APP} --from-dir=. --follow
