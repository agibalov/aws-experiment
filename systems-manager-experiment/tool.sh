#!/bin/bash

set -x

Region=us-east-1
StackName=systems-manager-experiment

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

if [[ "${command}" == "deploy" ]]; then
  aws ssm put-parameter \
    --name ThePassword \
    --value qwerty \
    --type SecureString \
    --overwrite \
    --region ${Region}

  aws cloudformation deploy \
    --template-file templates/experiment.yml \
    --stack-name ${StackName} \
    --capabilities CAPABILITY_NAMED_IAM \
    --region ${Region}

elif [[ "${command}" == "undeploy" ]]; then
  undeploy_stack ${StackName}

  aws ssm delete-parameter \
    --name ThePassword \
    --region ${Region}

elif [[ "${command}" == "test" ]]; then
  thePassword=$(aws ssm get-parameter \
    --name ThePassword \
    --with-decryption \
    --query 'Parameter.Value' \
    --output text \
    --region ${Region})
  echo "ThePassword: ${thePassword}"

  testFunctionName=$(get_stack_output ${StackName} "TestFunctionName")
  aws lambda invoke \
    --function-name ${testFunctionName} \
    1.txt \
    --region ${Region}
  cat 1.txt
  rm 1.txt

elif [[ "${command}" == "" ]]; then
  echo "No command specified"
fi
