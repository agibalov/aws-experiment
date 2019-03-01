#!/bin/bash

set -x

Region=us-east-1

command=$1
envTag=$2

if [[ "${envTag}" == "" ]]; then
  echo "envTag is not specified"
  exit 1
fi

PipelineStackName=${envTag}-pipeline
AppStackName=${envTag}-app

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

if [[ "${command}" == "deploy-pipeline" ]]; then
  gitHubRepositoryOwner=$3
  gitHubRepositoryName=$4
  gitHubRepositoryBranchName=$5
  gitHubRepositoryOAuthToken=$6

  if [[ "${gitHubRepositoryOAuthToken}" == "" ]]; then
    gitHubRepositoryOAuthToken=$(get_stack_parameter ${PipelineStackName} "GitHubRepositoryOAuthToken")
  fi

  if [[ "${gitHubRepositoryOAuthToken}" == "" ]]; then
    echo "gitHubRepositoryOAuthToken is not set"
    exit 1
  fi

  aws cloudformation deploy \
    --template-file pipeline.yml \
    --stack-name ${PipelineStackName} \
    --capabilities CAPABILITY_NAMED_IAM \
    --parameter-overrides \
    EnvTag=${envTag} \
    GitHubRepositoryOwnerName=${gitHubRepositoryOwner} \
    GitHubRepositoryName=${gitHubRepositoryName} \
    GitHubRepositoryBranchName=${gitHubRepositoryBranchName} \
    GitHubRepositoryOAuthToken=${gitHubRepositoryOAuthToken} \
    --region ${Region}

elif [[ "${command}" == "undeploy-pipeline" ]]; then
  pipelineArtifactStoreBucketName=$(get_stack_output ${PipelineStackName} "PipelineArtifactStoreBucketName")
  aws s3 rm s3://${pipelineArtifactStoreBucketName}/ --recursive

  undeploy_stack ${PipelineStackName}

elif [[ "${command}" == "start-pipeline" ]]; then
  pipelineName=$(get_stack_output ${PipelineStackName} "PipelineName")
  aws codepipeline start-pipeline-execution \
    --name ${pipelineName} \
    --region ${Region}

elif [[ "${command}" == "deploy-app" ]]; then
  aws cloudformation deploy \
    --template-file app.yml \
    --stack-name ${AppStackName} \
    --capabilities CAPABILITY_NAMED_IAM \
    --parameter-overrides \
    EnvTag=${envTag} \
    --region ${Region}

elif [[ "${command}" == "undeploy-app" ]]; then
  undeploy_stack ${AppStackName}

elif [[ "${command}" == "" ]]; then
  echo "No command specified"

fi
