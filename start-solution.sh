#!/bin/bash

set -e -x

./start-openshift.sh
./start-datagrid.sh
./start-kafka.sh
./deploy-solution.sh
