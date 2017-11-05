#!/bin/bash

set -e -x

mvn clean install -N
(cd ./data-model; ./install.sh)
(cd ./workshop-main; ./deploy.sh --solution)
(cd ./stations-injector; ./deploy.sh --solution)
(cd ./positions-injector; ./deploy.sh)
(cd ./delayed-listener; ./deploy.sh  --solution)
(cd ./delayed-trains; ./deploy.sh  --solution)
(cd ./datagrid-visualizer; ./deploy.sh)
(cd ./simple-web-application; ./deploy.sh --solution)

(cd ./web-viewer; ./start.sh)
