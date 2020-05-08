# aws-s3-experiment

* `./gradlew clean aws` to run tests using your default AWS profile.
* `./gradlew clean localstack` to run tests using using testcontainers/localstack.
* `./gradlew clean minio` to run tests using using testcontainers/minio.

As of 5/8/2019:

1. Minio doesn't support versions
2. LocalStack doesn't support etags and presigned URL headers

As of 11/11/2017 (LocalStack 0.6.0), their S3 doesn't support versioning and etags, so a few tests I have their don't make sense when running against LocalStack.
