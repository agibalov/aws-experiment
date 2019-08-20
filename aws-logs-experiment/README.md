# aws-logs-experiment

* `./tool.sh deploy` to create log group, log stream and publish sample records.
* `./tool.sh test` to run a dummy query. It may not see the newly created records immediately after deployment. Give it some time and try again.
* `./tool.sh undeploy` to destroy log stream and log group.
