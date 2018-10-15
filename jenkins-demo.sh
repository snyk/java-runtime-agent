#!/usr/bin/env bash

JENKINS_VERSION=2.112

# fetch the jenkins war
WAR=jenkins-${JENKINS_VERSION}.war
[ -f "$WAR" ] || wget -O "$WAR" http://mirrors.jenkins.io/war/${JENKINS_VERSION}/jenkins.war

# build the agent
(cd agent && ./gradlew test shadow)

java \
    -javaagent:$(pwd)/agent/build/libs/snyk-java-runtime-agent.jar=file:$(pwd)/agent/snyk-jenkins.properties \
    -jar "$WAR"
