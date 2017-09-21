#!/usr/bin/env bash

set -e -x

(cd ..; mvn clean install -N)

mvn clean install -DskipTests=true
