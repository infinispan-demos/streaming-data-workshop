#!/bin/bash

set -e -x

while :
do
   curl http://delayed-train-positions-myproject.127.0.0.1.nip.io/position
   sleep 2
done
