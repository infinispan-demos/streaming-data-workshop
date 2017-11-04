#!/bin/bash

set -e
set -x

TARBALL=eclipse-java-oxygen-1a-linux-gtk-x86_64.tar.gz

cd ${HOME}
wget  -O ${TARBALL} "https://www.eclipse.org/downloads/download.php?file=/technology/epp/downloads/release/oxygen/1a/${TARBALL}&r=1"
tar zxf ${TARBALL}
rm ${TARBALL}

mkdir --parents ${HOME}/.local/share/applications
cat <<EOF > ${HOME}/.local/share/applications/eclipse-ide-java.desktop
[Desktop Entry]
Version=1.0
Type=Application
Name=Eclipse IDE for Java Developers
Icon=${HOME}/eclipse/icon.xpm
Exec=${HOME}/eclipse/eclipse
Comment=The essential tools for any Java developer, including a Java IDE, a Git client, XML Editor, Mylyn, Maven and Gradle integration...
Categories=Development;IDE;
Terminal=false
EOF
