#!/bin/bash

set -x

Region=us-east-1
SshPrivateKeyFileName=ssh-key
SshPublicKeyFileName=${SshPrivateKeyFileName}.pub

command=$1

get_stack_name() {
  echo "transfer-family-experiment"
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

if [[ "${command}" == "generate-key" ]]; then
  ssh-keygen -t rsa -b 4096 -N '' -f ${SshPrivateKeyFileName}

elif [[ "${command}" == "deploy-basic" ]]; then
  stackName=$(get_stack_name)
  aws cloudformation deploy \
    --template-file template-basic.yml \
    --stack-name ${stackName} \
    --capabilities CAPABILITY_NAMED_IAM \
    --region ${Region} \
    --parameter-overrides \
    SshPublicKey="$(cat ${SshPublicKeyFileName})"

elif [[ "${command}" == "deploy-custom-auth" ]]; then
  stackName=$(get_stack_name)
  aws cloudformation deploy \
    --template-file template-custom-auth.yml \
    --stack-name ${stackName} \
    --capabilities CAPABILITY_NAMED_IAM \
    --region ${Region} \
    --parameter-overrides \
    User1SshPublicKey="$(cat ${SshPublicKeyFileName})" \
    User2SshPublicKey="$(cat ${SshPublicKeyFileName})"

  restApiId=$(get_stack_output ${stackName} "RestApiId")
  restApiStageName=$(get_stack_output ${stackName} "RestApiStageName")
  aws apigateway create-deployment \
    --rest-api-id ${restApiId} \
    --stage-name ${restApiStageName} \
    --region ${Region}

elif [[ "${command}" == "undeploy" ]]; then
  stackName=$(get_stack_name)

  bucketName=$(get_stack_output ${stackName} "BucketName")
  aws s3 rm s3://${bucketName} --recursive

  undeploy_stack ${stackName}

elif [[ "${command}" == "sftp-key" ]]; then
  userName=${2:?not set or empty}
  stackName=$(get_stack_name)
  serverHost=$(get_stack_output ${stackName} "ServerHost")

  sftp -i ${SshPrivateKeyFileName} ${userName}@${serverHost}

elif [[ "${command}" == "sftp-password" ]]; then
  userName=${2:?not set or empty}
  stackName=$(get_stack_name)
  serverHost=$(get_stack_output ${stackName} "ServerHost")

  sftp ${userName}@${serverHost}

elif [[ "${command}" == "" ]]; then
  echo "No command specified"
else
  echo "Unknown command ${command}"
fi
