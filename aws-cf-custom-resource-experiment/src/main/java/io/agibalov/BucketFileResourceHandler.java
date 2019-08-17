package io.agibalov;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import okhttp3.*;

import java.util.HashMap;
import java.util.Map;

public class BucketFileResourceHandler implements RequestHandler<Map<String, Object>, Object> {
    @Override
    public Object handleRequest(Map<String, Object> input, Context context) {
        LambdaLogger logger = context.getLogger();

        String responseUrl = (String) input.get("ResponseURL");
        logger.log(String.format("Response URL: %s", responseUrl));

        String physicalResourceId = "unknown";

        try {
            AmazonS3 s3Client = AmazonS3ClientBuilder.defaultClient();

            logger.log(String.format("input: %s", input));

            String resourceType = (String) input.get("ResourceType");
            if (!resourceType.equals("Custom::BucketFile")) {
                throw new RuntimeException(String.format("Unexpected resource type: \"%s\"", resourceType));
            }

            Map<String, Object> resourceProperties = (Map<String, Object>) input.get("ResourceProperties");
            String bucketName = (String) resourceProperties.get("BucketName");
            if (bucketName == null) {
                throw new RuntimeException("\"BucketName\" is not set");
            }

            String key = (String) resourceProperties.get("Key");
            if (key == null) {
                throw new RuntimeException("\"Key\" is not set");
            }

            String content = (String) resourceProperties.get("Content");
            if (content == null) {
                throw new RuntimeException("\"Content\" is not set");
            }

            physicalResourceId = String.format("%s-%s", bucketName, key);

            String requestType = (String) input.get("RequestType");
            if (requestType.equals("Create")) {
                s3Client.putObject(bucketName, key, content);
            } else if (requestType.equals("Update")) {
                s3Client.putObject(bucketName, key, content);
            } else if (requestType.equals("Delete")) {
                s3Client.deleteObject(bucketName, key);
            } else {
                throw new RuntimeException(String.format("Don't know how to handle \"%s\"", requestType));
            }

            Map<String, Object> output = new HashMap<>();
            output.put("Status", "SUCCESS");
            output.put("Reason", "hey there");
            output.put("PhysicalResourceId", physicalResourceId);
            output.put("StackId", input.get("StackId"));
            output.put("RequestId", input.get("RequestId"));
            output.put("LogicalResourceId", input.get("LogicalResourceId"));
            output.put("Data", new HashMap<String, Object>() {{
                put("ContentLength", content.length());
            }});

            sendResponse(logger, responseUrl, output);
        } catch(Throwable t) {
            logger.log(String.format("Exception: %s", t));

            String reason = t.getMessage();
            if (reason == null) {
                reason = t.toString();
            }

            Map<String, Object> output = new HashMap<>();
            output.put("Status", "FAILED");
            output.put("Reason", reason);
            output.put("PhysicalResourceId", physicalResourceId);
            output.put("StackId", input.get("StackId"));
            output.put("RequestId", input.get("RequestId"));
            output.put("LogicalResourceId", input.get("LogicalResourceId"));
            sendResponse(logger, responseUrl, output);
        }

        return null;
    }

    @SneakyThrows
    private static void sendResponse(
            LambdaLogger logger,
            String responseUrl,
            Map<String, Object> responseBody) {

        ObjectMapper objectMapper = new ObjectMapper();
        String json = objectMapper.writeValueAsString(responseBody);

        OkHttpClient okHttpClient = new OkHttpClient();
        Response response = okHttpClient.newCall(new Request.Builder()
                .url(responseUrl)
                .put(RequestBody.create(json, MediaType.parse("application/json")))
                .build()).execute();
        logger.log(String.format("Response status: %d", response.code()));
    }
}
