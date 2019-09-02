Region=us-east-1
StackName=aws-appsync-experiment

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
elif [[ "${command}" == "undeploy" ]]; then
  undeploy_stack ${StackName}
elif [[ "${command}" == "test" ]]; then
  apiUrl=$(get_stack_output ${StackName} "ApiUrl")
  apiKey=$(get_stack_output ${StackName} "ApiKey")

  graphql="curl -XPOST
    ${apiUrl}
    -w "\\n"
    -H Content-Type:application/graphql
    -H x-api-key:${apiKey}
    -d "

  echo "Insert #1"
  ${graphql} '{
    "query":"mutation Test($id: String!, $text: String!) { putTodo(id:$id,text:$text) { id text } }",
    "variables":{"id":"todo1", "text":"Todo One"},
    "operationName":"Test"
  }'

  echo "Insert #2"
  ${graphql} '{
    "query":"mutation Test($id: String!, $text: String!) { putTodo(id:$id,text:$text) { id text } }",
    "variables":{"id":"todo2", "text":"Todo Two"},
    "operationName":"Test"
  }'

  echo "Get all"
  ${graphql} '{
    "query":"query Test { todos { id text } }",
    "variables":null,
    "operationName":"Test"
  }'

  echo "Delete #1"
  ${graphql} '{
    "query":"mutation Test($id:String!) { deleteTodo(id:$id) { id text } }",
    "variables":{"id":"todo1"},
    "operationName":"Test"
  }'

  echo "Get #2"
  ${graphql} '{
    "query":"query Test($id:String!) { todo(id:$id) { id text } }",
    "variables":{"id":"todo2"},
    "operationName":"Test"
  }'
elif [[ "${command}" == "" ]]; then
  echo "No command specified"
else
  echo "Unknown command: ${command}"
fi
