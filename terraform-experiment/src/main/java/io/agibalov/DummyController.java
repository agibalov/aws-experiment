package io.agibalov;

import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.time.Instant;

@RequestMapping("/")
public class DummyController {
    private final NamedParameterJdbcTemplate jdbcTemplate;

    public DummyController(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @GetMapping
    public ResponseEntity<?> index() {
        int result = jdbcTemplate.queryForObject("select 2 + 3", new MapSqlParameterSource(), Integer.class);
        return ResponseEntity.ok(MessageDto.builder()
                .message(String.format("Hello world! %s (%d)", Instant.now(), result))
                .build());
    }
}
