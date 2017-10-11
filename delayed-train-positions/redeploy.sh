#!/usr/bin/env bash

set -e -x

(cd ../datamodel; ./install.sh)

APP=delayed-train-positions

mvn clean package -DskipTests=true; oc start-build ${APP} --from-dir=. --follow
