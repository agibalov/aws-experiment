package io.agibalov;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.time.Instant;

@RequestMapping("/")
public class DummyController {
    @GetMapping
    public ResponseEntity<?> index() {
        return ResponseEntity.ok(MessageDto.builder()
                .message(String.format("Hello world! %s", Instant.now()))
                .build());
    }
}
