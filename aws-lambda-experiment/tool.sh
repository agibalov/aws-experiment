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

  aws s3 cp ./build/distributions/aws-lambda-experiment-1.0-SNAPSHOT.zip s3://$codeBucketName/ --acl public-read

  aws cloudformation create-stack \
    --stack-name $stackName \
    --template-body file://dummy.template \
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
  #    {
  #        "OutputValue": "tb4pzrxu02",
  #        "OutputKey": "ApiId"
  #    }
  # ]
  ApiId=`\
  aws cloudformation describe-stacks \
    --stack-name $stackName \
    --query Stacks[0].Outputs \
    --region $region | \
  python -c "import json,sys;print json.load(sys.stdin)[0]['OutputValue']
  "`
  
  declare -a resources=(
    "dummyJs"
    "dummyJava"
  )

  for resource in "${resources[@]}"
  do
    resourceUrl="https://$ApiId.execute-api.$region.amazonaws.com/prod/$resource"
    echo "Testing GET $resourceUrl"
    curl $resourceUrl
    echo ""
  done

else
  echo "Unknown command '$command'"
fi
