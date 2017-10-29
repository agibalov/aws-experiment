package me.loki2302;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.datamodeling.*;
import com.amazonaws.services.dynamodbv2.model.ProvisionedThroughput;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.junit.ClassRule;
import org.junit.Test;

import java.io.IOException;
import java.time.Instant;

import static org.junit.Assert.assertEquals;

public class DynamoDbMapperCustomTypeTest {
    private final static String TEST_TABLE_NAME = "dummy2";

    @ClassRule
    public static AmazonDynamoDBProvider amazonDynamoDBProvider = new AmazonDynamoDBProvider();

    @Test
    public void canUseCustomTypeConverter() throws IOException {
        AmazonDynamoDB amazonDynamoDB = amazonDynamoDBProvider.getAmazonDynamoDB();
        DynamoDBMapperConfig dynamoDBMapperConfig = DynamoDBMapperConfig.builder()
                .withTableNameOverride(new DynamoDBMapperConfig.TableNameOverride(TEST_TABLE_NAME))
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
            todoItem.setMyInstant(Instant.parse("1986-02-23T12:34:56Z"));
            todoItemTableMapper.save(todoItem);

            TodoItem loadedTodoItem = todoItemTableMapper.load("id1");
            assertEquals("1986-02-23T12:34:56Z", loadedTodoItem.getMyInstant().toString());
        }
    }

    @Test
    public void canUseStandardJsonConverter() throws IOException {
        AmazonDynamoDB amazonDynamoDB = amazonDynamoDBProvider.getAmazonDynamoDB();
        DynamoDBMapperConfig dynamoDBMapperConfig = DynamoDBMapperConfig.builder()
                .withTableNameOverride(new DynamoDBMapperConfig.TableNameOverride(TEST_TABLE_NAME))
                .build();
        DynamoDBMapper dynamoDBMapper = new DynamoDBMapper(amazonDynamoDB, dynamoDBMapperConfig);

        try(DynamoDbTableResource ignore = new DynamoDbTableResource(amazonDynamoDB, dynamoDBMapper
                .generateCreateTableRequest(TodoItem.class)
                .withProvisionedThroughput(new ProvisionedThroughput()
                        .withWriteCapacityUnits(1L)
                        .withReadCapacityUnits(1L)))) {

            DynamoDBTableMapper<TodoItem, String, ?> todoItemTableMapper =
                    dynamoDBMapper.newTableMapper(TodoItem.class);

            MyDocument myDocument = new MyDocument();
            myDocument.setSomeNumber(2302);
            myDocument.setSomeString("hello there");

            TodoItem todoItem = new TodoItem();
            todoItem.setId("id1");
            todoItem.setMyDocument(myDocument);
            todoItemTableMapper.save(todoItem);

            TodoItem loadedTodoItem = todoItemTableMapper.load("id1");
            assertEquals(2302, loadedTodoItem.getMyDocument().getSomeNumber());
            assertEquals("hello there", loadedTodoItem.getMyDocument().getSomeString());
        }
    }

    @Test
    public void canUseStandardEnumConverter() throws IOException {
        AmazonDynamoDB amazonDynamoDB = amazonDynamoDBProvider.getAmazonDynamoDB();
        DynamoDBMapperConfig dynamoDBMapperConfig = DynamoDBMapperConfig.builder()
                .withTableNameOverride(new DynamoDBMapperConfig.TableNameOverride(TEST_TABLE_NAME))
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
            todoItem.setMyStatus(MyStatus.IN_PROGRESS);
            todoItemTableMapper.save(todoItem);

            TodoItem loadedTodoItem = todoItemTableMapper.load("id1");
            assertEquals(MyStatus.IN_PROGRESS, loadedTodoItem.getMyStatus());
        }
    }

    @DynamoDBTable(tableName = "A DUMMY VALUE TO BE OVERRIDDEN BY CONFIGURATION")
    @NoArgsConstructor
    @Data
    public static class TodoItem {
        @DynamoDBHashKey
        private String id;

        @DynamoDBAttribute
        @DynamoDBTypeConverted(converter = InstantAsLongTypeConverter.class)
        private Instant myInstant;

        @DynamoDBAttribute
        @DynamoDBTypeConvertedJson
        private MyDocument myDocument;

        @DynamoDBAttribute
        @DynamoDBTypeConvertedEnum
        private MyStatus myStatus;
    }

    public enum MyStatus {
        NOT_STARTED,
        IN_PROGRESS,
        DONE
    }

    @NoArgsConstructor
    @Data
    public static class MyDocument {
        private String someString;
        private int someNumber;
    }

    public static class InstantAsLongTypeConverter implements DynamoDBTypeConverter<Long, Instant> {
        @Override
        public Long convert(Instant object) {
            if(object == null) {
                return null;
            }
            return object.toEpochMilli();
        }

        @Override
        public Instant unconvert(Long object) {
            if(object == null) {
                return null;
            }
            return Instant.ofEpochMilli(object);
        }
    }
}
