#!/usr/bin/env bash

set -e -x

APP=workshop-main

mvn compile

oc start-build ${APP} --from-dir=. --follow
