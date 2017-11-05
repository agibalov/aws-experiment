# aws-elk-experiment

Learning CloudWatch logs aggregation with Elasticsearch.

Do `npm i` to install dependencies for `./tool`.

* `./gradlew clean build` to build the Dummy lambda.
* `./tool deploy` to deploy everything.
* `./tool undeploy` to undeploy everything.

Note:

* Deployment takes about 11 minutes (most of the time is spent to create the Elasticsearch resource).
* Once deployment is done, go to Kibana (see the stack's `KibanaURL` output) and use `cwl-*` to configure an index pattern.
* `logstoelasticsearch.js` is a source of lambda function that AWS generates when you do "stream to Elasticsearch" in AWS console. I had to slightly adjust the code to make it configurable.

Concerns/todos:

* `DummyLambda` outputs logs to console, which means that the best thing CloudWatch can do is reading the console output line by line. Things like exception stacktraces don't work with this approach - every line get's interpreted as an individual record. My current workaround is to replace `'\n'` with `''` in stacktraces (obviously a bad solution). The options to consider are: 
  * Log stuff in JSON format. [CloudWatch should be OK with it](http://docs.aws.amazon.com/AmazonCloudWatch/latest/logs/FilterAndPatternSyntax.html) on one hand, and on the other hand I'll be able to use multiline strings.
  * Don't do console output, use some CloudWatch-specific log appender. 
* Kibana is publicly available without any authentication.
  * Just adjust the `AccessPolicies` of `MyElasticsearchDomain`.
