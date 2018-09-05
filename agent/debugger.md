## Debugger

### Advantages

 * Can probably dynamically add and remove breakpoints, during monitoring
 * Client/agent is a much more normal Java application,
    which can load libraries, and be debugged.
 * Dynamic disabling restores original speed.
 
### Disadvantages
 
 * Much more complicated application startup, except with launcher?
 * MethodEntry performance is worse than expected. Way worse.
 
Currently seeing a web request to goof balloon from ~40ms to ~234s (4 minutes).
