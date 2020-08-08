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

get_shared_state_key() {
  local sharedEnvTag=$1
  echo "${sharedEnvTag}-shared"
}

get_app_state_key() {
  local appEnvTag=$1
  echo "${appEnvTag}-app"
}

get_pipeline_state_key() {
  local appEnvTag=$1
  echo "${appEnvTag}-pipeline"
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

elif [[ "${command}" == *-shared ]]; then
  sharedEnvTag=${sharedEnvTag:?not set or empty}

  sharedStateKey=$(get_shared_state_key ${sharedEnvTag})
  activate_environment ${sharedStateKey} ${TerraformModulesPath}/shared

  terraformCommand=$(terraform_command_from_command ${command})

  AWS_REGION=${Region} \
  TF_VAR_shared_env_tag=${sharedEnvTag} \
  terraform ${terraformCommand} -auto-approve ${TerraformModulesPath}/shared

  if [[ "${command}" == undeploy-* ]]; then
    delete_environment ${sharedStateKey}
  fi

elif [[ "${command}" == *-app ]]; then
  sharedEnvTag=${sharedEnvTag:?not set or empty}
  appEnvTag=${appEnvTag:?not set or empty}

  dnsStateKey=$(get_dns_state_key)
  sharedStateKey=$(get_shared_state_key ${sharedEnvTag})
  appStateKey=$(get_app_state_key ${appEnvTag})
  activate_environment ${appStateKey} ${TerraformModulesPath}/app

  terraformCommand=$(terraform_command_from_command ${command})

  AWS_REGION=${Region} \
  TF_VAR_state_bucket_name=${StateBucketName} \
  TF_VAR_dns_state_key=${dnsStateKey} \
  TF_VAR_shared_state_key=${sharedStateKey} \
  TF_VAR_app_env_tag=${appEnvTag} \
  terraform ${terraformCommand} -auto-approve ${TerraformModulesPath}/app

  if [[ "${command}" == undeploy-* ]]; then
    delete_environment ${appStateKey}
  fi

elif [[ "${command}" == *-pipeline ]]; then
  sharedEnvTag=${sharedEnvTag:?not set or empty}
  appEnvTag=${appEnvTag:?not set or empty}
  branch=${branch:?not set or empty}

  pipelineStateKey=$(get_pipeline_state_key ${appEnvTag})
  activate_environment ${pipelineStateKey} ${TerraformModulesPath}/pipeline

  terraformCommand=$(terraform_command_from_command ${command})

  AWS_REGION=${Region} \
  TF_VAR_shared_env_tag=${sharedEnvTag} \
  TF_VAR_app_env_tag=${appEnvTag} \
  TF_VAR_branch_name=${branch} \
  terraform ${terraformCommand} -auto-approve ${TerraformModulesPath}/pipeline

  if [[ "${command}" == undeploy-* ]]; then
    delete_environment ${pipelineStateKey}
  fi

elif [[ "${command}" == "" ]]; then
  echo "No command specified"
else
  echo "Unknown command ${command}"
fi
