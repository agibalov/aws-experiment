#!/bin/bash

set -x

Region=us-east-1

command=$1

get_app_stack_name() {
  echo "codeartifact-experiment"
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

if [[ "${command}" == "deploy" ]]; then
  branch=${branch:?not set or empty}
  stackName=$(get_app_stack_name)
  aws cloudformation deploy \
    --template-file template.yml \
    --stack-name ${stackName} \
    --capabilities CAPABILITY_NAMED_IAM \
    --region ${Region} \
    --parameter-overrides \
    BranchName=${branch}

elif [[ "${command}" == "undeploy" ]]; then
  stackName=$(get_app_stack_name)
  undeploy_stack ${stackName}

#elif [[ "${command}" == "test-npm" ]]; then
#  stackName=$(get_app_stack_name)
#  domainName=$(get_stack_output ${stackName} "DomainName")
#  domainOwner=$(get_stack_output ${stackName} "DomainOwner")
#  repositoryName=$(get_stack_output ${stackName} "RepositoryName")
#
#  docker build --file npm-test.Dockerfile --tag test-npm .
#  docker run \
#    --env CODEARTIFACT_DOMAIN=${domainName} \
#    --env CODEARTIFACT_DOMAIN_OWNER=${domainOwner} \
#    --env CODEARTIFACT_REPOSITORY=${repositoryName} \
#    --env AWS_REGION=${Region} \
#    --rm \
#    test-npm

elif [[ "${command}" == "" ]]; then
  echo "No command specified"
else
  echo "Unknown command ${command}"
fi
