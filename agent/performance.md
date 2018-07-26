## Performance

What kind of overhead are we expecting here?


### Expectations

NewRelic claim 2-3%, can be up to 30% in production according to rumour.

We're expecting the "only observe methods with vulnerabilities" approach
to have an unmeasurable overhead.

### goof + ab

`ab -n 5000 -c 8 http://localhost:8080/` isn't accurate enough to measure the overhead on
`errata` (Chris' laptop).

