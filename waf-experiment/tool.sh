#!/bin/bash

set -x

Region=us-east-1
EcsStackName=ecs
AppStackName=app

command=$1

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

if [[ "${command}" == "deploy-ecs" ]]; then
  aws cloudformation deploy \
    --template-file cloudformation/ecs.yml \
    --stack-name ${EcsStackName} \
    --capabilities CAPABILITY_NAMED_IAM \
    --region ${Region}

elif [[ "${command}" == "undeploy-ecs" ]]; then
  undeploy_stack ${EcsStackName}

elif [[ "${command}" == "deploy-app" ]]; then
  aws cloudformation deploy \
    --template-file cloudformation/app.yml \
    --stack-name ${AppStackName} \
    --capabilities CAPABILITY_NAMED_IAM \
    --region ${Region} \
    --parameter-overrides \
    EcsStackName=${EcsStackName}

elif [[ "${command}" == "undeploy-app" ]]; then
  undeploy_stack ${AppStackName}

elif [[ "${command}" == "" ]]; then
  echo "No command specified"
fi
