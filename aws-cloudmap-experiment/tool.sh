Region=us-east-1
StackName=aws-cloudmap-experiment

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
  templateName=$2
  aws cloudformation deploy \
    --template-file $2.yml \
    --stack-name ${StackName} \
    --capabilities CAPABILITY_NAMED_IAM \
    --region ${Region}
elif [[ "${command}" == "undeploy" ]]; then
  undeploy_stack ${StackName}
elif [[ "${command}" == "register-instance" ]]; then
  serviceId=$(get_stack_output ${StackName} "ServiceId")
  aws servicediscovery register-instance \
    --service-id ${serviceId} \
    --instance-id instance1 \
    --attributes \
    AWS_INSTANCE_IPV4=1.2.3.4,AWS_INSTANCE_PORT=4321 \
    --region ${Region}
elif [[ "${command}" == "deregister-instance" ]]; then
  serviceId=$(get_stack_output ${StackName} "ServiceId")
  aws servicediscovery deregister-instance \
    --service-id ${serviceId} \
    --instance-id instance1 \
    --region ${Region}
elif [[ "${command}" == "discover-instances" ]]; then
  namespaceName=$(get_stack_output ${StackName} "NamespaceName")
  serviceName=$(get_stack_output ${StackName} "ServiceName")
  aws servicediscovery discover-instances \
    --namespace-name ${namespaceName} \
    --service-name ${serviceName} \
    --region ${Region}
elif [[ "${command}" == "" ]]; then
  echo "No command specified"
else
  echo "Unknown command: ${command}"
fi
