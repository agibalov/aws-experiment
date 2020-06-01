#!/bin/bash

set -x

Region=us-east-1

command=$1

function get_app_stack_name() {
  local envTag=$1
  echo "${envTag}-app"
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

  restApiId=$(get_stack_output ${appStackName} "RestApiId")
  restApiStageName=$(get_stack_output ${appStackName} "RestApiStageName")
  aws apigateway create-deployment \
    --rest-api-id ${restApiId} \
    --stage-name ${restApiStageName} \
    --region ${Region}

  accessibleBucketName=$(get_stack_output ${appStackName} "AccessibleBucketName")
  aws s3 sync content s3://${accessibleBucketName} --delete

  inaccessibleBucketName=$(get_stack_output ${appStackName} "InaccessibleBucketName")
  aws s3 sync content s3://${inaccessibleBucketName} --delete

elif [[ "${command}" == "undeploy" ]]; then
  envTag=${envTag:?not set or empty}
  appStackName=$(get_app_stack_name ${envTag})

  accessibleBucketName=$(get_stack_output ${appStackName} "AccessibleBucketName")
  aws s3 rm s3://${accessibleBucketName} --recursive

  inaccessibleBucketName=$(get_stack_output ${appStackName} "InaccessibleBucketName")
  aws s3 rm s3://${inaccessibleBucketName} --recursive

  undeploy_stack ${appStackName}

elif [[ "${command}" == "" ]]; then
  echo "No command specified"
else
  echo "Unknown command ${command}"
fi
