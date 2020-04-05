package io.agibalov;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.*;
import org.junit.Rule;
import org.junit.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.net.URL;
import java.sql.Date;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;

public class PresignedUrlTest {
    private final static String TEST_BUCKET_NAME = "weufhiewurhi23uhr23r23";

    @Rule
    public final AmazonS3Provider amazonS3Provider = new AmazonS3Provider();

    @Test
    public void canGenerateAPresignedDownloadLink() {
        AmazonS3 amazonS3 = amazonS3Provider.getAmazonS3();
        amazonS3.createBucket(TEST_BUCKET_NAME);

        amazonS3.putObject(TEST_BUCKET_NAME, "1.txt", "hello there");
        URL url = amazonS3.generatePresignedUrl(new GeneratePresignedUrlRequest(TEST_BUCKET_NAME, "1.txt")
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
        if (amazonS3Provider.isRunningAgainstAws()) {
            // LocalStack doesn't handle these
            assertEquals("attachment", responseEntity.getHeaders().getContentDisposition().getType());
            assertEquals("custom123.txt", responseEntity.getHeaders().getContentDisposition().getFilename());
        }
        assertEquals("hello there", responseEntity.getBody());

        ObjectListing objectListing = amazonS3.listObjects(TEST_BUCKET_NAME);
        List<S3ObjectSummary> objectSummaries = objectListing.getObjectSummaries();
        List<DeleteObjectsRequest.KeyVersion> keys = objectSummaries.stream()
                .map(s -> new DeleteObjectsRequest.KeyVersion(s.getKey()))
                .collect(Collectors.toList());
        amazonS3.deleteObjects(new DeleteObjectsRequest(TEST_BUCKET_NAME)
                .withKeys(keys));

        amazonS3.deleteBucket(TEST_BUCKET_NAME);
    }
}
