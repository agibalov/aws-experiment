#!/bin/bash

set -ex

Region=us-east-1
BaseStackName=proton-experiment-base
EnvironmentTemplateName=env1
ServiceTemplateName=service1

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

get_proton_service_pipeline_output() {
  local serviceName=$1
  local outputName=$2
  aws proton list-service-pipeline-outputs \
    --service-name ${serviceName} \
    --query 'outputs[?key==`'${outputName}'`].valueString' \
    --output text \
    --region ${Region}
}

if [[ "${command}" == "deploy-base" ]]; then
  aws cloudformation deploy \
    --template-file base.yaml \
    --stack-name ${BaseStackName} \
    --capabilities CAPABILITY_NAMED_IAM \
    --region ${Region}

  pipelineServiceRoleArn=$(get_stack_output ${BaseStackName} "PipelineServiceRoleArn")
  aws proton update-account-settings \
    --pipeline-service-role-arn ${pipelineServiceRoleArn} \
    --region ${Region}

elif [[ "${command}" == "undeploy-base" ]]; then
  bucketName=$(get_stack_output ${BaseStackName} "BucketName")
  aws s3 rm s3://${bucketName} --recursive
  undeploy_stack ${BaseStackName}

elif [[ "${command}" == "create-environment-template" ]]; then
  aws proton create-environment-template \
    --name ${EnvironmentTemplateName} \
    --region ${Region}

elif [[ "${command}" == "delete-environment-template" ]]; then
  aws proton list-environment-template-versions \
    --template-name ${EnvironmentTemplateName} \
    --region ${Region} | \
    python get_template_versions.py | \
    while read -r major minor; do \
      aws proton delete-environment-template-version \
        --template-name ${EnvironmentTemplateName} \
        --major-version ${major} \
        --minor-version ${minor} \
        --region ${Region}; \
    done

  aws proton delete-environment-template \
    --name ${EnvironmentTemplateName} \
    --region ${Region}

elif [[ "${command}" == "create-environment-template-version" ]]; then
  timestamp=$(date '+%s')
  bundleFilename=${timestamp}.tar.gz
  tar -zcvf ${bundleFilename} environment/

  templatesBucketName=$(get_stack_output "${BaseStackName}" "BucketName")
  aws s3 cp ${bundleFilename} s3://${templatesBucketName}/${bundleFilename}
  environmentTemplateVersionJson=$(aws proton create-environment-template-version \
    --template-name ${EnvironmentTemplateName} \
    --source s3=\{bucket=${templatesBucketName},key=${bundleFilename}\} \
    --region ${Region})

  rm ${bundleFilename}

  majorVersion=$(echo ${environmentTemplateVersionJson} | jq '.environmentTemplateVersion.majorVersion|tonumber')
  minorVersion=$(echo ${environmentTemplateVersionJson} | jq '.environmentTemplateVersion.minorVersion|tonumber')

  aws proton wait environment-template-version-registered \
    --major-version ${majorVersion} \
    --minor-version ${minorVersion} \
    --template-name ${EnvironmentTemplateName} \
    --region ${Region}

  aws proton update-environment-template-version \
    --major-version ${majorVersion} \
    --minor-version ${minorVersion} \
    --template-name ${EnvironmentTemplateName} \
    --status PUBLISHED \
    --region ${Region}

  echo "Environment template version: ${majorVersion}.${minorVersion}"

elif [[ "${command}" == "create-environment" ]]; then
  envName=${envName:?not set or empty}
  majorVersion=${majorVersion:?not set or empty}
  minorVersion=${minorVersion:?not set or empty}

  protonServiceRoleArn=$(get_stack_output "${BaseStackName}" "ProtonServiceRoleArn")
  aws proton create-environment \
    --name ${envName} \
    --spec "{ proton: EnvironmentSpec, spec: { env_name: ${envName} } }" \
    --template-major-version ${majorVersion} \
    --template-minor-version ${minorVersion} \
    --template-name ${EnvironmentTemplateName} \
    --proton-service-role-arn ${protonServiceRoleArn} \
    --region ${Region}

  aws proton wait environment-deployed \
    --name ${envName} \
    --region ${Region}

