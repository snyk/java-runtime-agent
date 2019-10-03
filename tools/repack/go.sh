#!/usr/bin/env bash
set -eu

P="$(pwd)"
T="$(mktemp -d)"
cd "$T"

V=7.2

mvn -q org.apache.maven.plugins:maven-dependency-plugin:3.1.1:get -Dartifact=org.ow2.asm:asm:$V
mvn -q org.apache.maven.plugins:maven-dependency-plugin:3.1.1:get -Dartifact=org.ow2.asm:asm-tree:$V

jar xf ~/.m2/repository/org/ow2/asm/asm/${V}/asm-${V}.jar
jar xf ~/.m2/repository/org/ow2/asm/asm-tree/${V}/asm-tree-${V}.jar

jar -cMf asm.jar org

cat >rewrite.properties <<'E'
rule org.objectweb.asm.** io.snyk.asm.@1
E

cat rewrite.properties

java -jar /usr/share/java/jarjar-1.4.jar process rewrite.properties asm.jar "${P}/asm-re-${V}.jar"
