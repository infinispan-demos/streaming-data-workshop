#!/bin/bash

set -e -x

while :
do
   curl http://test-datagrid-openshift.127.0.0.1.nip.io/api/greeting
   sleep 2
done
