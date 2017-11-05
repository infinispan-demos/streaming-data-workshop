#!/bin/bash

set -e
set -x

TARBALL=ideaIC-2017.2.5-no-jdk.tar.gz

cd ${HOME}
wget https://download.jetbrains.com/idea/${TARBALL}
tar zxf ${TARBALL}
mv $(ls | grep idea-IC) idea
rm ${TARBALL}

mkdir --parents ${HOME}/.local/share/applications
cat <<EOF > ${HOME}/.local/share/applications/jetbrains-idea-ce.desktop
[Desktop Entry]
Version=1.0
Type=Application
Name=IntelliJ IDEA Community Edition
Icon=${HOME}/idea/bin/idea.png
Exec="${HOME}/idea/bin/idea.sh" %f
Comment=The Drive to Develop
Categories=Development;IDE;
Terminal=false
StartupWMClass=jetbrains-idea-ce
EOF

mkdir --parents ${HOME}/.IdeaIC2017.2/config
cp ${HOME}/idea/bin/idea64.vmoptions ${HOME}/.IdeaIC2017.2/config/idea64.vmoptions
echo "-Dsun.java2d.uiScale.enabled=false" >> ${HOME}/.IdeaIC2017.2/config/idea64.vmoptions
