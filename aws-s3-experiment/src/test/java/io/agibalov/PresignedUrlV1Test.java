package io.agibalov;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.GeneratePresignedUrlRequest;
import com.amazonaws.services.s3.model.ResponseHeaderOverrides;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.util.IOUtils;
import io.agibalov.v1.S3BucketV1;
import org.junit.Rule;
import org.junit.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.sql.Date;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.junit.Assert.assertEquals;

public class PresignedUrlV1Test {
    @Rule
    public final AmazonS3Provider amazonS3Provider = new AmazonS3Provider();

    @Test
    public void canGenerateAPresignedDownloadLink() {
        AmazonS3 amazonS3 = amazonS3Provider.getAmazonS3();
        try (S3BucketV1 bucket = amazonS3Provider.getS3BucketV1()) {
            amazonS3.putObject(bucket.getBucketName(), "1.txt", "hello there");
            URL url = amazonS3.generatePresignedUrl(new GeneratePresignedUrlRequest(bucket.getBucketName(), "1.txt")
                    .withMethod(com.amazonaws.HttpMethod.GET)
                    .withResponseHeaders(new ResponseHeaderOverrides()
                            .withContentDisposition(String.format("attachment; filename=custom123.txt")))
                    .withExpiration(Date.from(Instant.now().plus(1, ChronoUnit.HOURS))));

            String urlString = url.toString();
            System.out.printf("URL string: %s\n", urlString);

            RestTemplate restTemplate = new RestTemplate();

            // Note: have to manually construct URL because RestTemplate does the weird things internally:
            // https://stackoverflow.com/a/47311387/852604
            ResponseEntity<String> responseEntity = restTemplate.getForEntity(URI.create(urlString), String.class);
            assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
            if (!(amazonS3Provider.getApiProvider() instanceof AmazonS3Provider.LocalStackApiProvider)) {
                // LocalStack doesn't support headers
                assertEquals("attachment", responseEntity.getHeaders().getContentDisposition().getType());
                assertEquals("custom123.txt", responseEntity.getHeaders().getContentDisposition().getFilename());
            }
            assertEquals("hello there", responseEntity.getBody());
        }
    }

    @Test
    public void canGenerateAPresignedUploadLink() throws IOException {
        AmazonS3 amazonS3 = amazonS3Provider.getAmazonS3();
        try (S3BucketV1 bucket = amazonS3Provider.getS3BucketV1()) {
            URL url = amazonS3.generatePresignedUrl(new GeneratePresignedUrlRequest(bucket.getBucketName(), "1.txt")
                    .withMethod(com.amazonaws.HttpMethod.PUT)
                    .withExpiration(Date.from(Instant.now().plus(1, ChronoUnit.HOURS))));

            String urlString = url.toString();
            System.out.printf("URL string: %s\n", urlString);

            RestTemplate restTemplate = new RestTemplate();

            // Note: have to manually construct URL because RestTemplate does the weird things internally:
            // https://stackoverflow.com/a/47311387/852604
            ResponseEntity<Void> responseEntity = restTemplate.exchange(
                    new RequestEntity<>(
                            "hello world!",
                            HttpMethod.PUT,
                            URI.create(urlString)),
                    Void.class);
            assertEquals(HttpStatus.OK, responseEntity.getStatusCode());

            S3Object s3Object = amazonS3.getObject(bucket.getBucketName(), "1.txt");
            String contentString = IOUtils.toString(s3Object.getObjectContent());
            assertEquals("hello world!", contentString);
        }
    }
}
