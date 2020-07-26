#!/bin/bash

set -x

Region=us-east-1
TerraformModulesPath=terraform
StateBucketName=tf-2vrqm3469v4me9izzzku

command=$1

activate_environment() {
  local stateKey=$1
  local modulePath=$2

  terraform init \
    -reconfigure \
    -backend-config="bucket=${StateBucketName}" \
    -backend-config="key=${stateKey}" \
    -backend-config="region=${Region}" \
    ${modulePath}
}

delete_environment() {
  local stateKey=$1
  aws s3 rm s3://${StateBucketName}/${stateKey}
}

terraform_command_from_command() {
  local command=$1
  if [[ "${command}" == deploy-* ]]; then
    echo "apply"
  elif [[ "${command}" == undeploy-* ]]; then
    echo "destroy"
  else
    exit 1
  fi
}

get_dns_state_key() {
  echo "dns"
}

if [[ "${command}" == "init" ]]; then
  aws s3api create-bucket --bucket ${StateBucketName} --region ${Region}

elif [[ "${command}" == "deinit" ]]; then
  aws s3 rm s3://${StateBucketName} --recursive
  aws s3api delete-bucket --bucket ${StateBucketName}

elif [[ "${command}" == *-dns ]]; then
  stateKey=$(get_dns_state_key)
  activate_environment ${stateKey} ${TerraformModulesPath}/dns

  terraformCommand=$(terraform_command_from_command ${command})

  AWS_REGION=${Region} \
  terraform ${terraformCommand} -auto-approve ${TerraformModulesPath}/dns

  if [[ "${command}" == undeploy-* ]]; then
    delete_environment ${stateKey}
  fi

elif [[ "${command}" == "" ]]; then
  echo "No command specified"
else
  echo "Unknown command ${command}"
fi
