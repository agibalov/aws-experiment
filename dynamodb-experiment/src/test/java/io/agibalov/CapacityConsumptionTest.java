package io.agibalov;

import com.amazonaws.AmazonWebServiceRequest;
import com.amazonaws.Request;
import com.amazonaws.Response;
import com.amazonaws.handlers.RequestHandler2;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.datamodeling.*;
import com.amazonaws.services.dynamodbv2.model.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.junit.ClassRule;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashMap;

public class CapacityConsumptionTest {
    private final static Logger LOGGER = LoggerFactory.getLogger(CapacityConsumptionTest.class);

    @ClassRule
    public static AmazonDynamoDBProvider amazonDynamoDBProvider = new AmazonDynamoDBProvider();

    @Test
    public void capacityConsumptionTracingShouldWork() throws IOException {
        AmazonDynamoDB amazonDynamoDB = amazonDynamoDBProvider.getAmazonDynamoDB()
                .withRequestHandlers(new RequestHandler2() {
                    @Override
                    public AmazonWebServiceRequest beforeExecution(AmazonWebServiceRequest request) {
                        if(request instanceof UpdateItemRequest) {
                            UpdateItemRequest updateItemRequest = (UpdateItemRequest)request;
                            updateItemRequest.setReturnConsumedCapacity(ReturnConsumedCapacity.TOTAL);
                        } else if(request instanceof ScanRequest) {
                            ScanRequest scanRequest = (ScanRequest)request;
                            scanRequest.setReturnConsumedCapacity(ReturnConsumedCapacity.TOTAL);
                        } else if(request instanceof QueryRequest) {
                            QueryRequest queryRequest = (QueryRequest)request;
                            queryRequest.setReturnConsumedCapacity(ReturnConsumedCapacity.TOTAL);
                        }
                        return request;
                    }

                    @Override
                    public void afterResponse(Request<?> request, Response<?> response) {
                        Object awsResponse = response.getAwsResponse();
                        if(awsResponse instanceof UpdateItemResult) {
                            UpdateItemResult updateItemResult = (UpdateItemResult)awsResponse;
                            LOGGER.info("PutItem capacity consumption: {}", updateItemResult.getConsumedCapacity());
                        } else if(awsResponse instanceof ScanResult) {
                            ScanResult scanResult = (ScanResult)awsResponse;
                            LOGGER.info("Scan capacity consumption: {}", scanResult.getConsumedCapacity());
                        } else if(awsResponse instanceof QueryResult) {
                            QueryResult queryResult = (QueryResult)awsResponse;
                            LOGGER.info("Query capacity consumption: {}", queryResult.getConsumedCapacity());
                        }
                    }
                })
                .build();
        DynamoDBMapperConfig dynamoDBMapperConfig = DynamoDBMapperConfig.builder()
                .withTableNameOverride(new DynamoDBMapperConfig.TableNameOverride("dummy2"))
                .build();
        DynamoDBMapper dynamoDBMapper = new DynamoDBMapper(amazonDynamoDB, dynamoDBMapperConfig);

        try(DynamoDbTableResource ignore = new DynamoDbTableResource(amazonDynamoDB, dynamoDBMapper
                .generateCreateTableRequest(Order.class)
                .withProvisionedThroughput(new ProvisionedThroughput()
                        .withWriteCapacityUnits(1L)
                        .withReadCapacityUnits(1L)))) {

            DynamoDBTableMapper<Order, String, String> orderTableMapper =
                    dynamoDBMapper.newTableMapper(Order.class);

            for(int userIndex = 0; userIndex < 5; ++userIndex) {
                String userId = String.format("user%d", userIndex);
                for(int timestampIndex = 0; timestampIndex < 3; ++timestampIndex) {
                    Order order = new Order();
                    order.setUserId(userId);
                    order.setOrderTime(String.format("%d", timestampIndex));
                    order.setOrderTotal(userIndex * 10 + timestampIndex);
                    order.setDescription(String.format("user %d timestamp %d", userIndex, timestampIndex));
                    orderTableMapper.save(order);
                }
            }

            orderTableMapper.scan(new DynamoDBScanExpression());

            orderTableMapper.query(new DynamoDBQueryExpression<Order>()
                    .withKeyConditionExpression("userId = :userId")
                    .withExpressionAttributeValues(new HashMap<String, AttributeValue>() {{
                        put(":userId", new AttributeValue().withS("user3"));
                    }}));
        }
    }

    @DynamoDBTable(tableName = "A DUMMY VALUE TO BE OVERRIDDEN BY CONFIGURATION")
    @NoArgsConstructor
    @Data
    public static class Order {
        @DynamoDBHashKey
        private String userId;

        @DynamoDBRangeKey
        private String orderTime;

        @DynamoDBIndexRangeKey(localSecondaryIndexName = "OrderTotalLocalIndex")
        private int orderTotal;

        @DynamoDBAttribute
        private String description;
    }
}
