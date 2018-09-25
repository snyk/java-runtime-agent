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

(removed a load of text about the `getAndIncrement` method, now using `lazySet`.)

`anoia`:

 * no agent: 23.993 23.843 23.912; mean: 23.916, sd: 0.075
 * reporting just classpath (no classes match): 24.134 24.274 23.979
 * all branching match, empty instrumentation method (very suspicious): 24.098 24.550 24.318
 * all branching match, `lazySet`: 28.608 28.579 28.652
 * osmosis/protobuf, `lazySet`: 27.821 28.069 27.918
 * osmosis, `lazySet`: 24.654 24.575 25.659


## Tested hardware

 * `errata`: Chris' laptop: i7-7700HQ, 16GB,
     OpenJDK8/Ubuntu 17.10 (probably soon 18.04).
 * `anoia`: Chris' desktop: i7-8700K, 32GB,
     OpenJDK8/Ubuntu 18.04.
