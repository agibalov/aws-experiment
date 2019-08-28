Region=us-east-1
StackName=aws-redshift-experiment

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

  bucketName=$(get_stack_output ${StackName} 'BucketName')
  aws s3 cp employees.csv s3://${bucketName}
elif [[ "${command}" == "undeploy" ]]; then
  bucketName=$(get_stack_output ${StackName} 'BucketName')
  aws s3 rm s3://${bucketName}/ --recursive

  undeploy_stack ${StackName}
elif [[ "${command}" == "test" ]]; then
  bucketName=$(get_stack_output ${StackName} 'BucketName')

  REDSHIFT_HOST=$(get_stack_output ${StackName} 'EndpointAddress') \
  REDSHIFT_PORT=$(get_stack_output ${StackName} 'EndpointPort') \
  REDSHIFT_DATABASE=$(get_stack_output ${StackName} 'DbName') \
  REDSHIFT_USERNAME=$(get_stack_output ${StackName} 'MasterUsername') \
  REDSHIFT_PASSWORD=$(get_stack_output ${StackName} 'MasterUserPassword') \
  IMPORT_CSV_FILE_S3_URL=s3://${bucketName}/employees.csv \
  EXPORT_CSV_S3_PREFIX=s3://${bucketName}/average- \
  IMPORT_IAM_ROLE_ARN=$(get_stack_output ${StackName} 'RoleArn') \
  IMPORT_REGION=${Region} \
  ./gradlew clean test
elif [[ "${command}" == "" ]]; then
  echo "No command specified"
else
  echo "Unknown command: ${command}"
fi
