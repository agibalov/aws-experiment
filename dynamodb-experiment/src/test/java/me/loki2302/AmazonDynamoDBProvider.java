package me.loki2302;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import org.testcontainers.dynamodb.DynaliteContainer;

public class AmazonDynamoDBProvider implements TestRule {
    private DynaliteContainer dynaliteContainer;
    private AmazonDynamoDB amazonDynamoDB;

    public AmazonDynamoDBProvider() {
        dynaliteContainer = System.getProperty("AWS") != null ? null : new DynaliteContainer();
    }

    public AmazonDynamoDB getAmazonDynamoDB() {
        if(dynaliteContainer != null) {
            return dynaliteContainer.getClient();
        }
        return amazonDynamoDB;
    }

    @Override
    public Statement apply(Statement base, Description description) {
        if(dynaliteContainer != null) {
            return dynaliteContainer.apply(base, description);
        }
        return new Statement() {
            @Override
            public void evaluate() throws Throwable {
                amazonDynamoDB = AmazonDynamoDBClientBuilder.defaultClient();
                try {
                    base.evaluate();
                } finally {
                    amazonDynamoDB.shutdown();
                }
            }
        };
    }
}
