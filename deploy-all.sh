#!/bin/bash

set -e -x

mvn clean install -N
(cd ./data-model; mvn install)
(cd ./workshop-main; ./first-deploy.sh)
(cd ./stations-injector; ./first-deploy.sh)
(cd ./positions-injector; ./first-deploy.sh)
(cd ./delayed-listener; ./first-deploy.sh)
(cd ./delayed-trains; ./first-deploy.sh)
(cd ./datagrid-visualizer; ./deploy.sh)
#(cd ./simple-web-application; mvn fabric8:deploy)
