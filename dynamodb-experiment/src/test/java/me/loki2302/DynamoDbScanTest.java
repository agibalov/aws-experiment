package me.loki2302;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.model.*;
import org.junit.ClassRule;
import org.junit.Test;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.Assert.assertEquals;

public class DynamoDbScanTest {
    private final static String TEST_TABLE_NAME = "test1";

    @ClassRule
    public static AmazonDynamoDBProvider amazonDynamoDBProvider = new AmazonDynamoDBProvider();

    @Test
    public void dummy() throws InterruptedException, IOException {
        AmazonDynamoDB amazonDynamoDB = amazonDynamoDBProvider.getAmazonDynamoDB();

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

            for(int i = 0; i < 50; ++i) {
                String id = UUID.randomUUID().toString();
                String text = Stream.iterate(0, x -> x + 1)
                        .limit(10000)
                        .map(x -> "0123456789")
                        .collect(Collectors.joining(""));

                Map<String, AttributeValue> attributeValues = new HashMap<>();
                attributeValues.put("id", new AttributeValue().withS(id));
                attributeValues.put("text", new AttributeValue().withS(text));
                PutItemRequest putItemRequest = new PutItemRequest()
                        .withTableName(TEST_TABLE_NAME)
                        .withItem(attributeValues);
                amazonDynamoDB.putItem(putItemRequest);

                if(i % 10 == 0) {
                    System.out.printf("%d\n", i);
                }
            }

            ScanResult scanResult = null;
            int pageCount = 0;
            do {
                ++pageCount;

                ScanRequest scanRequest = new ScanRequest(TEST_TABLE_NAME);
                if(scanResult != null && scanResult.getLastEvaluatedKey() != null) {
                    scanRequest = scanRequest.withExclusiveStartKey(scanResult.getLastEvaluatedKey());
                }

                scanResult = amazonDynamoDB.scan(scanRequest);
                System.out.printf("Scan page %d: %s\n", pageCount, scanResult.getLastEvaluatedKey());
            } while(scanResult.getLastEvaluatedKey() != null);

            assertEquals(5, pageCount);
        }
    }
}
