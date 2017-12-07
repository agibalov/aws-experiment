package me.loki2302;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.datamodeling.*;
import com.amazonaws.services.dynamodbv2.model.ProvisionedThroughput;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.junit.ClassRule;
import org.junit.Test;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class DynamoDbMapperNestedListTest {
    @ClassRule
    public static AmazonDynamoDBProvider amazonDynamoDBProvider = new AmazonDynamoDBProvider();

    @Test
    public void dynamoDbMapperShouldWork() throws IOException {
        AmazonDynamoDB amazonDynamoDB = amazonDynamoDBProvider.getAmazonDynamoDB();
        DynamoDBMapperConfig dynamoDBMapperConfig = DynamoDBMapperConfig.builder()
                .withTableNameOverride(new DynamoDBMapperConfig.TableNameOverride("dummy1"))
                .build();
        DynamoDBMapper dynamoDBMapper = new DynamoDBMapper(amazonDynamoDB, dynamoDBMapperConfig);

        try(DynamoDbTableResource ignore = new DynamoDbTableResource(amazonDynamoDB, dynamoDBMapper
                .generateCreateTableRequest(ShoppingList.class)
                .withProvisionedThroughput(new ProvisionedThroughput()
                        .withWriteCapacityUnits(1L)
                        .withReadCapacityUnits(1L)))) {

            ShoppingList shoppingList = new ShoppingList();
            shoppingList.setId("id1");
            shoppingList.setItems(Arrays.asList(
                    new ShoppingListItem("id1", "name1"),
                    new ShoppingListItem("id2", "name2")));
            dynamoDBMapper.save(shoppingList);

            ShoppingList loadedShoppingList = dynamoDBMapper.load(ShoppingList.class, "id1");
            assertEquals("id1", loadedShoppingList.getId());
            assertEquals(2, loadedShoppingList.getItems().size());
            assertEquals("id1", loadedShoppingList.getItems().get(0).getId());
            assertEquals("name1", loadedShoppingList.getItems().get(0).getName());
            assertEquals("id2", loadedShoppingList.getItems().get(1).getId());
            assertEquals("name2", loadedShoppingList.getItems().get(1).getName());
        }
    }

    @DynamoDBTable(tableName = "A DUMMY VALUE TO BE OVERRIDDEN BY CONFIGURATION")
    @NoArgsConstructor
    @Data
    public static class ShoppingList {
        @DynamoDBHashKey
        private String id;

        @DynamoDBAttribute
        private List<ShoppingListItem> items;
    }

    @NoArgsConstructor
    @Data
    @AllArgsConstructor
    @DynamoDBDocument
    public static class ShoppingListItem {
        private String id;
        private String name;
    }
}
