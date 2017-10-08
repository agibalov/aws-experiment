package me.loki2302;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.datamodeling.*;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiOperation;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.service.Contact;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

import java.util.Date;
import java.util.UUID;

@Configuration
@EnableWebMvc
@EnableConfigurationProperties
@EnableSwagger2
public class AppConfig {
    @Bean
    public AppProperties appProperties() {
        return new AppProperties();
    }

    @Bean
    public Docket api() {
        return new Docket(DocumentationType.SWAGGER_2)
                .apiInfo(apiInfo())
                .useDefaultResponseMessages(false)
                .select()
                .build();
    }

    @Bean
    public ApiInfo apiInfo() {
        return new ApiInfoBuilder()
                .title("My API title")
                .contact(new Contact(
                        "loki2302",
                        "http://loki2302.me",
                        "loki2302@loki2302.me"))
                .description("My API description")
                .license("My API license")
                .licenseUrl("http://retask.me/license")
                .termsOfServiceUrl("http://retask.me/tos")
                .version("My API version")
                .build();
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
    @Api(value = "Dummy controller", description = "Some dummy controller")
    public static class DummyController {
        @Autowired
        private AppProperties appProperties;

        @Autowired
        private DynamoDBTableMapper<TodoItem, String, ?> todoItemTableMapper;

        @GetMapping("/test")
        @ApiOperation(value = "Do test", notes = "Do some stuff, provide some response")
        public TestResponseDto index() {
            TodoItem todoItem = new TodoItem();
            todoItem.id = UUID.randomUUID().toString();
            todoItem.text = String.format("Todo %s", new Date());
            todoItemTableMapper.save(todoItem);

            int count = todoItemTableMapper.count(new DynamoDBScanExpression());
            String message = String.format("hello there! %s %s. Count: %d",
                    new Date(),
                    appProperties.getMessageSuffix(),
                    count);

            TestResponseDto testResponseDto = new TestResponseDto();
            testResponseDto.setMessage(message);
            return testResponseDto;
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

    @ApiModel("TestResponse")
    @NoArgsConstructor
    @Data
    public static class TestResponseDto {
        private String message;
    }
}
