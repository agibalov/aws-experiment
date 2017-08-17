package me.loki2302;

import com.amazonaws.services.cloudformation.AmazonCloudFormation;
import com.amazonaws.services.cloudformation.AmazonCloudFormationClientBuilder;
import com.amazonaws.services.cloudformation.model.*;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.amazonaws.waiters.FixedDelayStrategy;
import com.amazonaws.waiters.MaxAttemptsRetryStrategy;
import com.amazonaws.waiters.PollingStrategy;
import com.amazonaws.waiters.WaiterParameters;
import org.junit.rules.ExternalResource;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

public class CloudFormationStack extends ExternalResource {
    private final static PollingStrategy POLLING_STRATEGY = new PollingStrategy(
            new MaxAttemptsRetryStrategy(100),
            new FixedDelayStrategy(5));

    private String stackName;
    private String bucketName;
    private AmazonS3 amazonS3;
    private AmazonCloudFormation amazonCloudFormation;

    public void createBucket(String name) {
        amazonS3 = AmazonS3ClientBuilder.defaultClient();
        amazonS3.createBucket(name);
        bucketName = name;
    }

    public void upload(File file) {
        amazonS3.putObject(bucketName, file.getName(), file);
    }

    public Map<String, String> deploy(
            String cloudFormationTemplateBody,
            Map<String, String> parameters) {

        stackName = "test-" + UUID.randomUUID();

        amazonCloudFormation = AmazonCloudFormationClientBuilder.defaultClient();

        CreateStackRequest createStackRequest = new CreateStackRequest();
        createStackRequest.setStackName(stackName);
        createStackRequest.setOnFailure(OnFailure.DELETE);
        createStackRequest.setCapabilities(Collections.singleton(Capability.CAPABILITY_NAMED_IAM.toString()));
        createStackRequest.setTemplateBody(cloudFormationTemplateBody);
        createStackRequest.setParameters(makeParametersListFromParametersMap(parameters));
        CreateStackResult createStackResult = amazonCloudFormation.createStack(createStackRequest);
        System.out.printf("createStackResult: %s\n", createStackResult);

        DescribeStacksRequest describeStacksRequest = new DescribeStacksRequest();
        describeStacksRequest.setStackName(stackName);
        amazonCloudFormation.waiters()
                .stackCreateComplete()
                .run(new WaiterParameters<>(describeStacksRequest).withPollingStrategy(POLLING_STRATEGY));

        DescribeStacksResult describeStacksResult = amazonCloudFormation.describeStacks(describeStacksRequest);
        List<Output> outputs = describeStacksResult.getStacks().get(0).getOutputs();
        Map<String, String> outputsMap = new HashMap<>();
        outputs.forEach(o -> outputsMap.put(o.getOutputKey(), o.getOutputValue()));

        return outputsMap;
    }

    private static List<Parameter> makeParametersListFromParametersMap(Map<String, String> parametersMap) {
        return new ArrayList<>(parametersMap.entrySet().stream()
                .map(e -> new Parameter()
                        .withParameterKey(e.getKey())
                        .withParameterValue(e.getValue()))
                .collect(Collectors.toList()));
    }

    @Override
    protected void after() {
        DeleteStackRequest deleteStackRequest = new DeleteStackRequest();
        deleteStackRequest.setStackName(stackName);
        DeleteStackResult deleteStackResult = amazonCloudFormation.deleteStack(deleteStackRequest);
        System.out.printf("deleteStackResult: %s\n", deleteStackResult);

        DescribeStacksRequest describeStacksRequest = new DescribeStacksRequest();
        describeStacksRequest.setStackName(stackName);
        amazonCloudFormation.waiters()
                .stackDeleteComplete()
                .run(new WaiterParameters<>(describeStacksRequest).withPollingStrategy(POLLING_STRATEGY));

        ObjectListing objectListing = amazonS3.listObjects(bucketName);
        while(true) {
            for(Iterator it = objectListing.getObjectSummaries().iterator(); it.hasNext(); ) {
                S3ObjectSummary s3ObjectSummary = (S3ObjectSummary) it.next();
                System.out.printf("Deleting %s\n", s3ObjectSummary.getKey());
                amazonS3.deleteObject(bucketName, s3ObjectSummary.getKey());
            }

            if(objectListing.isTruncated()) {
                objectListing = amazonS3.listNextBatchOfObjects(objectListing);
            } else {
                break;
            }
        }
        amazonS3.deleteBucket(bucketName);
    }
}
