#!/usr/bin/env bash

set -e -x

APP=delayed-listener

mvn compile

oc start-build ${APP} --from-dir=. --follow
