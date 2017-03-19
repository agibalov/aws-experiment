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

  `\
  aws cloudformation describe-stacks \
    --stack-name $stackName \
    --query Stacks[0].Outputs \
    --region $region | \
  python -c "
import json,sys;
o=json.load(sys.stdin);
print 'export URL=%s' % (filter(lambda x: x['OutputKey']=='Url', o)[0]['OutputValue'],);
print 'export DBADDRESS=%s' % (filter(lambda x: x['OutputKey']=='DbEndpointAddress', o)[0]['OutputValue'],);
print 'export DBPORT=%s' % (filter(lambda x: x['OutputKey']=='DbEndpointPort', o)[0]['OutputValue'],);
  "`
  echo "Database address: $DBADDRESS"
  echo "Database port: $DBPORT"

  echo "Testing GET $URL"
  curl $URL
  echo ""
  curl $URL
  echo ""
  curl $URL
  echo ""

else
  echo "Unknown command '$command'"
fi
