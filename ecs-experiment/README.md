# ecs-experiment

An ECS hello world.

* `./tool.sh deploy-ecs` and `./tool.sh undeploy-ecs` to deploy and undeploy the ECS stack. This includes VPC, EC2 ASG, LB and ECS cluster.
* `./tool.sh deploy-app1-ecr` and `./tool.sh undeploy-app1-ecr` to deploy and undeploy the ECR stack for App1. This only includes the Docker repository.
* `./tool.sh deploy-app1` and `./tool.sh undeploy-app1` to deploy and undeploy the App1 application. This requires ECS and ECR stacks to be deployed.
