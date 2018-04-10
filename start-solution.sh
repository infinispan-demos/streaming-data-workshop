#!/bin/bash

set -e -x

./start-openshift.sh
setup-datagrid.sh
./deploy-solution.sh
