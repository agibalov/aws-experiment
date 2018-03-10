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

  rm dist.zip
  zip -r dist.zip node_modules static lambda.js package.json package-lock.json

  aws s3 mb s3://${deploymentBucketName} --region ${region}

  aws cloudformation package \
    --template-file cloudformation.yml \
    --s3-bucket ${deploymentBucketName} \
    --output-template-file _packaged.yml

  aws cloudformation deploy \
    --template-file _packaged.yml \
    --stack-name ${stackName} \
    --capabilities CAPABILITY_IAM \
    --region ${region}

  restApiId=$(get_stack_output ${stackName} "RestApiId")
  restApiStageName=$(get_stack_output ${stackName} "RestApiStageName")
  aws apigateway create-deployment \
    --rest-api-id ${restApiId} \
    --stage-name ${restApiStageName}

  websiteUrl=$(get_stack_output ${stackName} "WebSiteUrl")
  echo "Website URL: ${websiteUrl}"

elif [ "$command" == "undeploy" ]; then
  echo "UNDEPLOYING"

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
