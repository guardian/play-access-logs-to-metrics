## play-access-logs-to-metrics

This lambda will periodically read the provided ALB logs (through Athena), and compute daily aggregate for each endpoint of the given Play applications.

### Concept

We have very large Play applications, such as [frontend](https://github.com/guardian/frontend/) or MAPI. 
Both these applications have now existed for decades, and it's becoming hard to keep track of what endpoints are actually being used.

The idea behind this lambda is to read the access logs as close to the application as possible, match it against the `routes` file of the application, and compute daily aggregates for each endpoint.

### Configuration

