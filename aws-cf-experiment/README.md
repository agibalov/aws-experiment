# aws-cf-experiment

Shows how to store a few sets of configuration parameters in a CloudFormation templates and switch between these 2 sets using a single template parameters. A workaround for CF's limit of "at most 60 parameters" per template.

Do `npm i` to install dependencies before using it.

* `./tool deploy --envTag=dev1 --envType=dev` to deploy environment of type 'dev' to stack with suffix 'dev1'.
* `./tool deploy --envTag=qa1 --envType=qa` to deploy environment of type 'qa' to stack with suffix 'qa'.
* `./tool undeploy --envTag=dev1` to undeploy.
