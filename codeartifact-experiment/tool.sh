#!/bin/bash

set -x

Region=us-east-1

command=$1

get_app_stack_name() {
  echo "codeartifact-experiment"
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

if [[ "${command}" == "deploy" ]]; then
  branch=${branch:?not set or empty}
  stackName=$(get_app_stack_name)
  aws cloudformation deploy \
    --template-file template.yml \
    --stack-name ${stackName} \
    --capabilities CAPABILITY_NAMED_IAM \
    --region ${Region} \
    --parameter-overrides \
    BranchName=${branch}

elif [[ "${command}" == "undeploy" ]]; then
  stackName=$(get_app_stack_name)
  undeploy_stack ${stackName}

elif [[ "${command}" == "" ]]; then
  echo "No command specified"
else
  echo "Unknown command ${command}"
fi
