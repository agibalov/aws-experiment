package io.agibalov;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.model.*;
import org.junit.*;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class BasicDynamoDbTest {
    private final static String TEST_TABLE_NAME = "test1";

    @ClassRule
    public static AmazonDynamoDBProvider amazonDynamoDBProvider = new AmazonDynamoDBProvider();

    @Test
    public void dynamoDbShouldWork() throws IOException {
        AmazonDynamoDB amazonDynamoDB = amazonDynamoDBProvider.getAmazonDynamoDB()
                .build();
        try(DynamoDbTableResource ignore = new DynamoDbTableResource(amazonDynamoDB, new CreateTableRequest()
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
}
