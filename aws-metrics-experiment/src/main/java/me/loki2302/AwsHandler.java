package me.loki2302;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.Banner;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;

import java.util.Random;

import static net.logstash.logback.argument.StructuredArguments.keyValue;

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
    public static class App {
        @Bean
        public DummyService dummyService() {
            return new DummyService();
        }
    }

    public static class DummyService {
        private final static Logger LOGGER = LoggerFactory.getLogger(DummyService.class);
        private final Random random = new Random();

        public void doSomething() {
            LOGGER.info("Hello world!!!");

            try {
                int x = 1 / 0;
            } catch (Throwable t) {
                LOGGER.error("Something terrible has happened", t);
            }

            LOGGER.info("SomeStaticValue", keyValue("value", 7));
            LOGGER.info("SomeRandomValue", keyValue("value", 11 + random.nextInt() % 5));
        }
    }
}
