command=$1

if [[ "${command}" == "test" ]]; then
  profile=$2
  awslimitchecker="docker run
    -v $(readlink -f ~/.aws/credentials):/root/.aws/credentials
    jantman/awslimitchecker:master
    --skip-ta
    --profile ${profile}
    --region us-east-1"

  echo "*** KNOWN SERVICE TYPES"
  ${awslimitchecker} --list-services

  echo "*** LIMITS"
  ${awslimitchecker} --list-limits

  echo "*** CURRENT USAGE"
  ${awslimitchecker} --show-usage

  echo "*** CURRENT STATE"
  ${awslimitchecker} \
    --warning-threshold 1 \
    --critical-threshold 2
elif [[ "${command}" == "" ]]; then
  echo "No command specified"
else
  echo "Unknown command: ${command}"
fi
