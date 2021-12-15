# proton-experiment

The AWS Proton hello world.

## How to deploy and run

* `./tool.sh deploy-base` (and `./tool.sh undeploy-base`) to deploy and undeploy the CloudFormation stack with resources necessary for Proton to run.
* `./tool.sh create-environment-template` (and `./tool.sh create-environment-template`) to create and delete the environment template.
* `./tool.sh create-environment-template-version` to create the environment template version.
* `envName=<ENVNAME> majorVersion=<MAJORVERSION> minorVersion=<MINORVERSION> ./tool.sh create-environment` to create the environment. `<ENVNAME>` is environment name, like `dev`. `<MAJORVERSION>` and `<MINORVERSION>` are the environment template version to be used for this environment (see the output of `./tool.sh create-environment-template-version`)

## Notes

TODO
