#!/bin/bash

set -e -x

(cd ./data-model; ./install.sh)
(cd ./workshop-main; ./redeploy.sh)
(cd ./stations-injector; ./redeploy.sh)
(cd ./positions-injector; ./redeploy.sh)
(cd ./delayed-listener; ./redeploy.sh)
(cd ./delayed-trains; ./redeploy.sh)
