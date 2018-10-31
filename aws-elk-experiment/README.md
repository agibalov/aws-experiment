# aws-elk-experiment

Learning CloudWatch logs aggregation with Elasticsearch (with Cognito authentication).

Do `npm i` to install dependencies for `./tool`.

* `./gradlew clean build` to build the Dummy lambda.
* `./tool deploy` to deploy everything.
* `./tool undeploy` to undeploy everything.
* `./tool delete-old-indexes` to delete indexes older than ...

Note:

* Before deploying it, modify `cf-cognito.yml` to use your email address instead of the default one. You'll need a user to sign in to Kibana.
* Deployment takes about 11 minutes (most of the time is spent to create the Elasticsearch resource). Once deployment is done, ES needs some time to finalize the configuration changes ("Domain status" is "Processing"). Wait for status to become "Ready". Even at this point it may be not 100%, so if next steps don't work for you, give it more time.
* Once ES is ready, go to Kibana (see the stack's `KibanaURL` output) and see it wants your credentials. Supply the credentials, reset the password and see the Kibana configuration page.
* Use `cwl-*` to configure the index pattern.
* `logstoelasticsearch.js` is a source of lambda function that AWS generates when you do "stream to Elasticsearch" in AWS console. I had to slightly adjust the code to make it configurable.
* There's no straightforward way to make Elasticsearch delete outdated indexes. The official solution for this is [Curator](https://github.com/elastic/curator). The `./tool delete-old-indexes` command runs the `curator-delete-old-indexes.sh` script, that uses the [bobrik/curator:5.2.0](https://hub.docker.com/r/bobrik/curator/).
