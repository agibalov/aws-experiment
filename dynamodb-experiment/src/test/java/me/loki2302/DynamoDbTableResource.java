package me.loki2302;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.model.CreateTableRequest;
import com.amazonaws.services.dynamodbv2.model.DeleteTableRequest;
import com.amazonaws.services.dynamodbv2.model.DescribeTableRequest;
import com.amazonaws.waiters.FixedDelayStrategy;
import com.amazonaws.waiters.MaxAttemptsRetryStrategy;
import com.amazonaws.waiters.PollingStrategy;
import com.amazonaws.waiters.WaiterParameters;

import java.io.Closeable;
import java.io.IOException;

public class DynamoDbTableResource implements Closeable {
    private final static PollingStrategy POLLING_STRATEGY = new PollingStrategy(
            new MaxAttemptsRetryStrategy(100),
            new FixedDelayStrategy(1));
    private final AmazonDynamoDB amazonDynamoDB;
    private final CreateTableRequest createTableRequest;

    public DynamoDbTableResource(
            AmazonDynamoDB amazonDynamoDB,
            CreateTableRequest createTableRequest) {

        this.amazonDynamoDB = amazonDynamoDB;
        this.createTableRequest = createTableRequest;

        amazonDynamoDB.createTable(createTableRequest);
        amazonDynamoDB.waiters().tableExists().run(
                new WaiterParameters<DescribeTableRequest>()
                        .withRequest(new DescribeTableRequest()
                                .withTableName(createTableRequest.getTableName()))
                        .withPollingStrategy(POLLING_STRATEGY));
    }

    @Override
    public void close() throws IOException {
        DeleteTableRequest deleteTableRequest = new DeleteTableRequest()
                .withTableName(createTableRequest.getTableName());
        amazonDynamoDB.deleteTable(deleteTableRequest);
        amazonDynamoDB.waiters().tableNotExists().run(
                new WaiterParameters<DescribeTableRequest>()
                    .withRequest(new DescribeTableRequest()
                            .withTableName(createTableRequest.getTableName()))
                    .withPollingStrategy(POLLING_STRATEGY));
    }
}
