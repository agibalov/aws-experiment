#!/bin/bash

set -x

Region=us-east-1
SecretsManagegerExperimentStackName=secrets-manager

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

get_stack_parameter() {
  local stackName=$1
  local parameterName=$2
  aws cloudformation describe-stacks \
    --stack-name ${stackName} \
    --query 'Stacks[0].Parameters[?ParameterKey==`'${parameterName}'`].ParameterValue' \
    --output text \
    --region ${Region}
}

if [[ "${command}" == "deploy-secrets-manager-experiment" ]]; then
  aws cloudformation deploy \
    --template-file templates/secrets-manager-experiment.yml \
    --stack-name ${SecretsManagegerExperimentStackName} \
    --capabilities CAPABILITY_NAMED_IAM \
    --region ${Region}

elif [[ "${command}" == "undeploy-secrets-manager-experiment" ]]; then
  undeploy_stack ${SecretsManagegerExperimentStackName}

elif [[ "${command}" == "test-secrets-manager-experiment" ]]; then
  testFunctionName=$(get_stack_output ${SecretsManagegerExperimentStackName} "TestFunctionName")
  aws lambda invoke \
    --function-name ${testFunctionName} \
    1.txt \
    --region ${Region}
  cat 1.txt
  rm 1.txt

elif [[ "${command}" == "" ]]; then
  echo "No command specified"
fi
