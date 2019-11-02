set -e

Region=us-east-1
StackName=vpn-experiment
ServerCertificateArnFileName=.server-certificate-arn.txt
ClientCertificateArnFileName=.client-certificate-arn.txt

command=$1

get_stack_output() {
  local stackName=$1
  local outputName=$2
  aws cloudformation describe-stacks \
    --stack-name ${stackName} \
    --query 'Stacks[0].Outputs[?OutputKey==`'${outputName}'`].OutputValue' \
    --output text \
    --region ${Region}
}

undeploy_stack() {
  local stackName=$1
  aws cloudformation delete-stack \
    --stack-name ${stackName} \
    --region ${Region}

  aws cloudformation wait stack-delete-complete \
    --stack-name ${stackName} \
    --region ${Region}
}

if [[ "${command}" == "make-certificates" ]]; then
  git clone https://github.com/OpenVPN/easy-rsa.git

  pushd easy-rsa/easyrsa3
  ./easyrsa init-pki
  EASYRSA_BATCH=1 ./easyrsa build-ca nopass
  ./easyrsa build-server-full server1 nopass
  ./easyrsa build-client-full client1.tld nopass
  popd

  if [ ! -f ${ServerCertificateArnFileName} ]; then
    serverCertificateArn=$(aws acm import-certificate \
      --certificate file://easy-rsa/easyrsa3/pki/issued/server1.crt \
      --private-key file://easy-rsa/easyrsa3/pki/private/server1.key \
      --certificate-chain file://easy-rsa/easyrsa3/pki/ca.crt \
      --region ${Region} \
      --output text \
      --query 'CertificateArn')
    echo ${serverCertificateArn} >${ServerCertificateArnFileName}
  fi

  if [ ! -f ${ClientCertificateArnFileName} ]; then
    clientCertificateArn=$(aws acm import-certificate \
      --certificate file://easy-rsa/easyrsa3/pki/issued/client1.tld.crt \
      --private-key file://easy-rsa/easyrsa3/pki/private/client1.tld.key \
      --certificate-chain file://easy-rsa/easyrsa3/pki/ca.crt \
      --region ${Region} \
      --output text \
      --query 'CertificateArn')
    echo ${clientCertificateArn} >${ClientCertificateArnFileName}
  fi

elif [[ "${command}" == "unmake-certificates" ]]; then
  serverCertificateArn=$(cat ${ServerCertificateArnFileName})
  aws acm delete-certificate \
    --certificate-arn ${serverCertificateArn} \
    --region ${Region}
  rm ${ServerCertificateArnFileName}

  clientCertificateArn=$(cat ${ClientCertificateArnFileName})
  aws acm delete-certificate \
    --certificate-arn ${clientCertificateArn} \
    --region ${Region}
  rm ${ClientCertificateArnFileName}

  rm -rf easy-rsa

elif [[ "${command}" == "deploy" ]]; then
  serverCertificateArn=$(cat ${ServerCertificateArnFileName})
  clientCertificateArn=$(cat ${ClientCertificateArnFileName})

  aws cloudformation deploy \
    --template-file template.yml \
    --stack-name ${StackName} \
    --capabilities CAPABILITY_NAMED_IAM \
    --region ${Region} \
    --no-fail-on-empty-changeset \
    --parameter-overrides \
    ServerCertificateArn=${serverCertificateArn} \
    ClientCertificateArn=${clientCertificateArn}

  dummyInstanceIp=$(get_stack_output ${StackName} "DummyInstanceIp")
  echo "Dummy Instance IP: ${dummyInstanceIp}"

elif [[ "${command}" == "undeploy" ]]; then
  undeploy_stack ${StackName}

elif [[ "${command}" == "export" ]]; then
  clientVpnEndpointId=$(get_stack_output ${StackName} "ClientVpnEndpointId")
  aws ec2 export-client-vpn-client-configuration \
    --client-vpn-endpoint-id ${clientVpnEndpointId} \
    --region ${Region} \
    --output text >config.ovpn

elif [[ "${command}" == "connect" ]]; then
  sudo openvpn \
    --config config.ovpn \
    --cert easy-rsa/easyrsa3/pki/issued/client1.tld.crt \
    --key easy-rsa/easyrsa3/pki/private/client1.tld.key

elif [[ "${command}" == "" ]]; then
  echo "No command specified"
else
  echo "Unknown command: ${command}"
fi
