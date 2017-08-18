package me.loki2302.awstestability;

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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

public class CloudFormationStack extends ExternalResource {
    private final static Logger LOGGER = LoggerFactory.getLogger(CloudFormationStack.class);
    private final static PollingStrategy POLLING_STRATEGY = new PollingStrategy(
            new MaxAttemptsRetryStrategy(100),
            new FixedDelayStrategy(5));

    private String stackName;
    private String sourceBucketName;
    private AmazonS3 amazonS3;
    private AmazonCloudFormation amazonCloudFormation;

    @Override
    protected void before() throws Throwable {
        sourceBucketName = "test-" + UUID.randomUUID();
        amazonS3 = AmazonS3ClientBuilder.defaultClient();

        LOGGER.info("Creating bucket {}", sourceBucketName);
        amazonS3.createBucket(sourceBucketName);

        LOGGER.info("Created a bucket {}", sourceBucketName);
    }

    public String getSourceBucketName() {
        return sourceBucketName;
    }

    public void uploadToSourceBucket(File file) {
        String s3Key = file.getName();
        LOGGER.info("Uploading {} to {} as {}", file, sourceBucketName, s3Key);
        amazonS3.putObject(sourceBucketName, s3Key, file);
    }

    public Map<String, String> deploy(
            String cloudFormationTemplateBody,
            Map<String, String> parameters) {

        stackName = "test-" + UUID.randomUUID();
        amazonCloudFormation = AmazonCloudFormationClientBuilder.defaultClient();

        LOGGER.info("Creating stack {}", stackName);
        CreateStackRequest createStackRequest = new CreateStackRequest();
        createStackRequest.setStackName(stackName);
        createStackRequest.setOnFailure(OnFailure.DELETE);
        createStackRequest.setCapabilities(Collections.singleton(Capability.CAPABILITY_NAMED_IAM.toString()));
        createStackRequest.setTemplateBody(cloudFormationTemplateBody);
        createStackRequest.setParameters(makeParametersListFromParametersMap(parameters));
        amazonCloudFormation.createStack(createStackRequest);

        LOGGER.info("Waiting for CloudFormation to finish creating stack {}", stackName);
        DescribeStacksRequest describeStacksRequest = new DescribeStacksRequest();
        describeStacksRequest.setStackName(stackName);
        amazonCloudFormation.waiters()
                .stackCreateComplete()
                .run(new WaiterParameters<>(describeStacksRequest).withPollingStrategy(POLLING_STRATEGY));
        LOGGER.info("Stack {} has been created", stackName);

        DescribeStacksResult describeStacksResult = amazonCloudFormation.describeStacks(describeStacksRequest);
        List<Output> outputs = describeStacksResult.getStacks().get(0).getOutputs();
        Map<String, String> outputsMap = new HashMap<>();
        outputs.forEach(o -> outputsMap.put(o.getOutputKey(), o.getOutputValue()));
        LOGGER.info("Stack {} outputs: {}", stackName, outputs);

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
        LOGGER.info("Deleting stack {}", stackName);
        DeleteStackRequest deleteStackRequest = new DeleteStackRequest();
        deleteStackRequest.setStackName(stackName);
        amazonCloudFormation.deleteStack(deleteStackRequest);

        LOGGER.info("Waiting for CloudFormation to finish deleting stack {}", stackName);
        DescribeStacksRequest describeStacksRequest = new DescribeStacksRequest();
        describeStacksRequest.setStackName(stackName);
        amazonCloudFormation.waiters()
                .stackDeleteComplete()
                .run(new WaiterParameters<>(describeStacksRequest).withPollingStrategy(POLLING_STRATEGY));
        LOGGER.info("Stack {} has been deleted", stackName);

        LOGGER.info("Deleting contents of bucket {}", sourceBucketName);
        ObjectListing objectListing = amazonS3.listObjects(sourceBucketName);
        while(true) {
            for(Iterator it = objectListing.getObjectSummaries().iterator(); it.hasNext(); ) {
                S3ObjectSummary s3ObjectSummary = (S3ObjectSummary) it.next();
                LOGGER.info("Deleting {}", s3ObjectSummary.getKey());
                amazonS3.deleteObject(sourceBucketName, s3ObjectSummary.getKey());
            }

            if(objectListing.isTruncated()) {
                objectListing = amazonS3.listNextBatchOfObjects(objectListing);
            } else {
                break;
            }
        }
        LOGGER.info("Finished deleting contents of bucket {}", sourceBucketName);
        LOGGER.info("Deleting bucket {}", sourceBucketName);
        amazonS3.deleteBucket(sourceBucketName);
        LOGGER.info("Deleted bucket {}", sourceBucketName);
    }
}
