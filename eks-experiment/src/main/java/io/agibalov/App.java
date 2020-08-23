package io.agibalov;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@Slf4j
@SpringBootApplication
public class App {
    public static void main(String[] args) {
        SpringApplication.run(App.class, args);
    }

    @Bean
    public AmazonS3 s3() {
        return AmazonS3ClientBuilder.standard()
                .build();
    }

    @Bean
    public DummyController dummyController(AmazonS3 s3) {
        return new DummyController(s3);
    }
}
