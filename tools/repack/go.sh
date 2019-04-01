#!/usr/bin/env bash

P="$(pwd)"
T="$(mktemp -d)"
cd "$T"

V=7.1

jar xf ~/.m2/repository/org/ow2/asm/asm/${V}/asm-${V}.jar
jar xf ~/.m2/repository/org/ow2/asm/asm-tree/${V}/asm-tree-${V}.jar

jar -cMf asm.jar org

cat >rewrite.properties <<'E'
rule org.objectweb.asm.** io.snyk.asm.@1
E

cat rewrite.properties

java -jar /usr/share/java/jarjar-1.4.jar process rewrite.properties asm.jar "${P}/asm-re-${V}.jar"
