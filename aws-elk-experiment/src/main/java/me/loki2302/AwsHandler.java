package me.loki2302;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.boot.Banner;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;

public class AwsHandler {
    private static ConfigurableApplicationContext context;

    public void handle() {
        if(context == null) {
            context = new SpringApplicationBuilder(App.class)
                    .bannerMode(Banner.Mode.OFF)
                    .run();
        }

        DummyService dummyService = context.getBean(DummyService.class);
        dummyService.doSomething();
    }

    @SpringBootApplication
    @EnableConfigurationProperties(AppProperties.class)
    public static class App {
        @Bean
        public AppProperties appProperties() {
            return new AppProperties();
        }

        @Bean
        public DummyService dummyService(AppProperties appProperties) {
            return new DummyService(appProperties);
        }
    }

    public static class DummyService {
        private final static Logger LOGGER = LoggerFactory.getLogger(DummyService.class);

        private final AppProperties appProperties;

        public DummyService(AppProperties appProperties) {
            this.appProperties = appProperties;
        }

        public void doSomething() {
            MDC.put("appName", appProperties.getName());
            try {

                LOGGER.info("Hello world!!!");

                try {
                    int x = 1 / 0;
                } catch (Throwable t) {
                    LOGGER.error("Something terrible has happened", t);
                }
            } finally {
                MDC.remove("appName");
            }
        }
    }

    @ConfigurationProperties("app")
    public static class AppProperties {
        private String name;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }
}
