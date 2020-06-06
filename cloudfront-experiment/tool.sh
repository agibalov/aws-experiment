#!/bin/bash

set -x

Region=us-east-1

command=$1

get_app_stack_name() {
  local envTag=$1
  echo "${envTag}-cloudfront-experiment"
}

undeploy_stack() {
  local stackName=$1
  aws cloudformation delete-stack \
    --stack-name ${stackName} \
    --region ${Region}

  aws cloudformation wait stack-delete-complete \
    --stack-name ${stackName} \
    --region ${Region}
}

get_stack_output() {
  local stackName=$1
  local outputName=$2
  aws cloudformation describe-stacks \
    --stack-name ${stackName} \
    --query 'Stacks[0].Outputs[?OutputKey==`'${outputName}'`].OutputValue' \
    --output text \
    --region ${Region}
}

if [[ "${command}" == "deploy" ]]; then
  envTag=${envTag:?not set or empty}
  appStackName=$(get_app_stack_name ${envTag})
  aws cloudformation deploy \
    --template-file template.yml \
    --stack-name ${appStackName} \
    --capabilities CAPABILITY_NAMED_IAM \
    --region ${Region} \
    --parameter-overrides \
    EnvTag=${envTag}

  bucketName=$(get_stack_output ${appStackName} "BucketName")
  aws s3 sync content s3://${bucketName} --delete

elif [[ "${command}" == "undeploy" ]]; then
  envTag=${envTag:?not set or empty}
  appStackName=$(get_app_stack_name ${envTag})

  bucketName=$(get_stack_output ${appStackName} "BucketName")
  aws s3 rm s3://${bucketName} --recursive

  undeploy_stack ${appStackName}

elif [[ "${command}" == "" ]]; then
  echo "No command specified"
else
  echo "Unknown command ${command}"
fi
