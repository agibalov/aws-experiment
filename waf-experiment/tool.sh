#!/bin/bash

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

elif [[ "${command}" == "test" ]]; then
  rootUrl=$(get_stack_output ${AppStackName} "Url")
  echo "url: ${rootUrl}"

  i=0
  while [ $i -ne 100000 ]
  do
    i=$(($i+1))
    echo "$i: $(curl -s -o /dev/null -w "%{http_code}" ${rootUrl})"
    sleep 0.1
  done
elif [[ "${command}" == "" ]]; then
  echo "No command specified"
fi
