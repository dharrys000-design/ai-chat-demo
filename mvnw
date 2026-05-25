#!/bin/sh
# ---------------------------------------------------------------------------
# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements.  See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership.  The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License.  You may obtain a copy of the License at
#
#    http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing,
# software distributed under the License is distributed on an
# "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
# KIND, either express or implied.  See the License for the
# specific language governing permissions and limitations
# under the License.
# ---------------------------------------------------------------------------

# Maven start up script for cygwin, msys, and windows

if [ -z "$JAVA_HOME" ] && [ -r /etc/profile.d/java.sh ]; then
    . /etc/profile.d/java.sh
fi

if [ -z "$JAVA_HOME" ]; then
    try_result=$( type java 2>/dev/null )
    if [ -x "$try_result" ]; then
        export JAVA_HOME=$( cd "$( dirname "$try_result" )" && pwd )
    fi
fi

if [ ! -x "$JAVA_HOME/bin/java" ]; then
    echo "The JAVA_HOME environment variable is not defined correctly"
    echo "this environment variable is needed to run this program"
    exit 1
fi

CLASSWORLDS_JAR="$( cd "$( dirname "$0" )" && pwd )/.mvn/wrapper/maven-wrapper.jar"
MAVEN_PROJECTBASEDIR="${MAVEN_BASEDIR:-"$( cd "$( dirname "$0" )" && pwd -P )"}"

exec "$JAVA_HOME/bin/java" \
  $MAVEN_OPTS \
  -classpath "$CLASSWORLDS_JAR" \
  -Dclassworlds.conf="$MAVEN_PROJECTBASEDIR/.mvn/wrapper/m2e.properties" \
  -Dmaven.home="$MAVEN_PROJECTBASEDIR" \
  -Dmaven.multiModuleProjectDirectory="$MAVEN_PROJECTBASEDIR" \
  org.apache.maven.wrapper.MavenWrapperMain "$@"