elif [[ "${command}" == "delete-environment" ]]; then
  envName=${envName:?not set or empty}
  aws proton delete-environment \
    --name ${envName} \
    --region ${Region}

elif [[ "${command}" == "create-service-template" ]]; then
  aws proton create-service-template \
    --name ${ServiceTemplateName} \
    --region ${Region}

elif [[ "${command}" == "delete-service-template" ]]; then
  aws proton list-service-template-versions \
    --template-name ${ServiceTemplateName} \
    --region ${Region} | \
    python get_template_versions.py | \
    while read -r major minor; do \
      aws proton delete-service-template-version \
        --template-name ${ServiceTemplateName} \
        --major-version ${major} \
        --minor-version ${minor} \
        --region ${Region}; \
    done

  aws proton delete-service-template \
    --name ${ServiceTemplateName} \
    --region ${Region}

elif [[ "${command}" == "create-service-template-version" ]]; then
  envMajorVersion=${envMajorVersion:?not set or empty}

  timestamp=$(date '+%s')
  bundleFilename=${timestamp}.tar.gz
  tar -zcvf ${bundleFilename} service/

  templatesBucketName=$(get_stack_output "${BaseStackName}" "BucketName")
  aws s3 cp ${bundleFilename} s3://${templatesBucketName}/${bundleFilename}
  serviceTemplateVersionJson=$(aws proton create-service-template-version \
    --compatible-environment-templates \
    '[{"templateName":"'${EnvironmentTemplateName}'","majorVersion":"'${envMajorVersion}'"}]' \
    --template-name ${ServiceTemplateName} \
    --source s3=\{bucket=${templatesBucketName},key=${bundleFilename}\} \
    --region ${Region})

  rm ${bundleFilename}

  majorVersion=$(echo ${serviceTemplateVersionJson} | jq '.serviceTemplateVersion.majorVersion|tonumber')
  minorVersion=$(echo ${serviceTemplateVersionJson} | jq '.serviceTemplateVersion.minorVersion|tonumber')

  aws proton wait service-template-version-registered \
    --major-version ${majorVersion} \
    --minor-version ${minorVersion} \
    --template-name ${ServiceTemplateName} \
    --region ${Region}

  aws proton update-service-template-version \
    --major-version ${majorVersion} \
    --minor-version ${minorVersion} \
    --template-name ${ServiceTemplateName} \
    --status PUBLISHED \
    --region ${Region}

  echo "Service template version: ${majorVersion}.${minorVersion}"

elif [[ "${command}" == "create-service" ]]; then
  envName=${envName:?not set or empty}
  serviceName=${serviceName:?not set or empty}
  majorVersion=${majorVersion:?not set or empty}

  aws proton create-service \
    --name ${serviceName} \
    --spec "{ proton: ServiceSpec, pipeline: { hello: qqq }, instances: [ { name: inst1, environment: ${envName}, spec: { hello: world } } ] }" \
    --template-name ${ServiceTemplateName} \
    --template-major-version ${majorVersion} \
    --repository-connection-arn "arn:aws:codestar-connections:us-east-1:433819845724:connection/3986b1a7-2213-4f23-88c0-858b6e7a6b9d" \
    --repository-id "agibalov/aws-experiment" \
    --branch "labs-1496" \
    --region ${Region}

  aws proton wait service-created \
    --name ${serviceName} \
    --region ${Region}

  # TODO: service-instance-deployed, service-pipeline-deployed?

elif [[ "${command}" == "delete-service" ]]; then
  serviceName=${serviceName:?not set or empty}

  pipelineArtifactsBucketName=$(get_proton_service_pipeline_output "${serviceName}" "PipelineArtifactsBucketName")
  aws s3 rm s3://${pipelineArtifactsBucketName} --recursive

  aws proton delete-service \
    --name ${serviceName} \
    --region ${Region}

  aws proton wait service-deleted \
    --name ${serviceName} \
    --region ${Region}

elif [[ "${command}" == "" ]]; then
  echo "No command specified"
else
  echo "Unknown command ${command}"
fi
