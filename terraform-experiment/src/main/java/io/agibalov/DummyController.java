package io.agibalov;

import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.time.Instant;
import java.util.UUID;

@RequestMapping("/")
public class DummyController {
    private final NamedParameterJdbcTemplate jdbcTemplate;

    public DummyController(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @GetMapping
    public ResponseEntity<?> index() {
        String id = UUID.randomUUID().toString();
        jdbcTemplate.update(
                "insert into Notes(id, text) values(:id, :text)",
                new MapSqlParameterSource()
                        .addValue("id", id)
                        .addValue("text", String.format("Note #%s", id)));
        int count = jdbcTemplate.queryForObject(
                "select count(*) from Notes",
                new MapSqlParameterSource(),
                Integer.class);
        return ResponseEntity.ok(MessageDto.builder()
                .message(String.format("Hello world! %s (count=%d)", Instant.now(), count))
                .build());
    }
}
