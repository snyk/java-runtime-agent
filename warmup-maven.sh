#!/bin/sh
set -eu

D=~/.m2/snyk-index

if [ -d "$D" ]; then
    echo "${D} already exists, not warming caches."
    exit 0
fi

curl https://b.goeswhere.com/central-index-2018-11-13.tar.bz2 | tar -jxv
mkdir -p "$D"
mv central-index "${D}/"

echo Done.
