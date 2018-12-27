#!/bin/bash

set -x

Region=us-east-1
StackName=dummy-stack1
DeploymentBucketName=loki2302-deployment1
WebsiteBucketName=loki2302-dummy-bucket1

command=$1

get_stack_output() {
  local stackName=$1
  local outputName=$2
  aws cloudformation describe-stacks \
    --stack-name ${stackName} \
    --query 'Stacks[0].Outputs[?OutputKey==`'${outputName}'`].OutputValue' \
    --output text \
    --region ${Region}
}

if [[ "$command" == "" ]]; then
  echo "No command specified"
elif [[ "$command" == "deploy" ]]; then
  echo "DEPLOYING"

  AWS_REGION=${Region} ./gradlew clean build
  if [[ $? != 0 ]]; then
    echo "Build failed"
    exit 1
  fi

  aws s3 mb s3://${DeploymentBucketName} --region ${Region}

  aws cloudformation package \
    --template-file serverless.yml \
    --s3-bucket ${DeploymentBucketName} \
    --output-template-file _packaged.yml

  aws cloudformation deploy \
    --template-file _packaged.yml \
    --stack-name ${StackName} \
    --capabilities CAPABILITY_IAM \
    --region ${Region} \
    --parameter-overrides \
    WebsiteBucketName=${WebsiteBucketName}

  aws s3 sync public s3://${WebsiteBucketName}/ --delete --acl public-read

  # Create new API deployment manually - CloudFormation DOESN'T do it
  restApiId=$(get_stack_output ${StackName} "RestApiId")
  restApiStageName=$(get_stack_output ${StackName} "RestApiStageName")
  aws apigateway create-deployment \
    --rest-api-id ${restApiId} \
    --stage-name ${restApiStageName} \
    --region ${Region}

  websiteUrl=$(get_stack_output ${StackName} "WebsiteUrl")
  restApiUrl=$(get_stack_output ${StackName} "RestApiUrl")
  echo "Website URL: ${websiteUrl}"
  echo "REST API URL: ${restApiUrl}"

  restApiKeyId=$(get_stack_output ${StackName} "RestApiKeyId")
  restApiKey=$(aws apigateway get-api-key \
    --api-key ${restApiKeyId} \
    --output text \
    --include-value \
    --query 'value' \
    --region ${Region})
  echo "API key: ${restApiKey}"

  jq -n \
    --arg apiUrl ${restApiUrl} \
    --arg apiKey ${restApiKey} \
    '{"apiUrl":$apiUrl,"apiKey":$apiKey}' > config.json
  aws s3 cp config.json s3://${WebsiteBucketName}/ --acl public-read

  swaggerHost=${restApiId}.execute-api.${Region}.amazonaws.com
  swaggerBasePath=/${restApiStageName}
  jq \
    --arg host "${swaggerHost}" \
    --arg basePath "${swaggerBasePath}" \
    '(.host = $host)|(.basePath = $basePath)' build/api.json > build/_api.json
  aws s3 cp build/_api.json s3://${WebsiteBucketName}/docs/api.json --acl public-read

elif [[ "$command" == "undeploy" ]]; then
  echo "UNDEPLOYING"

  aws s3 rm s3://${WebsiteBucketName}/ --recursive

  aws cloudformation delete-stack \
    --stack-name ${StackName} \
    --region ${Region}

  aws cloudformation wait stack-delete-complete \
    --stack-name ${StackName} \
    --region ${Region}

  aws s3 rm s3://${DeploymentBucketName}/ --recursive
  aws s3 rb s3://${DeploymentBucketName} --force

else
  echo "Unknown command '$command'"
fi
