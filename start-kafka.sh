#!/usr/bin/env bash
set -e -x
oc create -f openshift/kafka-template.yaml
oc new-app barnabas

