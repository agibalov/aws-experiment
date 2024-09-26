# proton-experiment

The AWS Proton hello world.

## Prerequisites

* AWS CLI
* Python 3.8+
* jq

## How to deploy and run

* `./tool.sh deploy-base` (and `./tool.sh undeploy-base`) to deploy and undeploy the CloudFormation stack with resources necessary for Proton to run.
* `./tool.sh create-environment-template` (and `./tool.sh delete-environment-template`) to create and delete the environment template.
* `./tool.sh create-environment-template-version` to create the environment template version.
* `envName=<ENVNAME> majorVersion=<MAJORVERSION> minorVersion=<MINORVERSION> ./tool.sh create-environment` to create (and destroy) the environment. `<ENVNAME>` is environment name, like `dev`. `<MAJORVERSION>` and `<MINORVERSION>` are the environment template version to be used for this environment (see the output of `./tool.sh create-environment-template-version`)
* `./tool.sh create-service-template` (and `./tool.sh delete-service-template`) to create and delete the service template.
* `envMajorVersion=<ENVMAJORVERSION> ./tool.sh create-service-template-version` to create the service template version.
* `envName=<ENVNAME> serviceName=<SERVICENAME> majorVersion=<MAJORVERSION> ./tool.sh create-service` (and `serviceName=<SERVICENAME> ./tool.sh delete-service`) to create and destroy services. `<ENVNAME>` is environment name, like `dev`. `<SERVICENAME>` is a service name, like `app1`. `<MAJORVERSION>` is the major version of the service template  (see the output of `./tool.sh create-service-template-version`)

## Notes

* Figure out CodeStar Github repository connections - either document step by step or automate.
* Proton makes an attempt to streamline infrastructure management by introducing "environments" (which are "just the AWS resources") and "services" (which are "the AWS resources + pipelines").
* TODO: build an ECS hello world (https://github.com/aws-samples/aws-proton-sample-templates/tree/main/loadbalanced-fargate-svc)
* TODO: use minor versions for development (avoid having to undeploy before deploying updates) 
