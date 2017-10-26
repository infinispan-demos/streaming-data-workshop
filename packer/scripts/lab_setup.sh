#!/bin/bash

set -e
set -x

export M2_HOME=${HOME}/maven
export PATH=${M2_HOME}/bin:${PATH}

echo "#################################################################"
echo "Checking Maven install"
echo
mvn -version
echo
echo "Done"
echo "#################################################################"

cd ${HOME}
git clone https://github.com/infinispan-demos/streaming-data-workshop
cd streaming-data-workshop
mvn install dependency:go-offline
