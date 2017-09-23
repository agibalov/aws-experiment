package me.loki2302;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.model.*;
import com.amazonaws.waiters.WaiterParameters;
import org.junit.Test;

import java.io.Closeable;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class DummyTest {
    private final static String TEST_TABLE_NAME = "test1";

    @Test
    public void amazonDynamoDBTest() throws InterruptedException, IOException {
        AmazonDynamoDB amazonDynamoDB = AmazonDynamoDBClientBuilder.defaultClient();

        try(DynamodbTable ignore = new DynamodbTable(amazonDynamoDB, new CreateTableRequest()
                .withTableName(TEST_TABLE_NAME)
                .withAttributeDefinitions(new AttributeDefinition()
                        .withAttributeName("id")
                        .withAttributeType(ScalarAttributeType.S))
                .withKeySchema(new KeySchemaElement()
                        .withAttributeName("id")
                        .withKeyType(KeyType.HASH))
                .withProvisionedThroughput(new ProvisionedThroughput()
                        .withReadCapacityUnits(1L)
                        .withWriteCapacityUnits(1L)))) {

            Map<String, AttributeValue> attributeValues = new HashMap<>();
            attributeValues.put("id", new AttributeValue().withS("id1"));
            attributeValues.put("text", new AttributeValue().withS("hello there"));
            PutItemRequest putItemRequest = new PutItemRequest()
                    .withTableName(TEST_TABLE_NAME)
                    .withItem(attributeValues);
            amazonDynamoDB.putItem(putItemRequest);

            Map<String, AttributeValue> keyAttributeValues = new HashMap<>();
            keyAttributeValues.put("id", new AttributeValue().withS("id1"));
            DeleteItemRequest deleteItemRequest = new DeleteItemRequest()
                    .withTableName(TEST_TABLE_NAME)
                    .withKey(keyAttributeValues);
            amazonDynamoDB.deleteItem(deleteItemRequest);
        }
    }

    public static class DynamodbTable implements Closeable {
        private final AmazonDynamoDB amazonDynamoDB;
        private final CreateTableRequest createTableRequest;

        public DynamodbTable(
                AmazonDynamoDB amazonDynamoDB,
                CreateTableRequest createTableRequest) {

            this.amazonDynamoDB = amazonDynamoDB;
            this.createTableRequest = createTableRequest;

            amazonDynamoDB.createTable(createTableRequest);

            amazonDynamoDB.waiters().tableExists().run(
                    new WaiterParameters<>(new DescribeTableRequest()
                            .withTableName(TEST_TABLE_NAME)));
        }

        @Override
        public void close() throws IOException {
            DeleteTableRequest deleteTableRequest = new DeleteTableRequest()
                    .withTableName(createTableRequest.getTableName());
            amazonDynamoDB.deleteTable(deleteTableRequest);

            amazonDynamoDB.waiters().tableNotExists().run(
                    new WaiterParameters<>(new DescribeTableRequest()
                            .withTableName(createTableRequest.getTableName())));
        }
    }
}
