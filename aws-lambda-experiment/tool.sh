#!/bin/bash

set -x

Region=us-east-1

command=$1

get_source_bucket_name() {
  local envTag=$1
  echo "${envTag}-lambda-experiment-source"
}

get_app_stack_name() {
  local envTag=$1
  echo "${envTag}-lambda-experiment"
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
  sourceBucketName=$(get_source_bucket_name ${envTag})
  aws s3 mb s3://${sourceBucketName} --region ${Region}
  aws cloudformation package \
    --template-file template.yml \
    --s3-bucket ${sourceBucketName} \
    --output-template-file _packaged.yml

  stackName=$(get_app_stack_name ${envTag})
  aws cloudformation deploy \
    --template-file _packaged.yml \
    --stack-name ${stackName} \
    --capabilities CAPABILITY_NAMED_IAM \
    --region ${Region} \
    --parameter-overrides \
    EnvTag=${envTag}

  restApiId=$(get_stack_output ${stackName} "RestApiId")
  restApiStageName=$(get_stack_output ${stackName} "RestApiStageName")
  aws apigateway create-deployment \
    --rest-api-id ${restApiId} \
    --stage-name ${restApiStageName} \
    --region ${Region}

elif [[ "${command}" == "undeploy" ]]; then
  envTag=${envTag:?not set or empty}
  stackName=$(get_app_stack_name ${envTag})
  undeploy_stack ${stackName}

  sourceBucketName=$(get_source_bucket_name ${envTag})
  aws s3 rm s3://${sourceBucketName}/ --recursive
  aws s3 rb s3://${sourceBucketName}

elif [[ "${command}" == "test" ]]; then
  envTag=${envTag:?not set or empty}
  stackName=$(get_app_stack_name ${envTag})
  restApiUrl=$(get_stack_output ${stackName} "RestApiUrl")
  curl \
    --request POST \
    --write-out '\n' \
    ${restApiUrl}/js
  curl \
    --request POST \
    --write-out '\n' \
    ${restApiUrl}/js/123
  curl \
    --request POST \
    --write-out '\n' \
    ${restApiUrl}/java
  curl \
    --request POST \
    --write-out '\n' \
    ${restApiUrl}/java/123

elif [[ "${command}" == "" ]]; then
  echo "No command specified"
else
  echo "Unknown command ${command}"
fi
