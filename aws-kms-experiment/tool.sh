#!/bin/bash

set -x

Region=us-east-1
DeploymentBucketName=random-bucket-name-22312
StackName=kms

command=$1

if [[ "${command}" == "deploy" ]]; then
  aws cloudformation deploy \
    --template-file cloudformation/template.yml \
    --stack-name ${StackName} \
    --capabilities CAPABILITY_NAMED_IAM \
    --region ${Region}

elif [[ "${command}" == "undeploy" ]]; then
  aws cloudformation delete-stack \
    --stack-name ${StackName} \
    --region ${Region}

  aws cloudformation wait stack-delete-complete \
    --stack-name ${StackName} \
    --region ${Region}

  aws s3 rm s3://${DeploymentBucketName} \
    --recursive \
    --region ${Region}

  aws s3 rb s3://${DeploymentBucketName} \
    --region ${Region}

elif [[ "${command}" == "" ]]; then
  echo "No command specified"
fi
