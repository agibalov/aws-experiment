package me.loki2302.integration;

import com.amazonaws.services.lambda.AWSLambda;
import com.amazonaws.services.lambda.AWSLambdaClientBuilder;
import com.amazonaws.services.lambda.model.InvocationType;
import com.amazonaws.services.lambda.model.InvokeRequest;
import com.amazonaws.services.lambda.model.InvokeResult;
import me.loki2302.awstestability.CloudFormationStack;
import me.loki2302.awstestability.CloudWatchLogs;
import org.junit.Rule;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeoutException;

public class DummyIntTest {
    private final static Logger LOGGER = LoggerFactory.getLogger(DummyIntTest.class);

    private final static Duration DEFAULT_AWAIT_DURATION = Duration.ofSeconds(30);

    @Rule
    public final CloudFormationStack cloudFormationStack = new CloudFormationStack();

    @Rule
    public final CloudWatchLogs cloudWatchLogs = new CloudWatchLogs();

    @Test
    public void dummy() throws InterruptedException, TimeoutException {
        // language=YAML
        String template = "AWSTemplateFormatVersion: '2010-09-09'\n" +
                "\n" +
                "Parameters:\n" +
                "  SourceBucketName:\n" +
                "    Type: String\n" +
                "  LambdaS3Key:\n" +
                "    Type: String\n" +
                "\n" +
                "Outputs:\n" +
                "  LambdaAFunctionLogGroup:\n" +
                "    Value: !GetAtt LambdaAFunctionStack.Outputs.LambdaAFunctionLogGroup\n" +
                "\n" +
                "Resources:        \n" +
                "  LambdaAFunctionStack:\n" +
                "    Type: AWS::CloudFormation::Stack\n" +
                "    Properties:\n" +
                "      TemplateURL: !Sub \"https://s3.amazonaws.com/${SourceBucketName}/lambda-a.yml\"\n" +
                "      Parameters:\n" +
                "        LambdaS3BucketName: !Ref SourceBucketName\n" +
                "        LambdaS3Key: !Ref LambdaS3Key\n" +
                "        LambdaRoleArn: !GetAtt MyLambdaRole.Arn\n" +
                "\n" +
                "  MyLambdaRole:\n" +
                "    Type: AWS::IAM::Role\n" +
                "    Properties:\n" +
                "      RoleName: MyLambdaRole\n" +
                "      AssumeRolePolicyDocument:\n" +
                "        Version: 2012-10-17\n" +
                "        Statement:\n" +
                "          Effect: Allow\n" +
                "          Principal:\n" +
                "            Service: lambda.amazonaws.com\n" +
                "          Action: sts:AssumeRole\n" +
                "      Path: /\n" +
                "      Policies:\n" +
                "        - PolicyName: DoEverythingButDontCreateLogGroups\n" +
                "          PolicyDocument:\n" +
                "            Version: 2012-10-17\n" +
                "            Statement:\n" +
                "              - Effect: Allow\n" +
                "                Action: \"*\"\n" +
                "                Resource: \"*\"\n" +
                "              - Effect: Deny\n" +
                "                Action: logs:CreateLogGroup\n" +
                "                Resource: \"*\"\n";

        Path templatesDirectoryPath = Paths.get(System.getProperty("templatesDirectory"));
        cloudFormationStack.uploadToSourceBucket(templatesDirectoryPath.resolve("lambda-a.yml").toFile());

        File lambdaAFile = new File(System.getProperty("lambdaAZipFilename"));
        cloudFormationStack.uploadToSourceBucket(lambdaAFile);

        Map<String, String> stackOutputs = cloudFormationStack.deploy(
                template,
                new HashMap<String, String>() {{
                    put("SourceBucketName", cloudFormationStack.getSourceBucketName());
                    put("LambdaS3Key", lambdaAFile.getName());
                }});

        String lambdaAFunctionLogGroup = stackOutputs.get("LambdaAFunctionLogGroup");

        AWSLambda awsLambda = AWSLambdaClientBuilder.defaultClient();
        InvokeRequest invokeRequest = new InvokeRequest();
        invokeRequest.setFunctionName("LambdaAFunction");
        invokeRequest.setPayload("{\"jUnitSays\": \"I am a JUnit test\"}");
        invokeRequest.setInvocationType(InvocationType.Event);
        InvokeResult invokeResult = awsLambda.invoke(invokeRequest);
        LOGGER.info("invokeResult: {}", invokeResult);

        Map<String, String> update1 = cloudWatchLogs.read(lambdaAFunctionLogGroup, Map.class, DEFAULT_AWAIT_DURATION);
        System.out.printf("Got update #1: %s\n", update1);

        Map<String, String> update2 = cloudWatchLogs.read(lambdaAFunctionLogGroup, Map.class, DEFAULT_AWAIT_DURATION);
        System.out.printf("Got update #2: %s\n", update2);

        LOGGER.info("DONE!");
    }
}
