#!/bin/bash

set -x

Region=us-east-1
DeploymentBucketName=random-bucket-name-22312
StackName=kms

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
  aws cloudformation deploy \
    --template-file cloudformation/template.yml \
    --stack-name ${StackName} \
    --capabilities CAPABILITY_NAMED_IAM \
    --region ${Region}

elif [[ "${command}" == "undeploy" ]]; then
  undeploy_stack ${StackName}

  aws s3 rm s3://${DeploymentBucketName} \
    --recursive \
    --region ${Region}

  aws s3 rb s3://${DeploymentBucketName} \
    --region ${Region}

elif [[ "${command}" == "test" ]]; then
  keyId=$(get_stack_output ${StackName} "KmsKeyId")
  originalText="hello world!"
  blob=$(aws kms encrypt \
    --key-id ${keyId} \
    --plaintext "${originalText}" \
    --region ${Region} \
    --query CiphertextBlob \
    --output text)
  echo "blob: ${blob}"

  aws kms decrypt \
    --ciphertext-blob fileb://<(echo ${blob} | base64 -d) \
    --region ${Region}

elif [[ "${command}" == "" ]]; then
  echo "No command specified"
fi
