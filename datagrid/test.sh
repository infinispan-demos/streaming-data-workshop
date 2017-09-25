#!/bin/bash

set -e -x

while :
do
   curl http://test-datagrid-myproject.127.0.0.1.nip.io/api/greeting
   sleep 2
done
