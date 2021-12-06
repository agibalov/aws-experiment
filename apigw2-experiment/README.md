# apigw2-experiment

The AWS API Gateway v2 hello world.

## Prerequisites

* [wscat](https://www.npmjs.com/package/wscat)

## How to deploy and run

* `./tool.sh deploy` to deploy.
* `./tool.sh undeploy` to undeploy.
* `./tool.sh test-http` to test the HTTP API.
* `./tool.sh test-ws` to connect to WebSocket API. Then:
  Type: `{"action":"SendMessage","text":"omg"}`

  You should get something like:
  ```
  Hello! The time is 2021-12-06T21:03:51.966Z and you're saying: "omg"
  ```

## Notes

* API Gateway v2 has the "auto deployments" feature (with v1 you have to `aws apigateway create-deployment ...` every time you make changes).
