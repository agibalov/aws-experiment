#!/bin/bash

region=us-west-1
stackName=dummy-stack
bucketName=loki2302-dummy-bucket

command=$1

if [ "$command" == "" ]; then
  echo "No command specified"
elif [ "$command" == "deploy" ]; then
  echo "DEPLOYING"

  aws cloudformation create-stack \
    --stack-name $stackName \
    --template-body file://dummy.template \
    --region $region \
    --capabilities CAPABILITY_IAM \
    --parameters ParameterKey=MyBucketName,ParameterValue=$bucketName

  aws cloudformation wait stack-create-complete \
    --stack-name $stackName \
    --region $region

  aws s3 cp public s3://$bucketName/ \
    --recursive \
    --acl public-read

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

  url="http://$bucketName.s3-website-$region.amazonaws.com"
  echo "The page is right here: $url"
  curl $url

else
  echo "Unknown command '$command'"
fi
