#!/bin/bash

region=us-east-1
stackName=AwsCiExperimentStack
templateBodyFilename=pipeline.yaml

appS3BucketName=loki2302-s3-bucket
dummyCodePipelinePipelineS3BucketName=dummy-codepipeline-pipeline-s3-bucket

command=$1

if [ "$command" == "" ]; then
  echo "No command specified"
elif [ "$command" == "create" ]; then
  gitHubOAuthToken=$2
  if [ "$gitHubOAuthToken" == "" ]; then
    echo "Please specify GitHub OAuth token"
  else
    echo "CREATING"

    aws cloudformation create-stack \
      --stack-name $stackName \
      --template-body file://$templateBodyFilename \
      --parameters \
      ParameterKey=DummyGitHubOAuthToken,ParameterValue=$gitHubOAuthToken \
      ParameterKey=AppS3BucketName,ParameterValue=$appS3BucketName \
      ParameterKey=DummyCodePipelinePipelineS3BucketName,ParameterValue=$dummyCodePipelinePipelineS3BucketName \
      --region $region \
      --capabilities CAPABILITY_NAMED_IAM

    aws cloudformation wait stack-create-complete \
      --stack-name $stackName \
      --region $region
  fi

elif [ "$command" == "update" ]; then
  gitHubOAuthToken=$2
  if [ "$gitHubOAuthToken" == "" ]; then
    echo "Please specify GitHub OAuth token"
  else
    echo "UPDATING"

    aws cloudformation update-stack \
      --stack-name $stackName \
      --template-body file://$templateBodyFilename \
      --parameters \
      --parameters \
      ParameterKey=DummyGitHubOAuthToken,ParameterValue=$gitHubOAuthToken \
      ParameterKey=AppS3BucketName,ParameterValue=$appS3BucketName \
      ParameterKey=DummyCodePipelinePipelineS3BucketName,ParameterValue=$dummyCodePipelinePipelineS3BucketName \
      --capabilities CAPABILITY_NAMED_IAM \
      --region $region

    aws cloudformation wait stack-update-complete \
      --stack-name $stackName \
      --region $region
  fi

elif [ "$command" == "delete" ]; then
  echo "DELETING"

  # CloudFormation can only delete empty S3 buckets
  aws s3 rm s3://$appS3BucketName/ --recursive
  aws s3 rm s3://$dummyCodePipelinePipelineS3BucketName/ --recursive

  # CloudFormation will fail to delete the stack,
  # because its default role is going to be deleted at some point
  # during the stack destruction.
  # To avoid this, I temporarily create a new admin role
  # and use it to delete the stack
  adminRoleName=DummyAdminRole
  adminAccessPolicyArn=arn:aws:iam::aws:policy/AdministratorAccess

  aws iam create-role \
    --role-name $adminRoleName \
    --assume-role-policy-document file://cloudformation-assume.json

  aws iam attach-role-policy \
    --role-name $adminRoleName \
    --policy-arn "$adminAccessPolicyArn"

  roleArn=`\
  aws iam get-role --role-name $adminRoleName | \
  python -c "import json,sys;print json.load(sys.stdin)['Role']['Arn']"`

  echo "Temporary admin role ARN is $roleArn"

  # Looks like create-role and attach-role-policy operations do something
  # after finishing. Give them 10 seconds to actually finish whatever
  # they do there.
  sleep 10

  aws cloudformation delete-stack \
    --role-arn "$roleArn" \
    --stack-name $stackName \
    --region $region

  aws cloudformation wait stack-delete-complete \
    --stack-name $stackName \
    --region $region

  aws iam detach-role-policy \
    --role-name $adminRoleName \
    --policy-arn "$adminAccessPolicyArn"

  aws iam delete-role \
    --role-name $adminRoleName

else
  echo "Unknown command '$command'"
fi
