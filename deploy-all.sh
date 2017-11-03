#!/bin/bash

set -e -x

mvn clean install -N
(cd ./data-model; ./install.sh)
(cd ./workshop-main; ./deploy.sh)
(cd ./stations-injector; ./deploy.sh)
(cd ./positions-injector; ./deploy.sh)
(cd ./delayed-listener; ./deploy.sh)
(cd ./delayed-trains; ./deploy.sh)
(cd ./datagrid-visualizer; ./deploy.sh)
(cd ./simple-web-application; ./deploy.sh)

(cd ./web-viewer; ./start.sh)
