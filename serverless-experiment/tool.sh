#!/bin/bash

region=us-east-1
stackName=dummy-stack1
deploymentBucketName=loki2302-deployment1
websiteBucketName=loki2302-dummy-bucket1

command=$1

get_stack_output() {
  stackName=$1
  outputName=$2
  aws cloudformation describe-stacks \
    --stack-name ${stackName} \
    --query 'Stacks[0].Outputs[?OutputKey==`'${outputName}'`].OutputValue' \
    --output text \
    --region ${region}
}

if [ "$command" == "" ]; then
  echo "No command specified"
elif [ "$command" == "deploy" ]; then
  echo "DEPLOYING"

  ./gradlew clean build
  if [ $? != 0 ]; then
    echo "Build failed"
    exit 1
  fi

  aws s3 mb s3://${deploymentBucketName} --region ${region}

  aws cloudformation package \
    --template-file serverless.yml \
    --s3-bucket ${deploymentBucketName} \
    --output-template-file _packaged.yml

  aws cloudformation deploy \
    --template-file _packaged.yml \
    --stack-name ${stackName} \
    --capabilities CAPABILITY_IAM \
    --region ${region} \
    --parameter-overrides \
    WebsiteBucketName=${websiteBucketName}

  aws s3 sync public s3://${websiteBucketName}/ --delete --acl public-read
  aws s3 cp build/api.json s3://${websiteBucketName}/docs/ --acl public-read

  # Create new API deployment manually - CloudFormation DOESN'T do it
  restApiId=$(get_stack_output ${stackName} "RestApiId")
  restApiStageName=$(get_stack_output ${stackName} "RestApiStageName")
  aws apigateway create-deployment \
    --rest-api-id ${restApiId} \
    --stage-name ${restApiStageName}

  websiteUrl=$(get_stack_output ${stackName} "WebsiteUrl")
  restApiUrl=$(get_stack_output ${stackName} "RestApiUrl")
  echo "Website URL: ${websiteUrl}"
  echo "REST API URL: ${restApiUrl}"

  restApiKeyId=$(get_stack_output ${stackName} "RestApiKeyId")
  restApiKey=$(aws apigateway get-api-key --api-key ${restApiKeyId} \
    --output text --include-value --query 'value')

  jq -n --arg apiUrl ${restApiUrl} --arg apiKey ${restApiKey} '{"apiUrl":$apiUrl,"apiKey":$apiKey}' > config.json
  aws s3 cp config.json s3://${websiteBucketName}/ --acl public-read

elif [ "$command" == "undeploy" ]; then
  echo "UNDEPLOYING"

  aws s3 rm s3://${websiteBucketName}/ --recursive

  aws cloudformation delete-stack \
    --stack-name ${stackName} \
    --region ${region}

  aws cloudformation wait stack-delete-complete \
    --stack-name ${stackName} \
    --region ${region}

  aws s3 rm s3://${deploymentBucketName}/ --recursive
  aws s3 rb s3://${deploymentBucketName} --force

else
  echo "Unknown command '$command'"
fi
