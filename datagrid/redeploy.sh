#!/usr/bin/env bash

set -e -x

TEST_APP=test-datagrid

mvn clean package -DskipTests=true
oc start-build ${TEST_APP} --from-dir=. --follow
