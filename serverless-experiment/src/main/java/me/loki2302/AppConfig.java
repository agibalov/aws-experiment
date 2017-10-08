package me.loki2302;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.datamodeling.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import java.util.Collections;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

@Configuration
@ComponentScan
@EnableWebMvc
@EnableConfigurationProperties
public class AppConfig {
    @Bean
    public AppProperties appProperties() {
        return new AppProperties();
    }

    @Bean
    @Autowired
    public DynamoDBTableMapper<TodoItem, String, ?> todoItemTableMapper(AppProperties appProperties) {
        AmazonDynamoDB amazonDynamoDB = AmazonDynamoDBClientBuilder.defaultClient();
        DynamoDBMapperConfig dynamoDBMapperConfig = DynamoDBMapperConfig.builder()
                .withTableNameOverride(new DynamoDBMapperConfig.TableNameOverride(appProperties.getTodosTableName()))
                .build();
        DynamoDBMapper dynamoDBMapper = new DynamoDBMapper(amazonDynamoDB, dynamoDBMapperConfig);
        DynamoDBTableMapper<TodoItem, String, ?> todoItemTableMapper =
                dynamoDBMapper.newTableMapper(TodoItem.class);
        return todoItemTableMapper;
    }

    @RestController
    @CrossOrigin
    public class DummyController {
        @Autowired
        private AppProperties appProperties;

        @Autowired
        private DynamoDBTableMapper<TodoItem, String, ?> todoItemTableMapper;

        @GetMapping("/test")
        public Map<String, String> index() {
            TodoItem todoItem = new TodoItem();
            todoItem.id = UUID.randomUUID().toString();
            todoItem.text = String.format("Todo %s", new Date());
            todoItemTableMapper.save(todoItem);

            int count = todoItemTableMapper.count(new DynamoDBScanExpression());
            String message = String.format("hello there! %s %s. Count: %d",
                    new Date(),
                    appProperties.getMessageSuffix(),
                    count);
            return Collections.singletonMap("message", message);
        }
    }

    @ConfigurationProperties("app")
    @NoArgsConstructor
    @Data
    public static class AppProperties {
        private String messageSuffix;
        private String todosTableName;
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
