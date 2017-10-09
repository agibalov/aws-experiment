package me.loki2302;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.client.RestTemplate;
import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.service.Contact;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collections;

@SpringBootTest(properties = {
        "APP_MESSAGE_SUFFIX=aaa",
        "APP_TODOS_TABLE_NAME=omg"
}, webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@RunWith(SpringRunner.class)
public class DocTest {
    @Test
    public void dummy() throws InterruptedException, IOException {
        RestTemplate restTemplate = new RestTemplate();
        String json = restTemplate.getForObject("http://localhost:8080/v2/api-docs", String.class);
        Files.write(Paths.get("build/api.json"), json.getBytes());
    }

    @SpringBootApplication
    @EnableSwagger2
    @Import(AppConfig.class)
    public static class Config {
        @Bean
        public Docket api() {
            return new Docket(DocumentationType.SWAGGER_2)
                    .apiInfo(apiInfo())
                    .protocols(Collections.singleton("https"))
                    .useDefaultResponseMessages(false)
                    .select()
                    .paths(s -> !s.startsWith("/error"))
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
    }
}
