Region=us-east-1
DeploymentBucketName=cf-custom-resource-deployment
BucketFileResourceStackName=bucket-file-resource
DemoStackName=demo

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

if [[ "${command}" == "deploy-bucket-file-resource" ]]; then
  ./gradlew clean buildZip

  aws s3 mb s3://${DeploymentBucketName} \
    --region ${Region}

  aws cloudformation package \
    --template-file cloudformation/bucket-file-resource.yml \
    --s3-bucket ${DeploymentBucketName} \
    --output-template-file bucket-file-resource.packaged.yml

  aws cloudformation deploy \
    --template-file bucket-file-resource.packaged.yml \
    --stack-name ${BucketFileResourceStackName} \
    --capabilities CAPABILITY_NAMED_IAM \
    --region ${Region}
elif [[ "${command}" == "undeploy-bucket-file-resource" ]]; then
  undeploy_stack ${BucketFileResourceStackName}
  aws s3 rm s3://${DeploymentBucketName}/ --recursive
  aws s3 rb s3://${DeploymentBucketName} --force
elif [[ "${command}" == "deploy-demo" ]]; then
  bucketFileResourceServiceToken=$(get_stack_output ${BucketFileResourceStackName} "BucketFileResourceServiceToken")
  aws cloudformation deploy \
    --template-file cloudformation/demo.yml \
    --stack-name ${DemoStackName} \
    --capabilities CAPABILITY_NAMED_IAM \
    --region ${Region} \
    --parameter-overrides \
    BucketFileResourceServiceToken=${bucketFileResourceServiceToken}
elif [[ "${command}" == "undeploy-demo" ]]; then
  undeploy_stack ${DemoStackName}
elif [[ "${command}" == "" ]]; then
  echo "No command specified"
else
  echo "Unknown command: ${command}"
fi
