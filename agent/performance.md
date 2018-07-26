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
    on `errata`, assuming we can trust JMH for this kind of thing.
 * `23us * 50,000 == 1.2 seconds`.
 * Hand-measuring tomcat startup sees about a 1.8 second timezone. This
    could be the synchronisation before `loadClass`, it could be optimisation
    blockages, or it could just be measurement errors / bias. 


## Tested hardware

 * `errata`: Chris' laptop: i7-7700HQ, 16GB,
     OpenJDK8/Ubuntu 17.10 (probably soon 18.04).
