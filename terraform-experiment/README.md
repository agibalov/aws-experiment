# terraform-experiment

## Prerequisites

* Terraform v0.12.26

## How to deploy and run

* `./tool.sh init` to construct the bucket that will store the Terraform state.
* `./tool.sh deinit` to destroy the bucket that stores the Terraform state.
* `./tool.sh deploy-dns` to deploy DNS resources.
* `./tool.sh undeploy-dns` to undeploy DNS resources.
* `./tool.sh deploy-shared` to deploy Shared resources (VPC, RDS)
* `./tool.sh undeploy-shared` to undeploy Shared resources.
