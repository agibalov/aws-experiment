# ecs-experiment

An ECS hello world.

* `./gradlew clean test` to run local integration tests (`@SpringBootTest`, `RestTemplate`, etc)
* `./tool.sh deploy-ecs` and `./tool.sh undeploy-ecs` to deploy and undeploy the ECS stack. This includes VPC, EC2 ASG, LB and ECS cluster.
* `./tool.sh deploy-app1-ecr` and `./tool.sh undeploy-app1-ecr` to deploy and undeploy the ECR stack for App1. This only includes the Docker repository.
* `./tool.sh deploy-app1` and `./tool.sh undeploy-app1` to deploy and undeploy the App1 application. This requires ECS and ECR stacks to be deployed.
* `./gradlew clean awsTest` to run post-deployment API tests (uses CloudFormation API to get App1 URL, then uses `RestTemplate` to make an HTTP request)

#### GET `/hello`

```js
{
  "message": "Hello there Wed Sep 27 22:55:26 UTC 2017!!!"
}
```

#### GET `/items`

(every time you make this request there's +1 new item)

```js
[
  {
    "id": 1,
    "text": "Note Wed Sep 27 22:49:49 UTC 2017 ae395d78-3f49-4fd3-b85f-38fbce299256"
  },
  {
    "id": 2,
    "text": "Note Wed Sep 27 22:49:53 UTC 2017 10ea702a-7b9d-4763-b69f-b02fa6b23ed3"
  }
]
```
