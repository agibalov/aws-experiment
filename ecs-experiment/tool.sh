#!/bin/bash

Region=us-east-1

DeploymentBucketName=random-bucket-name-1212121
EcsStackName=ECS
App1Name=App1
App1EcrRepositoryName=app1
App1EcrStackName=${App1Name}Ecr
App1StackName=${App1Name}

command=$1

undeploy_stack() {
  stackName=$1
  aws cloudformation delete-stack \
    --stack-name ${stackName} \
    --region ${Region}

  aws cloudformation wait stack-delete-complete \
    --stack-name ${stackName} \
    --region ${Region}
}

get_stack_output() {
  stackName=$1
  outputName=$2
  aws cloudformation describe-stacks \
    --stack-name ${stackName} \
    --query 'Stacks[0].Outputs[?OutputKey==`'${outputName}'`].OutputValue' \
    --output text \
    --region ${Region}
}

if [ "${command}" == "deploy-ecs" ]; then
  aws cloudformation deploy \
    --template-file cloudformation/ecs.yml \
    --stack-name ${EcsStackName} \
    --capabilities CAPABILITY_NAMED_IAM \
    --region ${Region}

elif [ "${command}" == "undeploy-ecs" ]; then
  undeploy_stack ${EcsStackName}

elif [ "${command}" == "deploy-app1-ecr" ]; then
  aws cloudformation deploy \
    --template-file cloudformation/ecr.yml \
    --stack-name ${App1EcrStackName} \
    --capabilities CAPABILITY_NAMED_IAM \
    --region ${Region} \
    --parameter-overrides \
    EcrRepositoryName=${App1EcrRepositoryName}

elif [ "${command}" == "undeploy-app1-ecr" ]; then
  imageIds=$(aws ecr list-images \
    --repository-name ${App1EcrRepositoryName} \
    --query 'imageIds[].[imageDigest]' \
    --output=text \
    --region ${Region} | sed -E 's/(.+)/imageDigest=\1/')

  aws ecr batch-delete-image \
    --repository-name ${App1EcrRepositoryName} \
    --image-ids ${imageIds} \
    --region ${Region}

  undeploy_stack ${App1EcrStackName}

elif [ "${command}" == "deploy-app1" ]; then
  ./gradlew clean bootRepackage

  app1JarFilename=$(basename $(ls build/libs/*.jar))
  ecrRepository1Url=$(get_stack_output ${App1EcrStackName} "RepositoryUrl")
  $(aws ecr get-login --no-include-email --region ${Region})
  tag=build-$(uuidgen | tail -c 8)
  app1Image="${ecrRepository1Url}:${tag}"
  docker build \
    --build-arg jarFilename=${app1JarFilename} \
    --build-arg tag=${tag} \
    --tag ${app1Image} .
  docker push ${app1Image}

  aws s3 mb s3://${DeploymentBucketName} --region ${Region}

  aws cloudformation package \
    --template-file cloudformation/app1.yml \
    --s3-bucket ${DeploymentBucketName} \
    --s3-prefix app1 \
    --output-template-file _packaged.yml

  aws cloudformation deploy \
    --template-file _packaged.yml \
    --stack-name ${App1StackName} \
    --capabilities CAPABILITY_NAMED_IAM \
    --region ${Region} \
    --parameter-overrides \
    EcsStackName=${EcsStackName} \
    Image=${app1Image}

elif [ "${command}" == "undeploy-app1" ]; then
  undeploy_stack ${App1StackName}

elif [ "${command}" == "" ]; then
  echo "No command specified"
fi
