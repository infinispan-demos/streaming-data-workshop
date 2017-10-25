#!/usr/bin/env bash

set -e -x

(cd ../data-model; ./install.sh)

APP=stations-injector

mvn clean package -DskipTests=true
oc start-build ${APP} --from-dir=. --follow
