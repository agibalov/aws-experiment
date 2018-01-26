# aws-messaging-experiment

`VisitsKinesisStream -> VisitEnrichmentServiceLambdaFunction -> EnrichedVisitsKinesisStream -> GreetingsServiceLambdaFunction`

* `./tool deploy --with-visit-enrichment-service --with-greetings-service` to deploy everything.
* `./tool deploy --with-visit-enrichment-service` to deploy only `VisitEnrichmentService` and what it needs.
* `./tool deploy --with-greetings-service` to deploy `GreetingsService` and what it needs.
* `./tool undeploy` to undeploy whatever is deployed.
