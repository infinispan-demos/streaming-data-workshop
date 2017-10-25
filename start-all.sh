#!/bin/bash

set -e -x

./start-openshift.sh
./start-datagrid.sh
./deploy-all.sh
