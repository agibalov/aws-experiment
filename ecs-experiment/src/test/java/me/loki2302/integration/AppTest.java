package me.loki2302.integration;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

import static org.junit.Assert.assertTrue;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
public class AppTest {
    @Test
    public void theAppShouldWork() {
        RestTemplate restTemplate = new RestTemplate();
        Map<String, String> body = restTemplate.getForObject("http://localhost:8080/hello", Map.class);
        assertTrue(body.get("message").contains("Hello there"));
    }
}
