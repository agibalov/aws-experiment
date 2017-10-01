# apigw-s3-experiment

An attempt to make API Gateway expose the S3 bucket contents. In theory, this allows you to restrict access to the content.

* `./tool.sh deploy` to deploy.
* `./tool.sh undeploy` to undeploy.
* `./tool.sh test` to run tests.

*Known issues*: binary files like PNG and JPEG get BASE64-encoded, even though `Content-Type` is `image/png` or `image/jpeg`.
