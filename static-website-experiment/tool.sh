#!/bin/bash

region=us-east-1
stackName=dummy-stack1
bucketName=loki2302-dummy-bucket1

command=$1

get_stack_output() {
  stackName=$1
  outputName=$2
  aws cloudformation describe-stacks \
    --stack-name ${stackName} \
    --query 'Stacks[0].Outputs[?OutputKey==`'${outputName}'`].OutputValue' \
    --output text \
    --region ${region}
}

if [ "$command" == "" ]; then
  echo "No command specified"
elif [ "$command" == "deploy-basic" ]; then
  echo "DEPLOYING"

  aws cloudformation deploy \
    --template-file basic.yml \
    --stack-name ${stackName} \
    --capabilities CAPABILITY_IAM \
    --region ${region} \
    --parameter-overrides \
    MyBucketName=$bucketName

  aws s3 cp public s3://$bucketName/ \
    --recursive \
    --acl public-read

elif [ "$command" == "deploy-spa" ]; then
  echo "DEPLOYING"

  aws cloudformation deploy \
    --template-file spa.yml \
    --stack-name ${stackName} \
    --capabilities CAPABILITY_IAM \
    --region ${region} \
    --parameter-overrides \
    MyBucketName=$bucketName

  aws s3 cp public s3://$bucketName/ \
    --recursive \
    --acl public-read

elif [ "$command" == "deploy-restricted" ]; then
  echo "DEPLOYING"

  aws cloudformation deploy \
    --template-file restricted.yml \
    --stack-name ${stackName} \
    --capabilities CAPABILITY_IAM \
    --region ${region} \
    --parameter-overrides \
    MyBucketName=$bucketName

  aws s3 cp public s3://$bucketName/ \
    --recursive

elif [ "$command" == "undeploy" ]; then
  echo "UNDEPLOYING"

  aws s3 rm s3://$bucketName/ \
    --recursive

  aws cloudformation delete-stack \
    --stack-name $stackName \
    --region $region

  aws cloudformation wait stack-delete-complete \
    --stack-name $stackName \
    --region $region

elif [ "$command" == "test" ]; then
  echo "TESTING"

  url=$(get_stack_output ${stackName} "WebsiteURL")
  echo "The page is right here: $url"

  echo
  echo "curl"
  curl $url

  echo
  echo "curl with user-agent"
  curl -A "hello there secret123 test" $url

else
  echo "Unknown command '$command'"
fi
