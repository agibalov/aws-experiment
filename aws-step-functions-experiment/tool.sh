Region=us-east-1
StackName=aws-step-functions-experiment

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
    --template-file template.yml \
    --stack-name ${StackName} \
    --capabilities CAPABILITY_NAMED_IAM \
    --region ${Region}
elif [[ "${command}" == "undeploy" ]]; then
  undeploy_stack ${StackName}
elif [[ "${command}" == "submit" ]]; then
  stateMachineArn=$(get_stack_output ${StackName} "StateMachineArn")
  executionArn=$(aws stepfunctions start-execution \
    --state-machine-arn ${stateMachineArn} \
    --input {\"a\":\"2\"\,\"b\":\"3\"} \
    --region ${Region} \
    --output text \
    --query 'executionArn')
  echo "executionArn: ${executionArn}"

  while true
  do
    status=$(aws stepfunctions describe-execution \
      --execution-arn ${executionArn} \
      --region ${Region} \
      --output text \
      --query 'status')
    echo "Status: ${status}"

    if [[ "${status}" != "RUNNING" ]]; then
      break
    fi

    sleep 1
  done

  aws stepfunctions get-execution-history \
    --execution-arn ${executionArn} \
    --region ${Region}

  output=$(aws stepfunctions describe-execution \
    --execution-arn ${executionArn} \
    --region ${Region} \
    --output text \
    --query 'output')
  echo "Output: ${output}"

elif [[ "${command}" == "" ]]; then
  echo "No command specified"
else
  echo "Unknown command: ${command}"
fi
