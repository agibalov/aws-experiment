package io.agibalov;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

@SpringBootApplication
public class App {
    public static void main(String[] args) {
        SpringApplication.run(App.class, args);
    }

    @Bean
    public DummyController dummyController(NamedParameterJdbcTemplate jdbcTemplate) {
        return new DummyController(jdbcTemplate);
    }
}
