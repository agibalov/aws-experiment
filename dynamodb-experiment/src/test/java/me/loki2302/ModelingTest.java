package me.loki2302;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.datamodeling.*;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.ProvisionedThroughput;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.junit.ClassRule;
import org.junit.Test;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ModelingTest {
    @ClassRule
    public static AmazonDynamoDBProvider amazonDynamoDBProvider = new AmazonDynamoDBProvider();

    @Test
    public void modelingShouldWork() throws IOException {
        AmazonDynamoDB amazonDynamoDB = amazonDynamoDBProvider.getAmazonDynamoDB();
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
            OrderService orderService = new OrderService(orderTableMapper);

            for(int userIndex = 0; userIndex < 5; ++userIndex) {
                String userId = String.format("user%d", userIndex);
                for(int timestampIndex = 0; timestampIndex < 3; ++timestampIndex) {
                    Order order = new Order();
                    order.setUserId(userId);
                    order.setOrderTime(String.format("%d", timestampIndex));
                    order.setDescription(String.format("user %d timestamp %d", userIndex, timestampIndex));
                    orderService.createOrder(order);
                }
            }

            // Should find all orders
            {
                List<Order> orders = orderService.findAllOrders();
                assertEquals(15, orders.size());
            }

            // Should not find any orders when user does not exist
            {
                List<Order> orders = orderService.findOrdersByUserId("thisUserDoesNotExist");
                assertEquals(0, orders.size());
            }

            // Should find all user's orders when user exists
            {
                List<Order> orders = orderService.findOrdersByUserId("user3");
                assertEquals(3, orders.size());
                assertTrue(orders.stream().anyMatch(o -> o.getUserId().equals("user3") && o.getOrderTime().equals("0")));
                assertTrue(orders.stream().anyMatch(o -> o.getUserId().equals("user3") && o.getOrderTime().equals("1")));
                assertTrue(orders.stream().anyMatch(o -> o.getUserId().equals("user3") && o.getOrderTime().equals("2")));
            }

            // Should find all user's orders that match the sort key criterion
            {
                List<Order> orders = orderService.findOrdersByUserIdAndOrderTimeAfter("user3", "0");
                assertEquals(2, orders.size());
                assertTrue(orders.stream().anyMatch(o -> o.getUserId().equals("user3") && o.getOrderTime().equals("1")));
                assertTrue(orders.stream().anyMatch(o -> o.getUserId().equals("user3") && o.getOrderTime().equals("2")));
            }

            // find first order
            {
                Order order = orderService.findFirstOrderByUserId("user3");
                assertEquals("0", order.getOrderTime());
            }

            // find last order
            {
                Order order = orderService.findLastOrderByUserId("user3");
                assertEquals("2", order.getOrderTime());
            }
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

        @DynamoDBAttribute
        private String description;
    }

    public static class OrderService {
        private final DynamoDBTableMapper<Order, String, String> orderTableMapper;

        public OrderService(DynamoDBTableMapper<Order, String, String> orderTableMapper) {
            this.orderTableMapper = orderTableMapper;
        }

        public void createOrder(Order order) {
            orderTableMapper.save(order);
        }

        public List<Order> findAllOrders() {
            return orderTableMapper.scan(new DynamoDBScanExpression());
        }

        public List<Order> findOrdersByUserId(String userId) {
            return orderTableMapper.query(new DynamoDBQueryExpression<Order>()
                    .withKeyConditionExpression("userId = :userId")
                    .withExpressionAttributeValues(new HashMap<String, AttributeValue>() {{
                        put(":userId", new AttributeValue().withS(userId));
                    }}));
        }

        public List<Order> findOrdersByUserIdAndOrderTimeAfter(String userId, String orderTime) {
            return orderTableMapper.query(new DynamoDBQueryExpression<Order>()
                    .withKeyConditionExpression("userId = :userId and orderTime > :orderTime")
                    .withExpressionAttributeValues(new HashMap<String, AttributeValue>() {{
                        put(":userId", new AttributeValue().withS(userId));
                        put(":orderTime", new AttributeValue().withS(orderTime));
                    }}));
        }

        public Order findFirstOrderByUserId(String userId) {
            List<Order> orders = orderTableMapper.query(new DynamoDBQueryExpression<Order>()
                    .withKeyConditionExpression("userId = :userId")
                    .withExpressionAttributeValues(new HashMap<String, AttributeValue>() {{
                        put(":userId", new AttributeValue().withS(userId));
                    }})
                    .withScanIndexForward(true)
                    .withLimit(1));
            return orders.get(0);
        }

        public Order findLastOrderByUserId(String userId) {
            List<Order> orders = orderTableMapper.query(new DynamoDBQueryExpression<Order>()
                    .withKeyConditionExpression("userId = :userId")
                    .withExpressionAttributeValues(new HashMap<String, AttributeValue>() {{
                        put(":userId", new AttributeValue().withS(userId));
                    }})
                    .withScanIndexForward(false)
                    .withLimit(1));
            return orders.get(0);
        }
    }
}
