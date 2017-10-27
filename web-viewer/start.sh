#!/bin/bash

set -e -x

# TODO Should be deployed to OpenShift and run there

source ~/.nvm/nvm.sh
nvm use 4.2
npm install
npm start
