#!/bin/bash

region=us-east-1
stackName=AwsCiExperimentStack
templateBodyFilename=dummy-codebuild.yaml

command=$1

if [ "$command" == "" ]; then
  echo "No command specified"
elif [ "$command" == "create" ]; then
  echo "CREATING"

  aws cloudformation create-stack \
    --stack-name $stackName \
    --template-body file://$templateBodyFilename \
    --region $region \
    --capabilities CAPABILITY_NAMED_IAM

  aws cloudformation wait stack-create-complete \
    --stack-name $stackName \
    --region $region

elif [ "$command" == "update" ]; then
  echo "UPDATING"

  aws cloudformation update-stack \
    --stack-name $stackName \
    --template-body file://$templateBodyFilename \
    --capabilities CAPABILITY_NAMED_IAM

  aws cloudformation wait stack-update-complete \
    --stack-name $stackName

elif [ "$command" == "delete" ]; then
  echo "DELETING"

  aws cloudformation delete-stack \
    --stack-name $stackName \
    --region $region

  aws cloudformation wait stack-delete-complete \
    --stack-name $stackName \
    --region $region

else
  echo "Unknown command '$command'"
fi
