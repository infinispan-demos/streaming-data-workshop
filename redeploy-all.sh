#!/bin/bash

set -e -x

(cd ./data-model; ./install.sh)
(cd ./workshop-main; ./redeploy.sh)
(cd ./stations-injector; ./redeploy.sh)
