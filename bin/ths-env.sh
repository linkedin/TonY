# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements.  See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership.  The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License.  You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

unset CLASSPATH
unset HADOOP_CLASSPATH

# When running a distributed configuration it is best to
# set JAVA_HOME in this file, so that it is correctly defined on
# remote nodes.
# The java implementation to use.
# export JAVA_HOME=

# Set Hadoop-specific environment variables here.
# export HADOOP_CONF_DIR=


# No need to set THS_HOME here since that 
# was done in start-history-server.sh
export THS_CONF_DIR=$THS_HOME/conf/
export THS_CLASSPATH="$THS_CONF_DIR:$HADOOP_CONF_DIR"

for f in $THS_HOME/tony-history-server/out/libs/*.jar; do
    export THS_CLASSPATH=$THS_CLASSPATH:$f
done

export THS_CLIENT_OPTS="-Xmx1024m"
