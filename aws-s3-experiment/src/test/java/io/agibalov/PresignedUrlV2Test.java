package io.agibalov;

import org.junit.Rule;
import org.junit.Test;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;
import software.amazon.awssdk.utils.IoUtils;

import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assume.assumeTrue;

public class PresignedUrlV2Test {
    private final static String TEST_BUCKET_NAME = "weufhiewurhi23uhr23r23";

    @Rule
    public final AmazonS3Provider amazonS3Provider = new AmazonS3Provider();

    @Test
    public void canGenerateAPresignedDownloadLink() {
        assumeTrue("AWS Java S3 SDK v1 worked fine, but with v2 it's: " +
                        "There were headers present in the request which were not signed",
                !(amazonS3Provider.getApiProvider() instanceof AmazonS3Provider.MinioApiProvider));

        S3Client s3Client = amazonS3Provider.getS3Client();

        s3Client.createBucket(CreateBucketRequest.builder()
                .bucket(TEST_BUCKET_NAME)
                .build());

        s3Client.putObject(
                PutObjectRequest.builder()
                        .bucket(TEST_BUCKET_NAME)
                        .key("1.txt")
                        .build(),
                RequestBody.fromBytes("hello there".getBytes(StandardCharsets.UTF_8)));

        S3Presigner s3Presigner = amazonS3Provider.getS3Presigner();
        PresignedGetObjectRequest presignedGetObjectRequest = s3Presigner.presignGetObject(
                GetObjectPresignRequest.builder()
                        .signatureDuration(Duration.of(1, ChronoUnit.HOURS))
                        .getObjectRequest(GetObjectRequest.builder()
                                .bucket(TEST_BUCKET_NAME)
                                .key("1.txt")
                                .responseContentDisposition(ContentDisposition.builder("attachment")
                                        .filename("custom123.txt")
                                        .build()
                                        .toString())
                                .build())
                        .build());
        URL url = presignedGetObjectRequest.url();

        String urlString = url.toString();
        System.out.printf("URL string: %s\n", urlString);

        RestTemplate restTemplate = new RestTemplate();

        // Note: have to manually construct URL because RestTemplate does the weird things internally:
        // https://stackoverflow.com/a/47311387/852604
        ResponseEntity<String> responseEntity = restTemplate.getForEntity(URI.create(urlString), String.class);
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        if (!(amazonS3Provider.getApiProvider() instanceof AmazonS3Provider.LocalStackApiProvider)) {
            // LocalStack doesn't support headers
            ContentDisposition contentDisposition = responseEntity.getHeaders().getContentDisposition();
            assertEquals("attachment", contentDisposition.getType());
            assertEquals("custom123.txt", contentDisposition.getFilename());
        }
        assertEquals("hello there", responseEntity.getBody());

        ListObjectsResponse listObjectsResponse = s3Client.listObjects(ListObjectsRequest.builder()
                .bucket(TEST_BUCKET_NAME)
                .build());

        s3Client.deleteObjects(DeleteObjectsRequest.builder()
                .bucket(TEST_BUCKET_NAME)
                .delete(Delete.builder()
                        .objects(listObjectsResponse.contents().stream()
                                .map(c -> ObjectIdentifier.builder()
                                        .key(c.key())
                                        .build())
                                .collect(Collectors.toList()))
                        .build())
                .build());

        s3Client.deleteBucket(DeleteBucketRequest.builder()
                .bucket(TEST_BUCKET_NAME)
                .build());
    }

    @Test
    public void canGenerateAPresignedUploadLink() throws IOException {
        S3Client s3Client = amazonS3Provider.getS3Client();

        s3Client.createBucket(CreateBucketRequest.builder()
                .bucket(TEST_BUCKET_NAME)
                .build());

        S3Presigner s3Presigner = amazonS3Provider.getS3Presigner();
        PresignedPutObjectRequest presignedPutObjectRequest = s3Presigner.presignPutObject(
                PutObjectPresignRequest.builder()
                        .signatureDuration(Duration.of(1, ChronoUnit.HOURS))
                        .putObjectRequest(PutObjectRequest.builder()
                                .bucket(TEST_BUCKET_NAME)
                                .key("1.txt")
                                .build())
                        .build());
        URL url = presignedPutObjectRequest.url();

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

        try (ResponseInputStream<GetObjectResponse> responseInputStream = s3Client.getObject(GetObjectRequest.builder()
                .bucket(TEST_BUCKET_NAME)
                .key("1.txt")
                .build())) {

            String contentString = IoUtils.toUtf8String(responseInputStream);
            assertEquals("hello world!", contentString);
        }

        ListObjectsResponse listObjectsResponse = s3Client.listObjects(ListObjectsRequest.builder()
                .bucket(TEST_BUCKET_NAME)
                .build());

        s3Client.deleteObjects(DeleteObjectsRequest.builder()
                .bucket(TEST_BUCKET_NAME)
                .delete(Delete.builder()
                        .objects(listObjectsResponse.contents().stream()
                                .map(c -> ObjectIdentifier.builder()
                                        .key(c.key())
                                        .build())
                                .collect(Collectors.toList()))
                        .build())
                .build());

        s3Client.deleteBucket(DeleteBucketRequest.builder()
                .bucket(TEST_BUCKET_NAME)
                .build());
    }
}
