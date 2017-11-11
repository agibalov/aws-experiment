# aws-elk-experiment

Learning CloudWatch logs aggregation with Elasticsearch.

Do `npm i` to install dependencies for `./tool`.

* `./gradlew clean build` to build the Dummy lambda.
* `./tool deploy` to deploy everything.
* `./tool undeploy` to undeploy everything.
* `./tool delete-old-indexes` to delete indexes older than ...

Note:

* Deployment takes about 11 minutes (most of the time is spent to create the Elasticsearch resource).
* The `AccessPolicies` are configured the way that you can only access Kibana using your AWS account. For this you need [AWS Request Signer](https://chrome.google.com/webstore/detail/aws-request-signer/edllpeohmcgobpcpciffdaddiinfcghf?utm_source=chrome-app-launcher-info-dialog) Chrome extension. Or alternatively just change the `AccessPolicies` to allow public access.
* Once deployment is done, go to Kibana (see the stack's `KibanaURL` output) and use `cwl-*` to configure an index pattern.
* `logstoelasticsearch.js` is a source of lambda function that AWS generates when you do "stream to Elasticsearch" in AWS console. I had to slightly adjust the code to make it configurable.
* There's no straightforward way to make Elasticsearch delete outdated indexes. The official solution for this is [Curator](https://github.com/elastic/curator). The `./tool delete-old-indexes` command runs the `curator-delete-old-indexes.sh` script, that uses the [bobrik/curator:5.2.0](https://hub.docker.com/r/bobrik/curator/).
