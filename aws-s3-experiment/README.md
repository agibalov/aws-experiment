# aws-s3-experiment

Learning AWS S3 API. Do `./gradlew clean test` to run tests using your default AWS profile.

Also contains an attempt to use [LocalStack](https://github.com/localstack/localstack) to run tests locally. As of LocalStack 0.8.1, tests are failing:

* `me.loki2302.DummyTest.eTagConstraintScenario()` - it looks like LocalStack doesn't support conditional requests yet (the test is about using the eTag feature). Fails immediately.
* `me.loki2302.DummyTest.versioningScenario()` - takes a few minutes to finish. There are errors in LocalStack output (`Error forwarding request: string index out of range`, `Error forwarding request: 'boundary'`). Fails on `setBucketVersioningConfiguration()` call.
* `me.loki2302.DummyTest.basicScenario()` - takes a few minutes to finish. There are errors in LocalStack output (`Error forwarding request: 'boundary'`). Fails on `deleteObjects()` call.

To run it:

* Do `docker-compose up` to run the LocalStack. Wait for it to say "Ready."
* Do `./gradlew clean localstack` to run tests against LocalStack.
