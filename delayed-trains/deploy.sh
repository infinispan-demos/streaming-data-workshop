#!/usr/bin/env bash

set -e -x

APP=delayed-trains

mvn compile

oc start-build ${APP} --from-dir=. --follow
