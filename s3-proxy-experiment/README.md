# s3-proxy-experiment

Expose S3 bucket contents as a website:

1. Build directory indexes if there's no index.html
2. Access various buckets by specifying bucket name in the URL
3. Add some security by requiring a secret word in the URL

## How to deploy and undeploy

* `envTag=dev ./tool.sh deploy` to deploy.
* `envTag=dev ./tool.sh undeploy` to undeploy.

The stack outputs include:

* `AccessibleBucketUrl` demonstrates what it looks like when the app has access to the bucket.
* `InaccessibleBucketUrl` demonstrates what it looks like when the app doesn't have access to the bucket.

## Notes

* A template-only solution - no external dependencies, no tooling except for AWS CLI. Uses API Gateway and Lambda.
* Having a secret word in the URL doesn't actually make things much more secure.
* HTTP caching is not supported, so every time a file is requested, it gets retrieved from the bucket. 
