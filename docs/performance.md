## Performance

What kind of overhead are we expecting here?


### Expectations

NewRelic claim 2-3%, can be up to 30% in production according to rumour.

We're expecting the "only observe methods with vulnerabilities" approach
to have an unmeasurable overhead.


### goof + ab

`ab -n 5000 -c 8 http://localhost:8080/` isn't accurate enough to measure the overhead on
`errata`.


### Startup

 * Loading maven and tomcat loads about `50,000` classes.
 * Synthetic benchmarks of rewriting a class takes about `23us` (yes, us)
    on `errata`, 20us on `anoia`, assuming we can trust JMH for this kind of thing.
 * `23us * 50,000 == 1.2 seconds`.
 * Parsing the class for the tree API accounts for about 60% of the overhead,
    all of this is in ASM. In theory, we could not use the tree API. Not sure
    how much overhead we would re-introduce in the process. Chris guesses a lot.
 * Hand-measuring tomcat startup sees about a 1.8 second timezone. This
    could be the synchronisation before `loadClass`, it could be optimisation
    blockages, or it could just be measurement errors / bias.
 * Original startup time is about 7 seconds, so that's a 20% slowdown. :(


### osmosis

```$bash
A='-javaagent:/home/faux/code/java-instrumentor/agent/build/libs/agent.jar=file:/home/faux/code/java-instrumentor/agent/'
OSMOSIS=/home/faux/clone/osmosis/package/bin/osmosis
time $OSMOSIS --read-pbf england-latest.osm.pbf --node-key-value keyValueList="highway.speed_camera" --write-xml radar.osm
```

`anoia`:

 * no agent: 23.993 23.843 23.912; mean: 23.916, sd: 0.075
 * reporting just classpath (no classes match): 24.134 24.274 23.979
 * reporting every method everywhere: 1:25.64 1:26.28 1:26.70; mean: 86.21, sd: 0.53
 * `org/openstreetmap/osmosis/osmbinary/BinaryParser` 24.853 25.396 25.053; mean: 25.101, sd: 0.275

`BinaryParser` has some *very* hot methods in, called tens of millions of times.

25.101/23.916 = 1.049; i.e. a ~5% slowdown.
86.21/23.916 = 3.6, i.e. a 3x slowdown

It's not clear whether the "all" case is just lots of 5% slowdowns, or if there's some
super hot method that's causing an issue.

 * `org/openstreetmap/osmosis/osmbinary/**`: 31.211
 * `org/openstreetmap/osmosis/osmbinary/**` `crosby/**`: 30.336 (hopefully just noise, it is matching both)
 * `com/google/protobuf/**` 69.94 (oh dear)
 * `com/google/protobuf/IntArrayList` 30.974

IntArrayList is interesting, it must be very heavily used, and the methods are
effectively free without our instrumentation. It's not a `getter` as it takes an
index and an array lookup. Maybe it has significant inlining wins?

Only IntArrayList, method filter (hacked in):

 * Everything but range check: 29.223
 * Everything but add*: 27.191
 * Everything but get*: 29.327???
 * Remove only: 24.052
 * No get/set/add: 25.679

Right, wtf?
 
 * only make..Message: 23.303
 * + mutableCopyWithCapacity: 24.501
 * + emptyList: 24.214
 * + add: 27.712
 
Seems that 'add' really is the cost. I wonder why.

https://github.com/protocolbuffers/protobuf/blob/v3.4.0/java/core/src/main/java/com/google/protobuf/IntArrayList.java#L154
 

Small but non-zero slowdown:
 * `com/google/protobuf/GeneratedMessage**`: 24.932
 * `com/google/protobuf/ByteString` 24.159
 * `com/google/protobuf/ByteString**` 25.000


## osmosis, but with "branches" filtering enabled

 * reporting just classpath (no classes match): 24.134 24.274 23.979
 * (old-style) reporting every method everywhere: 1:25.64 1:26.28 1:26.70; mean: 86.21, sd: 0.53
 * every method, except ones without branches: 56.624 56.880 56.973
 
 * `com/google/protobuf/IntArrayList` 27.1 (30.9 before)
 * `com/google/protobuf/**` 46.8 (69.94 before)
 * `org/openstreetmap/osmosis/osmbinary/**` 28.5 (31.2 before)


## Tested hardware

 * `errata`: Chris' laptop: i7-7700HQ, 16GB,
     OpenJDK8/Ubuntu 17.10 (probably soon 18.04).
 * `anoia`: Chris' desktop: i7-8700K, 32GB,
     OpenJDK8/Ubuntu 18.04.
