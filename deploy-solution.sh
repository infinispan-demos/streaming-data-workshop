#!/bin/bash

set -e -x

mvn clean install -N
(cd ./data-model; mvn install)
(cd ./workshop-main; mvn fabric8:deploy)
(cd ./stations-injector; mvn fabric8:deploy -Psolution)
(cd ./positions-injector; mvn fabric8:deploy)
(cd ./delayed-listener; mvn fabric8:deploy -Psolution)
(cd ./delayed-trains; mvn fabric8:deploy -Psolution)
(cd ./datagrid-visualizer; ./deploy.sh)
#(cd ./simple-web-application; mvn fabric8:deploy -Psolution)
