#!/usr/bin/env bash
set -e -x

oc project myproject

APP=positions-injector

# || true to make it idempotent
oc new-build --binary --name=${APP} -l app=${APP} || true

mvn clean dependency:copy-dependencies compile -DincludeScope=runtime

oc start-build ${APP} --from-dir=. --follow

# || true to make it idempotent
oc new-app ${APP} -l app=${APP} || true
# || true to make it idempotent
oc expose service ${APP} || true
