package io.agibalov;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.model.*;
import com.amazonaws.util.ImmutableMapParameter;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;

public class DummyTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(DummyTest.class);
    private static final String DUMMY_TABLE_NAME = "dummy1";

    @Test
    public void dummy() throws InterruptedException {
        AmazonDynamoDB amazonDynamoDB = AmazonDynamoDBClientBuilder.standard()
                .withRegion(Regions.US_EAST_1)
                .build();

        String data = makeData(10 * 1024);

        Thread dataWriterThread = new Thread(() -> {
            while(true) {
                try {
                    PutItemResult putItemResult = amazonDynamoDB.putItem(new PutItemRequest()
                            .withTableName(DUMMY_TABLE_NAME)
                            .withReturnConsumedCapacity(ReturnConsumedCapacity.TOTAL)
                            .withItem(ImmutableMapParameter.<String, AttributeValue>builder()
                                    .put("id", new AttributeValue().withS("x"))
                                    .put("data", new AttributeValue().withS(data))
                                    .build()));

                    LOGGER.info("putItem() consumed={}", putItemResult.getConsumedCapacity().getCapacityUnits());
                } catch (Throwable t) {
                    LOGGER.warn("putItem() failed", t);
                }

                try {
                    Thread.sleep(Duration.ofMillis(1000).toMillis());
                } catch (InterruptedException e) {
                    break;
                }
            }
        }, "data-writer");

        Thread wcuPollerThread = new Thread(() -> {
            while(true) {
                DescribeTableResult describeTableResult = amazonDynamoDB.describeTable(DUMMY_TABLE_NAME);
                TableDescription tableDescription = describeTableResult.getTable();
                ProvisionedThroughputDescription provisionedThroughput = tableDescription.getProvisionedThroughput();

                LOGGER.info("Provisioned WCU: {} (last descrease={}, last increase={})",
                        provisionedThroughput.getWriteCapacityUnits(),
                        provisionedThroughput.getLastDecreaseDateTime(),
                        provisionedThroughput.getLastIncreaseDateTime());

                try {
                    Thread.sleep(Duration.ofMinutes(1).toMillis());
                } catch (InterruptedException e) {
                    break;
                }
            }
        }, "wcu-poller");

        dataWriterThread.start();
        wcuPollerThread.start();

        Instant timeToStop = Instant.now().plus(Duration.ofMinutes(30));
        while(true) {
            Duration remainingTimeDuration = Duration.between(Instant.now(), timeToStop);
            if(remainingTimeDuration.isNegative()) {
                break;
            }
            LOGGER.info("{} left...", remainingTimeDuration);
            Thread.sleep(Duration.ofMinutes(1).toMillis());
        }
        LOGGER.info("Shutting down...");

        dataWriterThread.interrupt();
        wcuPollerThread.interrupt();

        dataWriterThread.join();
        wcuPollerThread.join();
    }

    private static String makeData(int numberOfCharacters) {
        StringBuilder sb = new StringBuilder();
        for(int i = 0; i < numberOfCharacters; ++i) {
            sb.append('x');
        }
        return sb.toString();
    }
}
