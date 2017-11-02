#!/bin/bash

set -e
set -x

sudo dnf install -y docker
sudo systemctl enable docker
sudo systemctl start docker

sudo groupadd docker && sudo gpasswd -a ${USER} docker && sudo systemctl restart docker

sudo shutdown -r now
