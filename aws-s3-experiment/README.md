# aws-s3-experiment

* `./gradlew clean test` to run tests using testcontainers/localstack.
* `./gradlew clean aws` to run tests using your default AWS profile.

As of 11/11/2017 (LocalStack 0.6.0), their S3 doesn't support versioning and etags, so a few tests I have their don't make sense when running against LocalStack.
