# MAKE SURE I DON'T COMMIT BEFORE REMOVING THE KEYS

# https://hub.docker.com/r/bobrik/curator/
# also see AWS ES compatibility matrix here: https://github.com/elastic/curator

# Since 5.3 they support aws_sign_request feature to look up the credentials automatically:
# https://www.elastic.co/guide/en/elasticsearch/client/curator/current/configfile.html#aws_sign_request
# 5.2 doesn't have it, so the credentials need to be supplied explicitly

awsAccessKeyId=${AWS_ACCESS_KEY_ID}
awsSecretAccessKey=${AWS_SECRET_ACCESS_KEY}
awsRegion=${AWS_REGION}
esHostWithPort=${ES_HOST_WITH_PORT}

if [ -z ${awsAccessKeyId} ] || [ -z ${awsSecretAccessKey} ] || [ -z ${awsRegion} ] || [ -z ${esHostWithPort} ] ; then
  echo "Please provide the arguments (see the source)"
  exit 1
fi

docker run \
  -v $(pwd)/curator-config.yml:/curator-config.yml \
  -v $(pwd)/curator-actions.yml:/curator-actions.yml \
  -e ES_HOST_WITH_PORT=${esHostWithPort} \
  -e AWS_ACCESS_KEY_ID=${awsAccessKeyId} \
  -e AWS_SECRET_ACCESS_KEY=${awsSecretAccessKey} \
  -e AWS_REGION=${awsRegion} \
  -e INDEX_AGE_UNIT=minutes \
  -e INDEX_AGE_UNIT_COUNT=1 \
  --rm \
  bobrik/curator:5.2.0 \
  --config /curator-config.yml \
  /curator-actions.yml
