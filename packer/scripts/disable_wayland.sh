#!/bin/bash

set -e
set -x

sudo sed -i -e 's/#WaylandEnable=false/WaylandEnable=false/' /etc/gdm/custom.conf
