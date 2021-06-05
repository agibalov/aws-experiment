package io.agibalov;

import io.agibalov.v2.S3BucketV2;
import org.junit.Rule;
import org.junit.Test;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.utils.IoUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.Assert.*;
import static org.junit.Assume.assumeTrue;

public class DummyV2Test {
    @Rule
    public final AmazonS3Provider amazonS3Provider = new AmazonS3Provider();

    @Test
    public void basicScenario() throws IOException {
        S3Client s3Client = amazonS3Provider.getS3Client();
        try (S3BucketV2 bucket = amazonS3Provider.getS3BucketV2()) {
            PutObjectResponse putObjectResponse = s3Client.putObject(
                    PutObjectRequest.builder()
                            .bucket(bucket.getBucketName())
                            .key("1.txt")
                            .build(),
                    RequestBody.fromBytes("hello there".getBytes(StandardCharsets.UTF_8)));
            String eTag1 = putObjectResponse.eTag();

            putObjectResponse = s3Client.putObject(
                    PutObjectRequest.builder()
                            .bucket(bucket.getBucketName())
                            .key("1.txt")
                            .build(),
                    RequestBody.fromBytes("hello there!!!".getBytes(StandardCharsets.UTF_8)));
            String eTag2 = putObjectResponse.eTag();

            assertNotEquals(eTag1, eTag2);

            try (ResponseInputStream<GetObjectResponse> responseInputStream = s3Client.getObject(GetObjectRequest.builder()
                    .bucket(bucket.getBucketName())
                    .key("1.txt")
                    .build())) {

                GetObjectResponse getObjectResponse = responseInputStream.response();
                if (!(amazonS3Provider.getApiProvider() instanceof AmazonS3Provider.LocalStackApiProvider)) {
                    assertEquals(eTag2, getObjectResponse.eTag());
                } else {
                    assertEquals("\"" + eTag2 + "\"", getObjectResponse.eTag());
                }

                String contentString = IoUtils.toUtf8String(responseInputStream);
                assertEquals("hello there!!!", contentString);
            }

            ListObjectsResponse listObjectsResponse = s3Client.listObjects(ListObjectsRequest.builder()
                    .bucket(bucket.getBucketName())
                    .build());

            List<S3Object> contents = listObjectsResponse.contents();
            assertEquals(1, contents.size());
            assertEquals("1.txt", contents.get(0).key());
        }
    }

    @Test
    public void versioningScenario() throws IOException {
        assumeTrue("Minio doesn't support versions",
                !(amazonS3Provider.getApiProvider() instanceof AmazonS3Provider.MinioApiProvider));

        S3Client s3Client = amazonS3Provider.getS3Client();
        try (S3BucketV2 bucket = amazonS3Provider.getS3BucketV2()) {
            s3Client.putBucketVersioning(PutBucketVersioningRequest.builder()
                    .bucket(bucket.getBucketName())
                    .versioningConfiguration(VersioningConfiguration.builder()
                            .status(BucketVersioningStatus.ENABLED)
                            .build())
                    .build());

            PutObjectResponse putObjectResponse1 = s3Client.putObject(
                    PutObjectRequest.builder()
                            .bucket(bucket.getBucketName())
                            .key("1.txt")
                            .build(),
                    RequestBody.fromBytes("hello there version one".getBytes(StandardCharsets.UTF_8)));
            String version1 = putObjectResponse1.versionId();

            PutObjectResponse putObjectResponse2 = s3Client.putObject(
                    PutObjectRequest.builder()
                            .bucket(bucket.getBucketName())
                            .key("1.txt")
                            .build(),
                    RequestBody.fromBytes("hello there version two".getBytes(StandardCharsets.UTF_8)));
            String version2 = putObjectResponse2.versionId();

            try (ResponseInputStream<GetObjectResponse> responseInputStream = s3Client.getObject(GetObjectRequest.builder()
                    .bucket(bucket.getBucketName())
                    .key("1.txt")
                    .build())) {

                assertEquals(version2, responseInputStream.response().versionId());
                assertEquals("hello there version two", IoUtils.toUtf8String(responseInputStream));
            }

            try (ResponseInputStream<GetObjectResponse> responseInputStream = s3Client.getObject(GetObjectRequest.builder()
                    .bucket(bucket.getBucketName())
                    .key("1.txt")
                    .versionId(version1)
                    .build())) {

                assertEquals(version1, responseInputStream.response().versionId());
                assertEquals("hello there version one", IoUtils.toUtf8String(responseInputStream));
            }
        }
    }

    @Test
    public void eTagConstraintScenario() throws IOException {
        assumeTrue("LocalStack doesn't support etags",
                !(amazonS3Provider.getApiProvider() instanceof AmazonS3Provider.LocalStackApiProvider));

        S3Client s3Client = amazonS3Provider.getS3Client();
        try (S3BucketV2 bucket = amazonS3Provider.getS3BucketV2()) {
            PutObjectResponse putObjectResponse = s3Client.putObject(
                    PutObjectRequest.builder()
                            .bucket(bucket.getBucketName())
                            .key("1.txt")
                            .build(),
                    RequestBody.fromBytes("hello there".getBytes(StandardCharsets.UTF_8)));
            String eTag = putObjectResponse.eTag();

            try {
                s3Client.getObject(GetObjectRequest.builder()
                        .bucket(bucket.getBucketName())
                        .key("1.txt")
                        .ifNoneMatch(eTag)
                        .build());
                fail();
            } catch (S3Exception e) {
                assertEquals(304, e.statusCode());
            }

            s3Client.putObject(
                    PutObjectRequest.builder()
                            .bucket(bucket.getBucketName())
                            .key("1.txt")
                            .build(),
                    RequestBody.fromBytes("hello there!!!".getBytes(StandardCharsets.UTF_8)));

            try (ResponseInputStream<GetObjectResponse> responseInputStream = s3Client.getObject(GetObjectRequest.builder()
                    .bucket(bucket.getBucketName())
                    .key("1.txt")
                    .ifNoneMatch(eTag)
                    .build())) {

                assertEquals("hello there!!!", IoUtils.toUtf8String(responseInputStream));
            }

            ListObjectsResponse listObjectsResponse = s3Client.listObjects(ListObjectsRequest.builder()
                    .bucket(bucket.getBucketName())
                    .build());

            List<S3Object> contents = listObjectsResponse.contents();
            assertEquals(1, contents.size());
            assertEquals("1.txt", contents.get(0).key());
        }
    }
}
