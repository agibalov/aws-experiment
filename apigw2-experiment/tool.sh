#!/bin/bash

set -x

Region=us-east-1

command=$1

get_app_stack_name() {
  echo "apigw2-experiment"
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
  stackName=$(get_app_stack_name)
  aws cloudformation deploy \
    --template-file template.yml \
    --stack-name ${stackName} \
    --capabilities CAPABILITY_NAMED_IAM \
    --region ${Region}

elif [[ "${command}" == "undeploy" ]]; then
  stackName=$(get_app_stack_name)
  undeploy_stack ${stackName}

elif [[ "${command}" == "test" ]]; then
  stackName=$(get_app_stack_name)
  apiEndpoint=$(get_stack_output ${stackName} "ApiEndpoint")
  curl \
    --request POST \
    --write-out '\n' \
    ${apiEndpoint}/hello-there

elif [[ "${command}" == "" ]]; then
  echo "No command specified"
else
  echo "Unknown command ${command}"
fi
