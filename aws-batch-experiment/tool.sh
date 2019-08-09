Region=us-east-1
StackName=aws-batch-experiment

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
  jobQueueArn=$(get_stack_output ${StackName} "JobQueueArn")
  jobDefinitionArn=$(get_stack_output ${StackName} "JobDefinitionArn")

  jobId=$(aws batch submit-job \
    --job-name "dummy-job" \
    --job-queue ${jobQueueArn} \
    --job-definition ${jobDefinitionArn} \
    --region ${Region} \
    --output text \
    --query 'jobId')
  echo "job ID: ${jobId}"

  while true
  do
    status=$(aws batch describe-jobs \
      --jobs ${jobId} \
      --region ${Region} \
      --output text \
      --query 'jobs[0].status')
    echo "Status: ${status}"

    if [[ "${status}" == "SUCCEEDED" ]] || [[ "${status}" == "FAILED" ]]; then
      break
    fi

    sleep 1
  done

  echo "done"
elif [[ "${command}" == "" ]]; then
  echo "No command specified"
else
  echo "Unknown command: ${command}"
fi
