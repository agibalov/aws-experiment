Region=us-east-1
StackName=codebuild-experiment

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

if [[ "${command}" == "deploy" ]]; then
  branch=${2:?not set or empty}
  aws cloudformation deploy \
    --template-file template.yml \
    --stack-name ${StackName} \
    --capabilities CAPABILITY_NAMED_IAM \
    --no-fail-on-empty-changeset \
    --region ${Region} \
    --parameter-overrides \
    BranchName=${branch}
elif [[ "${command}" == "undeploy" ]]; then
  undeploy_stack ${StackName}
elif [[ "${command}" == "" ]]; then
  echo "No command specified"
else
  echo "Unknown command: ${command}"
fi
