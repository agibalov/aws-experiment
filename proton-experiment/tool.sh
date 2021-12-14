#!/bin/bash

set -x

Region=us-east-1
BaseStackName=proton-experiment-base
DummyTemplateName=template1

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

if [[ "${command}" == "deploy-base" ]]; then
  aws cloudformation deploy \
    --template-file base.yaml \
    --stack-name ${BaseStackName} \
    --capabilities CAPABILITY_NAMED_IAM \
    --region ${Region}

elif [[ "${command}" == "undeploy-base" ]]; then
  undeploy_stack ${BaseStackName}

elif [[ "${command}" == "create-environment-template" ]]; then
  aws proton create-environment-template \
    --name ${DummyTemplateName} \
    --region ${Region}

elif [[ "${command}" == "delete-environment-template" ]]; then
  aws proton create-environment-template \
    --name ${DummyTemplateName} \
    --region ${Region}

elif [[ "${command}" == "create-environment-template-version" ]]; then
  timestamp=$(date '+%s')
  bundleFilename=${timestamp}.tar.gz
  tar -zcvf ${bundleFilename} environment/

  templatesBucketName=$(get_stack_output "${BaseStackName}" "BucketName")
  aws s3 cp ${bundleFilename} s3://${templatesBucketName}/${bundleFilename}
  aws proton create-environment-template-version \
    --template-name ${DummyTemplateName} \
    --source s3=\{bucket=${templatesBucketName},key=${bundleFilename}\} \
    --region ${Region}

  rm ${bundleFilename}

elif [[ "${command}" == "create-environment" ]]; then
  envName=${envName:?not set or empty}
  version=${version:?not set or empty}
  protonServiceRoleArn=$(get_stack_output "${BaseStackName}" "ProtonServiceRoleArn")
  aws proton create-environment \
    --name ${envName} \
    --spec "{ proton: EnvironmentSpec, spec: { env_name: ${envName} } }" \
    --template-major-version ${version} \
    --template-name ${DummyTemplateName} \
    --proton-service-role-arn ${protonServiceRoleArn} \
    --region ${Region}

  aws proton wait environment-deployed \
    --name ${envName} \
    --region ${Region}
  aws proton get-environment \
    --name ${envName} \
    --region ${Region}

elif [[ "${command}" == "delete-environment" ]]; then
  envName=${envName:?not set or empty}
  aws proton delete-environment \
    --name ${envName} \
    --region ${Region}

elif [[ "${command}" == "" ]]; then
  echo "No command specified"
else
  echo "Unknown command ${command}"
fi
