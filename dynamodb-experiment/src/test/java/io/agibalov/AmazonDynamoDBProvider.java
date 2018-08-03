package io.agibalov;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import org.testcontainers.dynamodb.DynaliteContainer;

public class AmazonDynamoDBProvider implements TestRule {
    private DynaliteContainer dynaliteContainer;

    public AmazonDynamoDBProvider() {
        dynaliteContainer = System.getProperty("AWS") != null ? null : new DynaliteContainer();
    }

    public AmazonDynamoDBClientBuilder getAmazonDynamoDB() {
        AmazonDynamoDBClientBuilder amazonDynamoDBClientBuilder = AmazonDynamoDBClientBuilder.standard();

        if(dynaliteContainer != null) {
            amazonDynamoDBClientBuilder = amazonDynamoDBClientBuilder
                    .withEndpointConfiguration(dynaliteContainer.getEndpointConfiguration())
                    .withCredentials(dynaliteContainer.getCredentials());
        } else {
            amazonDynamoDBClientBuilder = amazonDynamoDBClientBuilder
                    .withRegion(Regions.US_EAST_1);
        }

        return amazonDynamoDBClientBuilder;
    }

    @Override
    public Statement apply(Statement base, Description description) {
        if(dynaliteContainer != null) {
            return dynaliteContainer.apply(base, description);
        }
        return new Statement() {
            @Override
            public void evaluate() throws Throwable {
                base.evaluate();
            }
        };
    }
}
