package io.agibalov;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.model.*;
import org.junit.ClassRule;
import org.junit.Test;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class DynaliteScanLimitIssueTest {
    private final static String TEST_TABLE_NAME = "test1";

    @ClassRule
    public static AmazonDynamoDBProvider amazonDynamoDBProvider = new AmazonDynamoDBProvider();

    @Test
    public void canScan() throws IOException {
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
                        .withWriteCapacityUnits(100L)))) {

            int actualEvenNumberCount = 0;
            for(int i = 0; i < 50; ++i) {
                boolean isEven = i % 2 == 0;

                Map<String, AttributeValue> attributeValues = new HashMap<>();
                attributeValues.put("id", new AttributeValue().withS(String.valueOf(i)));
                attributeValues.put("type", new AttributeValue().withS(isEven ? "even" : "odd"));
                amazonDynamoDB.putItem(new PutItemRequest()
                        .withTableName(TEST_TABLE_NAME)
                        .withItem(attributeValues));

                if(isEven) {
                    ++actualEvenNumberCount;
                }
            }

            for (int pageSize = 1; pageSize < 30; ++pageSize) {
                int foundEvenNumberCount = 0;
                String exclusiveStartKey = null;
                while (true) {
                    ScanRequest scanRequest = new ScanRequest(TEST_TABLE_NAME)
                            .withFilterExpression("#type = :type")
                            .withExpressionAttributeNames(Collections.singletonMap("#type", "type"))
                            .withExpressionAttributeValues(Collections.singletonMap(":type", new AttributeValue().withS("even")))
                            .withLimit(pageSize);

                    if (exclusiveStartKey != null) {
                        scanRequest = scanRequest.withExclusiveStartKey(Collections.singletonMap(
                                "id",
                                new AttributeValue().withS(exclusiveStartKey)));
                    }

                    ScanResult scanResult = amazonDynamoDB.scan(scanRequest);
                    foundEvenNumberCount += scanResult.getItems().size();

                    Map<String, AttributeValue> lastEvaluatedKey = scanResult.getLastEvaluatedKey();
                    if (lastEvaluatedKey == null) {
                        break;
                    }

                    exclusiveStartKey = lastEvaluatedKey.get("id").getS();
                }

                boolean ok = foundEvenNumberCount == actualEvenNumberCount;
                if (!ok) {
                    System.out.printf("Page size: %d, ok: %b\n", pageSize, ok);
                }
            }
        }
    }
}
