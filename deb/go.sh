#!/bin/sh
egrep 'build .dest.*/lib[a-z0-9-]*-java_' sid.ninja | cut -d/ -f 2- | sed 's/\$suffix.*//; s/\$//g' | xargs ninja
