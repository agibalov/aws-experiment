#!/bin/bash

set -x

Region=us-east-1
TerraformModulesPath=terraform
StateBucketName=tf-rtert3443r

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

get_layer1_state_key() {
  echo "layer1"
}

get_layer2_state_key() {
  echo "layer2"
}

get_layer3_state_key() {
  local envTag=?1
  echo "${envTag}-layer3"
}

if [[ "${command}" == "init" ]]; then
  aws s3api create-bucket --bucket ${StateBucketName} --region ${Region}

elif [[ "${command}" == "deinit" ]]; then
  aws s3 rm s3://${StateBucketName} --recursive
  aws s3api delete-bucket --bucket ${StateBucketName}

elif [[ "${command}" == *-layer1 ]]; then
  stateKey=$(get_layer1_state_key)
  activate_environment ${stateKey} ${TerraformModulesPath}/layer1

  terraformCommand=$(terraform_command_from_command ${command})

  AWS_REGION=${Region} \
  terraform ${terraformCommand} -auto-approve ${TerraformModulesPath}/layer1

  if [[ "${command}" == undeploy-* ]]; then
    delete_environment ${stateKey}
  fi

elif [[ "${command}" == *-layer2 ]]; then
  stateKey=$(get_layer2_state_key)
  activate_environment ${stateKey} ${TerraformModulesPath}/layer2

  terraformCommand=$(terraform_command_from_command ${command})

  AWS_REGION=${Region} \
  TF_VAR_state_bucket=${StateBucketName} \
  TF_VAR_layer1_state_key=$(get_layer1_state_key) \
  terraform ${terraformCommand} -auto-approve ${TerraformModulesPath}/layer2

  if [[ "${command}" == undeploy-* ]]; then
    delete_environment ${stateKey}
  fi

elif [[ "${command}" == *-layer3 ]]; then
  envTag=${envTag:?not set or empty}
  stateKey=$(get_layer3_state_key ${envTag})
  activate_environment ${stateKey} ${TerraformModulesPath}/layer3

  terraformCommand=$(terraform_command_from_command ${command})

  AWS_REGION=${Region} \
  TF_VAR_env_tag=${envTag} \
  TF_VAR_state_bucket=${StateBucketName} \
  TF_VAR_layer1_state_key=$(get_layer1_state_key) \
  TF_VAR_layer2_state_key=$(get_layer2_state_key) \
  terraform ${terraformCommand} -auto-approve ${TerraformModulesPath}/layer3

  if [[ "${command}" == undeploy-* ]]; then
    delete_environment ${stateKey}
  fi

elif [[ "${command}" == "" ]]; then
  echo "No command specified"
else
  echo "Unknown command ${command}"
fi
