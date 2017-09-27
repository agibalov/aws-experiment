package me.loki2302.aws;

import com.amazonaws.services.cloudformation.AmazonCloudFormation;
import com.amazonaws.services.cloudformation.AmazonCloudFormationClientBuilder;
import com.amazonaws.services.cloudformation.model.DescribeStacksRequest;
import com.amazonaws.services.cloudformation.model.DescribeStacksResult;
import com.amazonaws.services.cloudformation.model.Output;
import com.amazonaws.services.cloudformation.model.Stack;
import org.junit.Test;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class AwsTest {
    @Test
    public void theAppShouldWorkWhenItsBeenDeployed() {
        AmazonCloudFormation amazonCloudFormation = AmazonCloudFormationClientBuilder.defaultClient();
        DescribeStacksRequest describeStacksRequest = new DescribeStacksRequest()
                .withStackName("App1");
        DescribeStacksResult describeStacksResult = amazonCloudFormation.describeStacks(describeStacksRequest);
        List<Stack> stacks = describeStacksResult.getStacks();
        assertEquals(1, stacks.size());

        Stack app1Stack = stacks.get(0);
        Output app1UrlOutput = app1Stack.getOutputs().stream()
                .filter(o -> o.getOutputKey().equals("App1Url"))
                .findFirst()
                .orElse(null);
        assertNotNull(app1UrlOutput);

        String app1Url = app1UrlOutput.getOutputValue();

        RestTemplate restTemplate = new RestTemplate();
        Map<String, String> body = restTemplate.getForObject(app1Url + "hello", Map.class);
        assertTrue(body.get("message").contains("Hello there"));
    }
}
