function get_ssm_parameter() {
  local parameterName="$1"
  aws ssm get-parameter \
    --name ${parameterName} \
    --with-decryption \
    --query 'Parameter.Value' \
    --output text \
    --region us-east-1
}

AMP_SNOWFLAKE_URL=$(get_ssm_parameter 'AmpSnowflakeUrl') \
AMP_SNOWFLAKE_USERNAME=$(get_ssm_parameter 'AmpSnowflakeUsername') \
AMP_SNOWFLAKE_PASSWORD=$(get_ssm_parameter 'AmpSnowflakePassword') \
AMP_MYSQL_USERNAME=$(get_ssm_parameter 'AmpMysqlUsername') \
AMP_MYSQL_PASSWORD=$(get_ssm_parameter 'AmpMysqlPassword') \
java $JAVA_OPTS \
  -Djava.security.egd=file:/dev/./urandom \
  -Dspring.profiles.active=$ENV \
  -jar app.jar
