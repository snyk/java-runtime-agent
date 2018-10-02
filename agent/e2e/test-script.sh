#!/bin/bash

set -eux

# PWD: /root
# goof: /root/java-goof

# start the app
(
  cd java-goof &&
  MAVEN_OPTS="-javaagent:/root/snyk-agent.jar=file:///root/snyk-goof-e2e.properties" \
    mvn tomcat7:run
) &

TOMCAT_PID=$!

trap "kill ${TOMCAT_PID}" EXIT

# wait for the app to start
for i in {1..30}; do
    sleep 1

    echo 'Checking for startup, attempt '${i}'...'
    # we don't really care if this fails for another reason, we'll try it again later
    curl -s http://localhost:8080 && break
done

# start printing the agent logs
(tail -n5000 -f java-goof/snyk-agent-*.log | sed 's/^/snyk-agent.log: /') &disown

# exploit the app
<java-goof/exploits/struts-exploit-headers.txt sed "s/COMMAND/env/" | xargs curl -v -X GET http://localhost:8080 -H

# wait for the next report from the agent
sleep 6

# show the reports
jq --color-output . /var/tmp/snyk-data/*.json

# we must have hit the methodEntry
fgrep org/apache/struts2/dispatcher/multipart/JakartaMultiPartRequest /var/tmp/snyk-data/*.json >/dev/null || (
    echo Class was never mentioned...
    exit 4
)
