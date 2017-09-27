package me.loki2302.integration;

import me.loki2302.Note;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
public class AppTest {
    @Test
    public void theAppsHelloEndpointShouldWork() {
        RestTemplate restTemplate = new RestTemplate();
        Map<String, String> body = restTemplate.getForObject("http://localhost:8080/hello", Map.class);
        assertTrue(body.get("message").contains("Hello there"));
    }

    @Test
    public void theAppsItemsEndpointShouldWork() {
        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<List<Note>> responseEntity = restTemplate.exchange(
                "http://localhost:8080/items",
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<List<Note>>() {});
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
    }
}
