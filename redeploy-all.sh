#!/bin/bash

set -e -x


(cd ./workshop-main; ./redeploy.sh)
(cd ./stations-injector; ./redeploy.sh)
