package io.agibalov;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.model.*;
import org.junit.ClassRule;
import org.junit.Test;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.fail;

public class ConditionalOperationsTest {
    private final static String TEST_TABLE_NAME = "test1";
    private final static CreateTableRequest CREATE_TABLE_REQUEST = new CreateTableRequest()
            .withTableName(TEST_TABLE_NAME)
            .withAttributeDefinitions(new AttributeDefinition()
                    .withAttributeName("id")
                    .withAttributeType(ScalarAttributeType.S))
            .withKeySchema(new KeySchemaElement()
                    .withAttributeName("id")
                    .withKeyType(KeyType.HASH))
            .withProvisionedThroughput(new ProvisionedThroughput()
                    .withReadCapacityUnits(1L)
                    .withWriteCapacityUnits(1L));

    @ClassRule
    public static AmazonDynamoDBProvider amazonDynamoDBProvider = new AmazonDynamoDBProvider();

    @Test
    public void conditionalPutShouldWork() throws IOException {
        AmazonDynamoDB amazonDynamoDB = amazonDynamoDBProvider.getAmazonDynamoDB()
                .build();
        try(DynamoDbTableResource ignore = new DynamoDbTableResource(amazonDynamoDB, CREATE_TABLE_REQUEST)) {
            {
                Map<String, AttributeValue> attributeValues = new HashMap<>();
                attributeValues.put("id", new AttributeValue().withS("id1"));
                attributeValues.put("text", new AttributeValue().withS("hello there"));
                PutItemRequest putItemRequest = new PutItemRequest()
                        .withTableName(TEST_TABLE_NAME)
                        .withItem(attributeValues);
                amazonDynamoDB.putItem(putItemRequest);
            }

            Map<String, AttributeValue> attributeValues = new HashMap<>();
            attributeValues.put("id", new AttributeValue().withS("id1"));
            attributeValues.put("text", new AttributeValue().withS("hello there123"));

            {
                // this put should fail because the "text" is "hello there", not "hello there111"
                PutItemRequest putItemRequest = new PutItemRequest()
                        .withTableName(TEST_TABLE_NAME)
                        .withItem(attributeValues)
                        .withConditionExpression("#text = :theText")
                        .withExpressionAttributeNames(new HashMap<String, String>() {{
                            put("#text", "text");
                        }})
                        .withExpressionAttributeValues(new HashMap<String, AttributeValue>() {{
                            put(":theText", new AttributeValue().withS("hello there111"));
                        }});
                try {
                    amazonDynamoDB.putItem(putItemRequest);
                    fail();
                } catch (ConditionalCheckFailedException e) {
                    // intentionally blank
                }
            }

            {
                // this put should fail because the "text" is "hello there"
                PutItemRequest putItemRequest = new PutItemRequest()
                        .withTableName(TEST_TABLE_NAME)
                        .withItem(attributeValues)
                        .withConditionExpression("#text = :theText")
                        .withExpressionAttributeNames(new HashMap<String, String>() {{
                            put("#text", "text");
                        }})
                        .withExpressionAttributeValues(new HashMap<String, AttributeValue>() {{
                            put(":theText", new AttributeValue().withS("hello there"));
                        }});
                amazonDynamoDB.putItem(putItemRequest);
            }
        }
    }

    @Test
    public void conditionalUpdateShouldWork() throws IOException {
        AmazonDynamoDB amazonDynamoDB = amazonDynamoDBProvider.getAmazonDynamoDB()
                .build();
        try(DynamoDbTableResource ignore = new DynamoDbTableResource(amazonDynamoDB, CREATE_TABLE_REQUEST)) {
            {
                Map<String, AttributeValue> attributeValues = new HashMap<>();
                attributeValues.put("id", new AttributeValue().withS("id1"));
                attributeValues.put("text", new AttributeValue().withS("hello there"));
                PutItemRequest putItemRequest = new PutItemRequest()
                        .withTableName(TEST_TABLE_NAME)
                        .withItem(attributeValues);
                amazonDynamoDB.putItem(putItemRequest);
            }

            {
                // this update should fail because the "text" is "hello there", not "hello there111"
                UpdateItemRequest updateItemRequest = new UpdateItemRequest()
                        .withTableName(TEST_TABLE_NAME)
                        .withUpdateExpression("SET #text = :newText")
                        .withKey(new HashMap<String, AttributeValue>() {{
                            put("id", new AttributeValue().withS("id1"));
                        }})
                        .withConditionExpression("#text = :theText")
                        .withExpressionAttributeNames(new HashMap<String, String>() {{
                            put("#text", "text");
                        }})
                        .withExpressionAttributeValues(new HashMap<String, AttributeValue>() {{
                            put(":theText", new AttributeValue().withS("hello there111"));
                            put(":newText", new AttributeValue().withS("some new text"));
                        }});
                try {
                    amazonDynamoDB.updateItem(updateItemRequest);
                    fail();
                } catch (ConditionalCheckFailedException e) {
                    // intentioanlly blank
                }
            }

            {
                // this update should succeed because the "text" is "hello there"
                UpdateItemRequest updateItemRequest = new UpdateItemRequest()
                        .withTableName(TEST_TABLE_NAME)
                        .withUpdateExpression("SET #text = :newText")
                        .withKey(new HashMap<String, AttributeValue>() {{
                            put("id", new AttributeValue().withS("id1"));
                        }})
                        .withConditionExpression("#text = :theText")
                        .withExpressionAttributeNames(new HashMap<String, String>() {{
                            put("#text", "text");
                        }})
                        .withExpressionAttributeValues(new HashMap<String, AttributeValue>() {{
                            put(":theText", new AttributeValue().withS("hello there"));
                            put(":newText", new AttributeValue().withS("some new text"));
                        }});

                amazonDynamoDB.updateItem(updateItemRequest);
            }
        }
    }
}
