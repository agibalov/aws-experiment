package me.loki2302;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.datamodeling.*;
import com.amazonaws.services.dynamodbv2.model.ProvisionedThroughput;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class DynamoDbMapperTest {
    @Test
    public void dynamoDbMapperShouldWork() throws IOException {
        AmazonDynamoDB amazonDynamoDB = AmazonDynamoDBClientBuilder.defaultClient();
        DynamoDBMapperConfig dynamoDBMapperConfig = DynamoDBMapperConfig.builder()
                .withTableNameOverride(new DynamoDBMapperConfig.TableNameOverride("dummy1"))
                .build();
        DynamoDBMapper dynamoDBMapper = new DynamoDBMapper(amazonDynamoDB, dynamoDBMapperConfig);

        try(DynamoDbTableResource ignore = new DynamoDbTableResource(amazonDynamoDB, dynamoDBMapper
                .generateCreateTableRequest(TodoItem.class)
                .withProvisionedThroughput(new ProvisionedThroughput()
                        .withWriteCapacityUnits(1L)
                        .withReadCapacityUnits(1L)))) {

            TodoItem todoItem = new TodoItem();
            todoItem.setId("id1");
            todoItem.setText("hello there!");
            dynamoDBMapper.save(todoItem);

            TodoItem loadedTodoItem = dynamoDBMapper.load(TodoItem.class, "id1");
            assertEquals("hello there!", loadedTodoItem.getText());

            TodoItem todoItemThatDoesNotExist = dynamoDBMapper.load(TodoItem.class, "id2");
            assertNull(todoItemThatDoesNotExist);
        }
    }

    @Test
    public void dynamoDbTableMapperShouldWork() throws IOException {
        AmazonDynamoDB amazonDynamoDB = AmazonDynamoDBClientBuilder.defaultClient();
        DynamoDBMapperConfig dynamoDBMapperConfig = DynamoDBMapperConfig.builder()
                .withTableNameOverride(new DynamoDBMapperConfig.TableNameOverride("dummy2"))
                .build();
        DynamoDBMapper dynamoDBMapper = new DynamoDBMapper(amazonDynamoDB, dynamoDBMapperConfig);

        try(DynamoDbTableResource ignore = new DynamoDbTableResource(amazonDynamoDB, dynamoDBMapper
                .generateCreateTableRequest(TodoItem.class)
                .withProvisionedThroughput(new ProvisionedThroughput()
                        .withWriteCapacityUnits(1L)
                        .withReadCapacityUnits(1L)))) {

            DynamoDBTableMapper<TodoItem, String, ?> todoItemTableMapper =
                    dynamoDBMapper.newTableMapper(TodoItem.class);

            TodoItem todoItem = new TodoItem();
            todoItem.setId("id1");
            todoItem.setText("hello there!");
            todoItemTableMapper.save(todoItem);

            TodoItem loadedTodoItem = todoItemTableMapper.load("id1");
            assertEquals("hello there!", loadedTodoItem.getText());

            TodoItem todoItemThatDoesNotExist = todoItemTableMapper.load("id2");
            assertNull(todoItemThatDoesNotExist);
        }
    }

    @DynamoDBTable(tableName = "A DUMMY VALUE TO BE OVERRIDDEN BY CONFIGURATION")
    @NoArgsConstructor
    @Data
    public static class TodoItem {
        @DynamoDBHashKey
        private String id;

        @DynamoDBAttribute
        private String text;
    }
}
