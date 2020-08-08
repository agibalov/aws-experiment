# terraform-experiment

## Prerequisites

* Terraform v0.12.26
* AWS CLI v2

## How to deploy and run

* `./tool.sh init` to construct the bucket that will store the Terraform state.
* `./tool.sh deinit` to destroy the bucket that stores the Terraform state.
* `./tool.sh deploy-dns` to deploy DNS resources.
* `./tool.sh undeploy-dns` to undeploy DNS resources.
* `sharedEnvTag=<SharedEnvTag> ./tool.sh deploy-shared` to deploy Shared resources (VPC, RDS, etc)
* `sharedEnvTag=<SharedEnvTag> ./tool.sh undeploy-shared` to undeploy Shared resources.
* `sharedEnvTag=<SharedEnvTag> appEnvTag=<AppEnvTag> ./tool.sh deploy-app` to deploy App resources (Mysql DB, ECS cluster, etc).
* `sharedEnvTag=<SharedEnvTag> appEnvTag=<AppEnvTag> ./tool.sh undeploy-app` to undeploy App resources.
