#!/bin/bash

set -eux

# PWD: /root
# goof: /root/java-goof

# run our "homebase" in the background
python3 simple_homebase.py 10 &
HOMEBASE_PID=$!

# start the app
(
  cd java-goof &&
  MAVEN_OPTS="-javaagent:/root/snyk-agent.jar=file:///root/snyk-goof-e2e.properties" \
    mvn tomcat7:run
) &

TOMCAT_PID=$!

trap "kill ${TOMCAT_PID} ${HOMEBASE_PID}" EXIT

# wait for the app to start
for i in 1 2 3 4 5; do
    sleep 1
    # we don't really care if this fails for another reason, we'll try it again later
    curl -q http://localhost:8080 && break
done

# start printing the agent logs
(tail -n5000 -f java-goof/snyk-agent-*.log | sed 's/^/snyk-agent.log: /') &disown

# exploit the app
<java-goof/exploits/struts-exploit-headers.txt sed "s/COMMAND/env/" | xargs curl -v -X GET http://localhost:8080 -H

# wait for the next report from the agent
sleep 6

# show the reports
jq --color-output . *.json

# we must have hit the methodEntry
fgrep org/apache/struts2/dispatcher/multipart/JakartaMultiPartRequest *.json >/dev/null || (
    echo Class was never mentioned...
    exit 4
)
