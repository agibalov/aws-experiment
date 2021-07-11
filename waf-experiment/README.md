# waf-experiment

An AWS WAF hello world

* `./tool.sh deploy-ecs` and `./tool.sh undeploy-ecs` to deploy and undeploy the ecs stack. This includes VPC, SGs, ECS cluster, ECR, etc. This part is common no matter if you want EC2 or Fargate.
* `./tool.sh deploy-app` and `./tool.sh undeploy-app` to deploy and undeploy the app stack.
