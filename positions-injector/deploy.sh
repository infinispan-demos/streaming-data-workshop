#!/usr/bin/env bash

set -e -x

APP=positions-injector

mvn compile

oc start-build ${APP} --from-dir=. --follow
