Region=us-east-1
StackName=vpc-experiment
KeyName=vpc-experiment-key
KeyFileName=key.pem

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
  if [[ ! -f ${KeyFileName} ]]; then
    aws ec2 create-key-pair \
      --key-name ${KeyName} \
      --region ${Region} \
      --output text \
      --query 'KeyMaterial' > ${KeyFileName}
    chmod 400 ${KeyFileName}
  fi

  setup=$2
  if [[ "${setup}" == "" ]]; then
    echo "Setup is not specified"
    exit 1
  fi

  templateFileName=${setup}.yml

  aws cloudformation deploy \
    --template-file ${templateFileName} \
    --stack-name ${StackName} \
    --capabilities CAPABILITY_NAMED_IAM \
    --region ${Region} \
    --parameter-overrides \
    KeyName=${KeyName}

  instanceAPublicIp=$(get_stack_output ${StackName} "InstanceAPublicIp")
  scp -i ${KeyFileName} ${KeyFileName} ec2-user@${instanceAPublicIp}:/home/ec2-user

elif [[ "${command}" == "undeploy" ]]; then
  undeploy_stack ${StackName}

  aws ec2 delete-key-pair \
    --key-name ${KeyName} \
    --region ${Region}
  rm -f ${KeyFileName}

elif [[ "${command}" == "ssh-to-instance-a" ]]; then
  instanceAPublicIp=$(get_stack_output ${StackName} "InstanceAPublicIp")
  ssh \
    -o ConnectTimeout=3 \
    -o ConnectionAttempts=1 \
    -i ${KeyFileName} \
    ec2-user@${instanceAPublicIp}

elif [[ "${command}" == "" ]]; then
  echo "No command specified"
else
  echo "Unknown command: ${command}"
fi
