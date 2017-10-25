#!/usr/bin/env bash

set -e -x

(cd ../data-model; ./install.sh)

APP=workshop-main

mvn clean package -DskipTests=true
oc start-build ${APP} --from-dir=. --follow
