FROM buildpack-deps:18.04-scm

RUN export DEBIAN_FRONTEND=noninteractive && \
        apt-get update && \
        apt-get install -y openjdk-8-jdk-headless openjdk-11-jre-headless- maven netcat-openbsd jq python3 && \
        apt-get clean && \
        rm -rf /var/lib/apt/lists

WORKDIR /root

RUN git clone https://github.com/snyk/java-goof && \
        cd java-goof && \
        mvn package tomcat7:help

COPY snyk-agent.jar snyk-goof-e2e.properties test-script.sh /root/

ENTRYPOINT bash test-script.sh
