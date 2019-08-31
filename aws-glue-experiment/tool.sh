Region=us-east-1
StackName=aws-glue-experiment

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
    --template-file template.yml \
    --stack-name ${StackName} \
    --capabilities CAPABILITY_NAMED_IAM \
    --region ${Region}

  bucketName=$(get_stack_output ${StackName} "BucketName")
  aws s3 cp ./export_rds_to_s3_job_script.py s3://${bucketName}/ \
    --region ${Region}

  rdsHost=$(get_stack_output ${StackName} "RdsHost")
  rdsPort=$(get_stack_output ${StackName} "RdsPort")
  rdsDb=$(get_stack_output ${StackName} "RdsDb")
  rdsUsername=$(get_stack_output ${StackName} "RdsUsername")
  rdsPassword=$(get_stack_output ${StackName} "RdsPassword")

  docker run -it --rm mysql:8 mysql \
    --host=${rdsHost} \
    --port=${rdsPort} \
    --user=${rdsUsername} \
    --password=${rdsPassword} \
    --execute "
    drop table if exists salaries;
    create table salaries(id varchar(100) primary key, salary int not null);
    insert into salaries(id, salary) values('emp1', 100);
    insert into salaries(id, salary) values('emp2', 200);
    insert into salaries(id, salary) values('emp3', 110);
    insert into salaries(id, salary) values('emp4', 110);
    " \
    ${rdsDb}
elif [[ "${command}" == "undeploy" ]]; then
  bucketName=$(get_stack_output ${StackName} "BucketName")
  aws s3 rm s3://${bucketName} \
    --recursive \
    --region ${Region}
  undeploy_stack ${StackName}
elif [[ "${command}" == "start-job" ]]; then
  jobName=$(get_stack_output ${StackName} "ExportRdsToS3JobName")
  jobRunId=$(aws glue start-job-run \
    --job-name ${jobName} \
    --region ${Region} \
    --output text \
    --query 'JobRunId')

  while true
  do
    state=$(aws glue get-job-run \
      --job-name ${jobName} \
      --run-id ${jobRunId} \
      --region ${Region} \
      --output text \
      --query 'JobRun.JobRunState')
    echo "$(date) State: ${state}"

    if [[ "${state}" == "SUCCEEDED" ]] || [[ "${state}" == "FAILED" ]]; then
      break
    fi

    sleep 60
  done

  echo "done"
elif [[ "${command}" == "" ]]; then
  echo "No command specified"
else
  echo "Unknown command: ${command}"
fi
