package me.loki2302;

import com.amazonaws.services.lambda.AWSLambda;
import com.amazonaws.services.lambda.AWSLambdaClientBuilder;
import com.amazonaws.services.lambda.model.InvocationType;
import com.amazonaws.services.lambda.model.InvokeRequest;
import com.amazonaws.services.lambda.model.InvokeResult;
import org.junit.Rule;
import org.junit.Test;

import java.io.File;
import java.util.*;

public class DummyTest {
    @Rule
    public final CloudFormationStack cloudFormationStack = new CloudFormationStack();

    @Rule
    public final CloudWatchLogs cloudWatchLogs = new CloudWatchLogs();

    @Test
    public void dummy() throws InterruptedException {
        // language=YAML
        String template = "AWSTemplateFormatVersion: '2010-09-09'\n" +
                "\n" +
                "Parameters:\n" +
                "  LambdaS3BucketName:\n" +
                "    Type: String\n" +
                "  LambdaS3Key:\n" +
                "    Type: String\n" +
                "    \n" +
                "Outputs:\n" +
                "  LambdaAArn:\n" +
                "    Value: !GetAtt LambdaAFunction.Arn\n" +
                "\n" +
                "Resources:\n" +
                "  LambdaAFunction:\n" +
                "    Type: AWS::Lambda::Function\n" +
                "    Properties:\n" +
                "      FunctionName: LambdaAFunction\n" +
                "      Handler: me.loki2302.MyLambdaHandler::handle\n" +
                "      Role: !GetAtt MyLambdaRole.Arn\n" +
                "      Code:\n" +
                "        S3Bucket: !Ref LambdaS3BucketName\n" +
                "        S3Key: !Ref LambdaS3Key\n" +
                "      Runtime: java8\n" +
                "      MemorySize: 128\n" +
                "      Timeout: 60\n" +
                "          \n" +
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
                "        - PolicyName: DoEverything\n" +
                "          PolicyDocument:\n" +
                "            Version: 2012-10-17\n" +
                "            Statement:\n" +
                "              - Effect: Allow\n" +
                "                Action: \"*\"\n" +
                "                Resource: \"*\"";

        String bucketName = "dummy-bucket-r23r23r23r23r";
        File lambdaAFile = new File(System.getProperty("lambdaAZipFilename"));
        cloudFormationStack.createBucket(bucketName);
        cloudFormationStack.upload(lambdaAFile);

        Map<String, String> outputs = cloudFormationStack.deploy(
                template,
                new HashMap<String, String>() {{
                    put("LambdaS3BucketName", bucketName);
                    put("LambdaS3Key", lambdaAFile.getName());
                }});
        System.out.printf("outputs: %s\n", outputs);

        AWSLambda awsLambda = AWSLambdaClientBuilder.defaultClient();
        InvokeRequest invokeRequest = new InvokeRequest();
        invokeRequest.setFunctionName("LambdaAFunction");
        invokeRequest.setPayload("{\"jUnitSays\": \"I am a JUnit test\"}");
        invokeRequest.setInvocationType(InvocationType.Event);
        InvokeResult invokeResult = awsLambda.invoke(invokeRequest);
        System.out.printf("invokeResult: %s\n", invokeResult);


        System.out.println("waiting for #1");
        cloudWatchLogs.waitForMarker("/aws/lambda/LambdaAFunction","MY_MARKER_ONE");

        System.out.println("waiting for #2");
        cloudWatchLogs.waitForMarker("/aws/lambda/LambdaAFunction","MY_MARKER_TWO");

        System.out.println("DONE!");
    }
}
