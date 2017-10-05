#!/bin/bash

region=us-east-1
stackName=dummy-stack1
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

  aws cloudformation deploy \
    --template-file serverless.yml \
    --stack-name ${stackName} \
    --capabilities CAPABILITY_IAM \
    --region ${region} \
    --parameter-overrides \
    WebsiteBucketName=${websiteBucketName}

  aws s3 cp public s3://${websiteBucketName}/ --recursive --acl public-read

  websiteUrl=$(get_stack_output ${stackName} "WebsiteUrl")
  restApiUrl=$(get_stack_output ${stackName} "RestApiUrl")
  echo "Website URL: ${websiteUrl}"
  echo "REST API URL: ${restApiUrl}"

  jq -n --arg apiUrl ${restApiUrl} '{"apiUrl":$apiUrl}' > config.json
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

else
  echo "Unknown command '$command'"
fi
