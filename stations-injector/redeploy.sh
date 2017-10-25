#!/usr/bin/env bash

set -e -x

if [[ $1 = "-am" ]]; then
  (cd ../data-model; ./install.sh)
fi

APP=stations-injector

mvn clean package -DskipTests=true
oc start-build ${APP} --from-dir=. --follow
