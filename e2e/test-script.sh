#!/bin/bash

set -eux

# PWD: [repository root]
# goof: ./java-goof

TEMP_DIR="$(mktemp -d)"
CONF_TEMPLATE="e2e/snyk-goof-e2e.properties"
AGENT_JAR="$(pwd)/build/libs/snyk-java-runtime-agent.jar"

<${CONF_TEMPLATE} sed 's,OUTPUT_PATH,'${TEMP_DIR}, > ${TEMP_DIR}/temp.properties

# clean-up previous (potentially failed) runs
rm -f java-goof/snyk-agent-*.log

# do some maven downloading outside of the waiting period
(cd java-goof && mvn -q tomcat7:help)

# start the app
(
  cd java-goof &&
  MAVEN_OPTS="-javaagent:${AGENT_JAR}=file://${TEMP_DIR}/temp.properties" \
    mvn tomcat7:run
) &

TOMCAT_PID=$!

trap "kill ${TOMCAT_PID} && rm -r '${TEMP_DIR}'" EXIT

# wait for the app to start
for i in {1..30}; do
    sleep 1

    echo 'Checking for startup, attempt '${i}'...'
    # we don't really care if this fails for another reason, we'll try it again later
    curl -s http://localhost:8080 && break
done

# exploit the app
<java-goof/exploits/struts-exploit-headers.txt sed "s/COMMAND/env/" | xargs curl -v -X GET http://localhost:8080 -H

# wait for the next report from the agent
sleep 6

# show the reports
tail -n5000 ${TEMP_DIR}/snyk-logs/agent-*.log
jq --color-output . ${TEMP_DIR}/*.json

# we must have hit the methodEntry
fgrep org/apache/struts2/dispatcher/multipart/JakartaMultiPartRequest ${TEMP_DIR}/*.json >/dev/null || (
    echo Class was never mentioned...
    exit 4
)
