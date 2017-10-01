#!/bin/bash

Region=us-east-1

WebsiteStackName=WebsiteStack
WebsiteBucketName=123413241-website-bucket

command=$1

get_stack_output() {
  stackName=$1
  outputName=$2
  aws cloudformation describe-stacks \
    --stack-name ${stackName} \
    --query 'Stacks[0].Outputs[?OutputKey==`'${outputName}'`].OutputValue' \
    --output text \
    --region ${Region}
}

if [ "${command}" == "deploy" ]; then
  aws cloudformation deploy \
    --template-file cloudformation/website.yml \
    --stack-name ${WebsiteStackName} \
    --capabilities CAPABILITY_IAM \
    --region ${Region} \
    --parameter-overrides \
    WebsiteBucketName=${WebsiteBucketName} \
    WebsiteApiStageName=DummyStage
  
  RESULT=$?
  if [ $RESULT != 0 ]; then
    echo "Failed to deploy the stack"
    exit 1
  fi

  aws s3 sync src s3://${WebsiteBucketName}
  
  RESULT=$?
  if [ $RESULT != 0 ]; then
    echo "Failed to sync files"
    exit 1
  fi

elif [ "${command}" == "undeploy" ]; then
  aws s3 rm s3://${WebsiteBucketName} --recursive

  RESULT=$?
  if [ $RESULT != 0 ]; then
    echo "Failed to remove all files from bucket"
    exit 1
  fi

  aws cloudformation delete-stack \
    --stack-name ${WebsiteStackName} \
    --region ${Region}

  RESULT=$?
  if [ $RESULT != 0 ]; then
    echo "Failed to delete the stack"
    exit 1
  fi

  aws cloudformation wait stack-delete-complete \
    --stack-name ${WebsiteStackName} \
    --region ${Region}
  
  RESULT=$?
  if [ $RESULT != 0 ]; then
    echo "Failed to wait for stack deletion to finish"
    exit 1
  fi

elif [ "${command}" == "test" ]; then
  websiteUrl=$(get_stack_output ${WebsiteStackName} "WebsiteUrl")
  curl ${websiteUrl}/index.html

  curl ${websiteUrl}/test.png -s -o 1.png
  actualPngHash=$(git hash-object 1.png)
  expectedPngHash=$(git hash-object src/test.png)
  echo "Actual PNG:   ${actualPngHash}"
  echo "Expected PNG: ${expectedPngHash}"

  curl ${websiteUrl}/test.jpg -s -o 1.jpg
  actualJpgHash=$(git hash-object 1.jpg)
  expectedJpgHash=$(git hash-object src/test.jpg)
  echo "Actual JPG:   ${actualJpgHash}"
  echo "Expected JPG: ${expectedJpgHash}"

elif [ "${command}" == "" ]; then
  echo "No command specified"
fi
