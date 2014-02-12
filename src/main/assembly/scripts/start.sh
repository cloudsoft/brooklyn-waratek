#!/bin/bash
# Copyright 2014 by Cloudsoft Corp.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
# http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

#set -x # DEBUG

if [ ! -z "${JAVA_HOME}" ] ; then 
    JAVA=${JAVA_HOME}/bin/java
else
    JAVA=$(which java)
fi

if [ ! -x "${JAVA}" ] ; then
    echo "Cannot find java. Set JAVA_HOME or add java to path."
    exit 1
fi

if [ -z "$(ls brooklyn-waratek*.jar 2> /dev/null)" ] ; then
    echo "Command must be run from the directory where it is installed."
    exit 1
fi

$JAVA -Xms256m -Xmx1024m -XX:MaxPermSize=1024m -classpath "conf/:patch/*:*:lib/*" com.waratek.cloudvm.Main "$@"
