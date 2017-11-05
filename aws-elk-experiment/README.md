# aws-elk-experiment

Learning CloudWatch logs aggregation with Elasticsearch.

Do `npm i` to install dependencies for `./tool`.

* `./tool deploy` to deploy everything.
* `./tool undeploy` to undeploy everything.

Note:

* Deployment takes about 11 minutes (most of the time is spent to create the Elasticsearch resource).
* Once deployment is done, go to Kibana (see the stack's `KibanaURL` output) and use `cwl-*` to configure an index pattern.
* `logstoelasticsearch.js` is a source of lambda function that AWS generates when you do "stream to Elasticsearch" in AWS console. I had to slightly adjust the code to make it configurable.
