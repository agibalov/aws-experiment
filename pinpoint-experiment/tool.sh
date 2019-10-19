set -x

Region=us-east-1
AppStackName=pinpoint-experiment
CampaignStackName=pinpoint-campaign

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
    --template-file app.yml \
    --stack-name ${AppStackName} \
    --capabilities CAPABILITY_NAMED_IAM \
    --region ${Region}

  importBucketName=$(get_stack_output ${AppStackName} "ImportBucketName")
  aws s3 cp us-users-segment.csv s3://${importBucketName}

  applicationId=$(get_stack_output ${AppStackName} "ApplicationId")
  importBucketAccessRoleArn=$(get_stack_output ${AppStackName} "ImportBucketAccessRoleArn")
  aws pinpoint create-import-job \
    --application-id ${applicationId} \
    --import-job-request \
DefineSegment=true,\
Format=CSV,\
RegisterEndpoints=true,\
RoleArn=${importBucketAccessRoleArn},\
S3Url=s3://${importBucketName}/us-users-segment.csv,\
SegmentName=UsUsers \
    --region ${Region}

  allUsersSegmentId=$(get_stack_output ${AppStackName} "AllUsersSegmentId")
  aws cloudformation deploy \
    --template-file campaign.yml \
    --stack-name ${CampaignStackName} \
    --capabilities CAPABILITY_NAMED_IAM \
    --region ${Region} \
    --parameter-overrides \
    ApplicationId=${applicationId} \
    SegmentId=${allUsersSegmentId}
elif [[ "${command}" == "undeploy" ]]; then
  undeploy_stack ${CampaignStackName}

  importBucketName=$(get_stack_output ${AppStackName} "ImportBucketName")
  aws s3 rm s3://${importBucketName} --recursive

  undeploy_stack ${AppStackName}
elif [[ "${command}" == "" ]]; then
  echo "No command specified"
else
  echo "Unknown command: ${command}"
fi
