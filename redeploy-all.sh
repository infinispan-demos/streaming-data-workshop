#!/bin/bash

set -e -x

(cd ./data-model; mvn install)
(cd ./workshop-main; mvn fabric8:deploy)
(cd ./stations-injector; mvn fabric8:deploy)
(cd ./positions-injector; mvn fabric8:deploy)
(cd ./stations-transport; mvn fabric8:deploy)
(cd ./positions-transport; mvn fabric8:deploy)
(cd ./delayed-listener; mvn fabric8:deploy)
(cd ./delayed-trains; mvn fabric8:deploy)
(cd ./simple-web-application; mvn fabric8:deploy)
