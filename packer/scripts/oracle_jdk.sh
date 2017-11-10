#!/bin/bash

echo "#################################################################"
echo "Installing Oracle JDK"
echo "By using this script you agree to the Oracle licensing agreement."
echo "#################################################################"

set -e
set -x

RPM_FILE=jdk-8u152-linux-x64.rpm
SHA_256=b95c69b10e41d0f91e1ae6ef51086025535a43235858326a5a8fd9c5693ecc28

cd ${HOME}
wget --no-cookies --header "Cookie: oraclelicense=accept-securebackup-cookie" http://download.oracle.com/otn-pub/java/jdk/8u152-b16/aa0333dd3019491ca4f6ddbe78cdb6d0/${RPM_FILE}
sha256sum -c <<< "${SHA_256} ${RPM_FILE}"
sudo rpm -ivh ${RPM_FILE}
rm ${RPM_FILE}
