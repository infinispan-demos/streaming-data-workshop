#!/bin/bash

set -e
set -x

MVN_VERSION=3.5.0

cd ${HOME}
MIRROR=$(curl 'https://www.apache.org/dyn/closer.cgi' |   grep -o '<strong>[^<]*</strong>' |   sed 's/<[^>]*>//g' |   head -1)
wget ${MIRROR}/maven/maven-3/${MVN_VERSION}/binaries/apache-maven-${MVN_VERSION}-bin.tar.gz
tar zxf apache-maven-${MVN_VERSION}-bin.tar.gz
mv apache-maven-${MVN_VERSION} maven
echo 'export M2_HOME=${HOME}/maven' >> ${HOME}/.bash_profile
echo 'export PATH=${M2_HOME}/bin:${PATH}' >> ${HOME}/.bash_profile
rm apache-maven-${MVN_VERSION}-bin.tar.gz
