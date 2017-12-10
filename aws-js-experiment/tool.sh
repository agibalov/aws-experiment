#!/bin/bash

region=us-east-1
stackName=AwsJsExperimentStack
websiteBucketName=aws-js-experiment-website
dataBucketName=aws-js-experiment-data

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
elif [ "$command" == "deploy" ]; then
  echo "DEPLOYING"

  aws cloudformation deploy \
    --template-file cf.yml \
    --stack-name ${stackName} \
    --capabilities CAPABILITY_NAMED_IAM \
    --region ${region} \
    --parameter-overrides \
    WebsiteBucketName=${websiteBucketName} \
    DataBucketName=${dataBucketName}

  aws s3 cp index.html s3://${websiteBucketName}/ --acl public-read

  awsKey=$(get_stack_output ${stackName} 'DummyUserAccessKeyId')
  awsSecret=$(get_stack_output ${stackName} 'DummyUserSecretAccessKey')

  jq -n \
    --arg awsKey ${awsKey} \
    --arg awsSecret ${awsSecret} \
    --arg s3Bucket ${dataBucketName} \
    --arg s3Key "hello.txt" \
    '{
      "awsKey": $awsKey,
      "awsSecret": $awsSecret,
      "s3Bucket": $s3Bucket,
      "s3Key": $s3Key
    }' > config.json

  aws s3 cp config.json s3://${websiteBucketName}/ --acl public-read

  websiteUrl=$(get_stack_output ${stackName} 'WebsiteURL')
  echo "Website URL: ${websiteUrl}"

elif [ "$command" == "undeploy" ]; then
  echo "UNDEPLOYING"

  aws s3 rm s3://${websiteBucketName}/ --recursive
  aws s3 rm s3://${dataBucketName}/ --recursive

  aws cloudformation delete-stack --stack-name ${stackName} --region ${region}
  aws cloudformation wait stack-delete-complete --stack-name ${stackName} --region ${region}

else
  echo "Unknown command '$command'"
fi
