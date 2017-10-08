package me.loki2302;

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

@Configuration
@ComponentScan
@EnableWebMvc
@EnableConfigurationProperties
public class AppConfig {
    @Bean
    public AppProperties appProperties() {
        return new AppProperties();
    }

    @RestController
    @CrossOrigin
    public class DummyController {
        @Autowired
        private AppProperties appProperties;

        @GetMapping("/test")
        public Map<String, String> index() {
            String message = "hello there " + new Date() + " " + appProperties.getMessageSuffix();
            return Collections.singletonMap("message", message);
        }
    }

    @ConfigurationProperties("app")
    public static class AppProperties {
        private String messageSuffix;

        public String getMessageSuffix() {
            return messageSuffix;
        }

        public void setMessageSuffix(String messageSuffix) {
            this.messageSuffix = messageSuffix;
        }
    }
}
