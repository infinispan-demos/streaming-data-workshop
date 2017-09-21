#!/usr/bin/env bash

set -e -x

(cd ../datamodel; ./install.sh)

APP=station-boards-injector

mvn clean package -DskipTests=true; oc start-build ${APP} --from-dir=. --follow
