#!/bin/bash

region=us-west-1
stackName=dummy-stack
codeBucketName=loki2302-code-bucket

command=$1

if [ "$command" == "" ]; then
  echo "No command specified"
elif [ "$command" == "deploy" ]; then
  echo "DEPLOYING"

  aws s3 mb s3://$codeBucketName --region $region

  aws s3 cp ./build/libs/aws-beanstalk-experiment-1.0-SNAPSHOT.war s3://$codeBucketName/ --acl public-read

  aws cloudformation create-stack \
    --stack-name $stackName \
    --template-body file://dummy.yaml \
    --region $region \
    --capabilities CAPABILITY_IAM

  aws cloudformation wait stack-create-complete \
    --stack-name $stackName \
    --region $region

elif [ "$command" == "undeploy" ]; then
  echo "UNDEPLOYING"

  aws cloudformation delete-stack \
    --stack-name $stackName \
    --region $region

  aws cloudformation wait stack-delete-complete \
    --stack-name $stackName \
    --region $region

  aws s3 rm s3://$codeBucketName/ --recursive

  aws s3 rb s3://$codeBucketName --force

elif [ "$command" == "test" ]; then
  echo "TESTING"

  # Python is used here to extract OutputValue from JSON provided by aws cloudformation
  # [
  #   {
  #     "OutputKey": "Url",
  #     "OutputValue": "awseb-e-k-AWSEBLoa-1V2C87OZUBMEA-2118969383.us-west-1.elb.amazonaws.com"
  #   }
  # ]

  url=http://`\
  aws cloudformation describe-stacks \
    --stack-name $stackName \
    --query Stacks[0].Outputs \
    --region $region | \
  python -c "import json,sys;print json.load(sys.stdin)[0]['OutputValue']
  "`

  echo "Testing GET $url"
  curl $url
  echo ""

else
  echo "Unknown command '$command'"
fi
