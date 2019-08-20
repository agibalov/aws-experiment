Region=us-east-1
LogGroupName=dummy2
LogStreamName=dummy2-stream1

command=$1

function query() {
  local queryString=$1
  now=$(date)

  queryId=$(aws logs start-query \
    --log-group-name ${LogGroupName} \
    --start-time $(date +%s%3N -d "${now} - 24 hours") \
    --end-time $(date +%s%3N -d "${now}") \
    --query-string "${queryString}" \
    --region ${Region} \
    --output text \
    --query "queryId")

  while true
  do
    status=$(aws logs get-query-results \
      --query-id ${queryId} \
      --region ${Region} \
      --output text \
      --query "status")
    echo "Status: ${status}"

    if [[ "${status}" == "Cancelled" ]] || [[ "${status}" == "Complete" ]] ||
        [[ "${status}" == "Failed" ]] || [[ "${status}" == "Timeout" ]]; then
      break
    fi

    sleep 1
  done

  aws logs get-query-results \
    --query-id ${queryId} \
    --region ${Region} \
    --query "results[*].{timestamp:[?field=='@timestamp']|[0].value,message:[?field=='@message']|[0].value}" \
    --output table
}

if [[ "${command}" == "deploy" ]]; then
  aws logs create-log-group \
    --log-group-name ${LogGroupName} \
    --region ${Region}
  aws logs put-retention-policy \
    --log-group-name ${LogGroupName} \
    --retention-in-days 1 \
    --region ${Region}
  aws logs create-log-stream \
    --log-group-name ${LogGroupName} \
    --log-stream-name ${LogStreamName} \
    --region ${Region}

  now=$(date)
  aws logs put-log-events \
    --log-group-name ${LogGroupName} \
    --log-stream-name ${LogStreamName} \
    --region ${Region} \
    --log-events \
    timestamp=$(date +%s%3N -d "${now} - 6 seconds"),message='Hello World' \
    timestamp=$(date +%s%3N -d "${now} - 5 seconds"),message="\"{\\\"level\\\":\\\"ERROR\\\", \\\"service\\\":\\\"UserService\\\"}\"" \
    timestamp=$(date +%s%3N -d "${now} - 4 seconds"),message="\"{\\\"level\\\":\\\"WARN\\\", \\\"service\\\":\\\"TransactionService\\\"}\"" \
    timestamp=$(date +%s%3N -d "${now} - 3 seconds"),message="\"{\\\"level\\\":\\\"WARN\\\", \\\"service\\\":\\\"UserService\\\"}\"" \
    timestamp=$(date +%s%3N -d "${now} - 2 seconds"),message="\"{\\\"level\\\":\\\"INFO\\\", \\\"service\\\":\\\"TransactionService\\\"}\"" \
    timestamp=$(date +%s%3N -d "${now} - 1 second"),message="\"{\\\"level\\\":\\\"ERROR\\\", \\\"service\\\":\\\"UserService\\\"}\""

elif [[ "${command}" == "undeploy" ]]; then
  aws logs delete-log-stream \
    --log-group-name ${LogGroupName} \
    --log-stream-name ${LogStreamName} \
    --region ${Region}
  aws logs delete-log-group \
    --log-group-name ${LogGroupName} \
    --region ${Region}

elif [[ "${command}" == "test" ]]; then
  echo "Everything about TransactionService"
  query  \
  "fields @timestamp, @message
    | filter service = 'TransactionService'
    | sort @timestamp desc"

  echo "Most recent warning"
  query  \
  "fields @timestamp, @message
    | filter level = 'WARN'
    | sort @timestamp desc
    | limit 1"

  echo "Everything that contains 'worl'"
  query  \
  "fields @timestamp, @message
    | filter @message like /(?i)worl/
    | sort @timestamp desc"

elif [[ "${command}" == "" ]]; then
  echo "No command specified"
else
  echo "Unknown command: ${command}"
fi
