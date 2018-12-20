#!/bin/bash

set -x

Region=us-east-1
DeploymentBucketName=random-bucket-name-1212121
EcsStackName=ECS
Ec2AppStackName=Ec2App
FargateAppStackName=FargateApp

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

delete_ecr_images() {
  local repositoryName=$1
  local imageIds=$(aws ecr list-images \
    --repository-name ${repositoryName} \
    --query 'imageIds[].[imageDigest]' \
    --output=text \
    --region ${Region} | sed -E 's/(.+)/imageDigest=\1/')

  if [[ ! -z "$imageIds" ]]; then
    aws ecr batch-delete-image \
      --repository-name ${repositoryName} \
      --image-ids ${imageIds} \
      --region ${Region}
  fi
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

deploy_app() {
  local dockerRepositoryUrl=$1
  local cloudFormationTemplateFilename=$2
  local deploymentBucketPrefix=$3
  local stackName=$4

  ./gradlew clean bootRepackage

  local appJarFilename=$(basename $(ls build/libs/*.jar))
  $(aws ecr get-login --no-include-email --region ${Region})
  local tag=build-$(uuidgen | tail -c 8)
  local appImage="${dockerRepositoryUrl}:${tag}"
  docker build \
    --build-arg jarFilename=${appJarFilename} \
    --build-arg tag=${tag} \
    --tag ${appImage} .
  docker push ${appImage}

  aws s3 mb s3://${DeploymentBucketName} \
    --region ${Region}

  aws cloudformation package \
    --template-file ${cloudFormationTemplateFilename} \
    --s3-bucket ${DeploymentBucketName} \
    --s3-prefix ${deploymentBucketPrefix} \
    --output-template-file _packaged.yml

  aws cloudformation deploy \
    --template-file _packaged.yml \
    --stack-name ${stackName} \
    --capabilities CAPABILITY_NAMED_IAM \
    --region ${Region} \
    --parameter-overrides \
    EcsStackName=${EcsStackName} \
    Image=${appImage}
}

if [[ "${command}" == "deploy-ecs" ]]; then
  aws cloudformation deploy \
    --template-file cloudformation/ecs.yml \
    --stack-name ${EcsStackName} \
    --capabilities CAPABILITY_NAMED_IAM \
    --region ${Region}

elif [[ "${command}" == "undeploy-ecs" ]]; then
  ec2AppRepositoryName=$(get_stack_output ${EcsStackName} "Ec2AppRepositoryName")
  delete_ecr_images ${ec2AppRepositoryName}

  fargateAppRepositoryName=$(get_stack_output ${EcsStackName} "FargateAppRepositoryName")
  delete_ecr_images ${fargateAppRepositoryName}

  undeploy_stack ${EcsStackName}

  aws s3 rm s3://${DeploymentBucketName} \
    --recursive \
    --region ${Region}

  aws s3 rb s3://${DeploymentBucketName} \
    --region ${Region}

elif [[ "${command}" == "deploy-ec2-app" ]]; then
  ec2AppRepositoryUrl=$(get_stack_output ${EcsStackName} "Ec2AppRepositoryUrl")
  deploy_app \
    ${ec2AppRepositoryUrl} \
    "cloudformation/ec2-app.yml" \
    "ec2" \
    ${Ec2AppStackName}

elif [[ "${command}" == "undeploy-ec2-app" ]]; then
  undeploy_stack ${Ec2AppStackName}

elif [[ "${command}" == "deploy-fargate-app" ]]; then
  fargateAppRepositoryUrl=$(get_stack_output ${EcsStackName} "FargateAppRepositoryUrl")
  deploy_app \
    ${fargateAppRepositoryUrl} \
    "cloudformation/fargate-app.yml" \
    "fargate" \
    ${FargateAppStackName}

elif [[ "${command}" == "undeploy-fargate-app" ]]; then
  undeploy_stack ${FargateAppStackName}

elif [[ "${command}" == "" ]]; then
  echo "No command specified"
fi
