# secrets-manager-experiment

* Deploy: `./tool.sh deploy`
* Undeploy: `./tool.sh undeploy`
* Test: `./tool.sh test`

## Notes

* Secret substitution is known to practically work for Lambda and ECS environment variables. Here: https://docs.aws.amazon.com/systems-manager/latest/userguide/integration-ps-secretsmanager.html they mention "Amazon EC2, Amazon Elastic Container Service, AWS Lambda, AWS CloudFormation, AWS CodeBuild, AWS CodeDeploy".
