# pulumi-experiment

A Pulumi/AWS hello world.

## Prerequisites

1. Have AWS CLI installed and configured.
2. Export `AWS_REGION` and `AWS_PROFILE` before running.

## Running it

* `pulumi stack init hello-world` to create an empty stack in Pulumi Console (no resources get created).
* `pulumi up` to deploy the stack resources.
* `pulumi stack output` to get the list of stack outputs.
* `open $(pulumi stack output websiteUrl)` to open the website.
* `pulumi destroy` to undeploy the stack resources (this doesn't destroy the stack) 
* `pulumi stack rm` to destroy the stack.
