package io.agibalov;

import lombok.SneakyThrows;
import org.springframework.boot.Banner;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.time.Instant;

@SpringBootApplication
public class App {
    public static void main(String[] args) {
        new SpringApplicationBuilder(App.class).bannerMode(Banner.Mode.OFF).run(args);
    }

    @Bean
    public DummyService dummyService(JdbcTemplate jdbcTemplate) {
        return new DummyService(jdbcTemplate);
    }

    @Bean
    public DummyController dummyController(DummyService dummyService) {
        return new DummyController(dummyService);
    }

    @RequestMapping
    public static class DummyController {
        private final DummyService dummyService;

        public DummyController(DummyService dummyService) {
            this.dummyService = dummyService;
        }

        @GetMapping
        public ResponseEntity<?> hello() {
            dummyService.doSomething();
            return ResponseEntity.ok(String.format("Hello %s", Instant.now()));
        }
    }

    public static class DummyService {
        private final JdbcTemplate jdbcTemplate;

        public DummyService(JdbcTemplate jdbcTemplate) {
            this.jdbcTemplate = jdbcTemplate;
        }

        @SneakyThrows
        public void doSomething() {
            jdbcTemplate.queryForObject("select 1 + 1", Integer.class);
            Thread.sleep(10);
        }
    }
}
