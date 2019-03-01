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
  gitHubOAuthToken=$6

  if [[ "${gitHubOAuthToken}" == "" ]]; then
    gitHubOAuthToken=$(get_stack_parameter ${PipelineStackName} "GitHubOAuthToken")
  fi

  if [[ "${gitHubOAuthToken}" == "" ]]; then
    echo "gitHubOAuthToken is not set"
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
    GitHubOAuthToken=${gitHubOAuthToken} \
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
  buildArtifactsDirectory=$3
  if [[ "${buildArtifactsDirectory}" == "" ]]; then
    buildArtifactsDirectory=.
  fi

  aws cloudformation deploy \
    --template-file app.yml \
    --stack-name ${AppStackName} \
    --capabilities CAPABILITY_NAMED_IAM \
    --parameter-overrides \
    EnvTag=${envTag} \
    --region ${Region}

  webSiteBucketName=$(get_stack_output ${AppStackName} "WebSiteBucketName")
  aws s3 cp ${buildArtifactsDirectory}/index.html s3://${webSiteBucketName} \
    --acl public-read

elif [[ "${command}" == "undeploy-app" ]]; then
  webSiteBucketName=$(get_stack_output ${AppStackName} "WebSiteBucketName")
  aws s3 rm s3://${webSiteBucketName}/ --recursive

  undeploy_stack ${AppStackName}

elif [[ "${command}" == "" ]]; then
  echo "No command specified"

fi
