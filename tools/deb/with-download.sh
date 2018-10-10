#!/bin/sh
set -eu

url=$1
out=$2

D=$(mktemp -d --tmpdir="$(dirname "$out")" --suffix=.extract)
trap 'rm -rf '"$D" EXIT

mkdir -p "$out"

curl -s "$url" \
    | bsdtar -Oxf /dev/stdin \
            --exclude=debian-binary \
            --exclude=control.tar.gz \
            --exclude=control.tar.bz2 \
            --exclude=control.tar.xz \
        | bsdtar -xf /dev/stdin \
            -C "$D"

find "$D" -type f -name '*.jar' -exec mv {} "${out}/" \;

# We could leave an empty directory, which will trick ninja
# into thinking it's done, but git won't track? That might work.
#rmdir --ignore-fail-on-non-empty "$out" # if it's empty

