## play-access-logs-to-metrics

This lambda will periodically read the provided ALB logs (through Athena), and compute daily aggregate for each endpoint
of the given Play applications.

### Concept

We have very large Play applications, such as [frontend](https://github.com/guardian/frontend/) or MAPI.
Both these applications have now existed for decades, and it's becoming hard to keep track of what endpoints are
actually being used.

The idea behind this lambda is to read the access logs as close to the application as possible, match it against the
`routes` file of the application, and compute daily aggregates for each endpoint.

### Configuration

The lambda is configured through two SSM properties in your account:

- `/${this.stage}/${this.stack}/play-access-logs-to-metrics/athenaOutputLocation`: the S3 location where the Athena
  query results will be
  stored. It should be in the format `s3://bucket/prefix/`.
- `/${this.stage}/${this.stack}/play-access-logs-to-metrics/inputConfig`: a JSON string containing the configuration for
  the lambda. It
  should have the following format:

```json
{
  "apps": [
    {
      "app": "facia",
      "stack": "frontend",
      "stage": "PROD",
      "routesUrl": "https://raw.githubusercontent.com/guardian/frontend/refs/heads/main/facia/conf/routes"
    },
    {
      "app": "onward",
      "stack": "frontend",
      "stage": "PROD",
      "routesUrl": "http://raw.githubusercontent.com/guardian/frontend/refs/heads/main/onward/conf/routes"
    }
  ]
}
```

