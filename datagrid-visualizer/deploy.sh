#!/usr/bin/env bash

set -e -x

APP=datagrid-visualizer

oc new-build --binary --name=${APP}
oc start-build ${APP} --from-dir=. --follow
oc new-app ${APP}
oc expose service ${APP}
