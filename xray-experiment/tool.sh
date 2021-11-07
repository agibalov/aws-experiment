#!/bin/bash

set -x

Region=us-east-1

command=$1

SharedStackName=xray-shared
AppStackName=xray-app

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

delete_ecr_images() {
  local repositoryName=$1
  local imageIds=$(aws ecr list-images \
    --repository-name ${repositoryName} \
    --query 'imageIds[].[imageDigest]' \
    --output=text \
    --region ${Region} | sed -E 's/(.+)/imageDigest=\1/')

  if [[ ! -z "$imageIds" ]]; then
    aws ecr batch-delete-image \
      --repository-name ${repositoryName} \
      --image-ids ${imageIds} \
      --region ${Region}
  fi
}

if [[ "${command}" == "deploy-shared" ]]; then
  aws cloudformation deploy \
    --template-file cloudformation/shared.yml \
    --stack-name ${SharedStackName} \
    --capabilities CAPABILITY_NAMED_IAM \
    --region ${Region}

elif [[ "${command}" == "undeploy-shared" ]]; then
  appRepositoryName=$(get_stack_output ${SharedStackName} "AppRepositoryName")
  delete_ecr_images ${appRepositoryName}

  undeploy_stack ${SharedStackName}

elif [[ "${command}" == "deploy-app" ]]; then
  appRepositoryUrl=$(get_stack_output ${SharedStackName} "AppRepositoryUrl")
  aws ecr get-login-password --region ${Region} | docker login \
    --username AWS \
    --password-stdin ${appRepositoryUrl}

  tag=build-$(uuidgen | tail -c 8)
  appImage="${appRepositoryUrl}:${tag}"
  docker build --tag ${appImage} .
  if [[ $? -ne 0 ]]; then
    echo "Failed to build Docker image"
    exit 1
  fi

  docker push ${appImage}
  if [[ $? -ne 0 ]]; then
    echo "Failed to push Docker image"
    exit 1
  fi

  aws cloudformation deploy \
    --template-file cloudformation/app.yml \
    --stack-name ${AppStackName} \
    --capabilities CAPABILITY_NAMED_IAM \
    --region ${Region} \
    --parameter-overrides \
    SharedStackName=${SharedStackName} \
    AppImage=${appImage}

  if [[ $? -ne 0 ]]; then
    echo "Failed to deploy ${AppStackName}"
    exit 1
  fi

elif [[ "${command}" == "undeploy-app" ]]; then
  undeploy_stack ${AppStackName}

elif [[ "${command}" == "" ]]; then
  echo "No command specified"
else
  echo "Unknown command ${command}"
fi
