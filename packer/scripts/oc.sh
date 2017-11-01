#!/bin/bash

set -e
set -x

sudo dnf install -y python2 python-yaml

sudo python - <<END

import yaml

with open('/etc/containers/registries.conf', 'r') as f:
  config = yaml.load(f)

config['insecure_registries'] = ['172.30.0.0/16']

with file('/etc/containers/registries.conf', 'w') as f:
  yaml.dump(config, f)

END

sudo systemctl daemon-reload
sudo systemctl restart docker

DOCKER_BRIDGE=$(sudo docker network inspect -f "{{range .IPAM.Config }}{{ .Subnet }}{{end}}" bridge)

sudo firewall-cmd --permanent --new-zone dockerc
sudo firewall-cmd --permanent --zone dockerc --add-source ${DOCKER_BRIDGE}
sudo firewall-cmd --permanent --zone dockerc --add-port 8443/tcp
sudo firewall-cmd --permanent --zone dockerc --add-port 53/udp
sudo firewall-cmd --permanent --zone dockerc --add-port 8053/udp
sudo firewall-cmd --reload

OC_VERSION=3.6.1
OC_COMMIT=008f2d5
OC_DIR=openshift-origin-client-tools-v${OC_VERSION}-${OC_COMMIT}-linux-64bit
OC_TARBALL=${OC_DIR}.tar.gz
OC_URL=https://github.com/openshift/origin/releases/download/v${OC_VERSION}/${OC_TARBALL}

wget ${OC_URL}
tar zxf ${OC_TARBALL}
mkdir --parents ${HOME}/bin
mv ${OC_DIR}/oc ${HOME}/bin
rm -rf ${OC_DIR} ${OC_TARBALL}
