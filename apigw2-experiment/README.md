# apigw2-experiment

The AWS API Gateway v2 hello world.

## Prerequisites

* [wscat](https://www.npmjs.com/package/wscat)

## How to deploy and run

* `./tool.sh deploy` to deploy.
* `./tool.sh undeploy` to undeploy.
* `./tool.sh test-http` to test the HTTP API.
* `./tool.sh test-ws` to test the WebSocket API.

## Notes

* API Gateway v2 has the "auto deployments" feature (with v1 you have to `aws apigateway create-deployment ...` every time you make changes).
