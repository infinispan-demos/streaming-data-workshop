#!/usr/bin/env bash

set -e -x

APP=stations-injector

mvn compile

oc start-build ${APP} --from-dir=. --follow
