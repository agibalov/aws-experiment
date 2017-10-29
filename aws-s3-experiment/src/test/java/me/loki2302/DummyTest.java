package me.loki2302;

import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.*;
import com.amazonaws.util.IOUtils;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.Assert.*;

public class DummyTest {
    private final static String TEST_BUCKET_NAME = "weufhiewurhi23uhr23r23";

    private AmazonS3 amazonS3;

    @Before
    public void createAmazonS3Client() {
        if(System.getProperty("LOCALSTACK") != null) {
            amazonS3 = AmazonS3ClientBuilder
                    .standard()
                    .withEndpointConfiguration(new AwsClientBuilder.EndpointConfiguration(
                            "http://localhost:4572",
                            "us-east-1"
                    ))
                    .withPathStyleAccessEnabled(true)
                    .build();
        } else {
            amazonS3 = AmazonS3ClientBuilder.defaultClient();
        }
    }

    @Test
    public void basicScenario() throws IOException {
        amazonS3.createBucket(TEST_BUCKET_NAME);

        PutObjectResult putObjectResult = amazonS3.putObject(TEST_BUCKET_NAME, "1.txt", "hello there");
        String eTag1 = putObjectResult.getETag();

        putObjectResult = amazonS3.putObject(TEST_BUCKET_NAME, "1.txt", "hello there!!!");
        String eTag2 = putObjectResult.getETag();
        assertNotEquals(eTag1, eTag2);

        S3Object s3Object = amazonS3.getObject(TEST_BUCKET_NAME, "1.txt");
        assertEquals(eTag2, s3Object.getObjectMetadata().getETag());

        String contentString = IOUtils.toString(s3Object.getObjectContent());
        assertEquals("hello there!!!", contentString);

        ObjectListing objectListing = amazonS3.listObjects(TEST_BUCKET_NAME);
        List<S3ObjectSummary> objectSummaries = objectListing.getObjectSummaries();
        assertEquals(1, objectSummaries.size());
        assertEquals("1.txt", objectSummaries.get(0).getKey());

        List<DeleteObjectsRequest.KeyVersion> keys = objectSummaries.stream()
                .map(s -> new DeleteObjectsRequest.KeyVersion(s.getKey()))
                .collect(Collectors.toList());
        amazonS3.deleteObjects(new DeleteObjectsRequest(TEST_BUCKET_NAME)
                .withKeys(keys));

        amazonS3.deleteBucket(TEST_BUCKET_NAME);
    }

    @Test
    public void versioningScenario() throws IOException {
        amazonS3.createBucket(TEST_BUCKET_NAME);
        amazonS3.setBucketVersioningConfiguration(new SetBucketVersioningConfigurationRequest(
                TEST_BUCKET_NAME,
                new BucketVersioningConfiguration()
                        .withStatus(BucketVersioningConfiguration.ENABLED)));

        PutObjectResult putObjectResult1 = amazonS3.putObject(
                TEST_BUCKET_NAME, "1.txt", "hello there version one");
        String version1 = putObjectResult1.getVersionId();

        PutObjectResult putObjectResult2 = amazonS3.putObject(
                TEST_BUCKET_NAME, "1.txt", "hello there version two");
        String version2 = putObjectResult2.getVersionId();

        S3Object s3Object = amazonS3.getObject(TEST_BUCKET_NAME, "1.txt");
        assertEquals(version2, s3Object.getObjectMetadata().getVersionId());
        assertEquals("hello there version two", IOUtils.toString(s3Object.getObjectContent()));

        S3Object s3ObjectV1 = amazonS3.getObject(new GetObjectRequest(TEST_BUCKET_NAME, "1.txt")
                .withVersionId(version1));
        assertEquals(version1, s3ObjectV1.getObjectMetadata().getVersionId());
        assertEquals("hello there version one", IOUtils.toString(s3ObjectV1.getObjectContent()));

        VersionListing versionListing = amazonS3.listVersions(new ListVersionsRequest()
                .withBucketName(TEST_BUCKET_NAME));
        List<S3VersionSummary> versionSummaries = versionListing.getVersionSummaries();
        assertEquals(2, versionSummaries.size());
        assertTrue(versionSummaries.stream().anyMatch(s -> s.getVersionId().equals(version1)));
        assertTrue(versionSummaries.stream().anyMatch(s -> s.getVersionId().equals(version2)));

        amazonS3.deleteObjects(new DeleteObjectsRequest(TEST_BUCKET_NAME)
            .withKeys(versionSummaries.stream()
                    .map(s -> new DeleteObjectsRequest.KeyVersion(s.getKey(), s.getVersionId()))
                    .collect(Collectors.toList())));

        amazonS3.deleteBucket(TEST_BUCKET_NAME);
    }

    @Test
    public void eTagConstraintScenario() {
        amazonS3.createBucket(TEST_BUCKET_NAME);

        amazonS3.putObject(TEST_BUCKET_NAME, "1.txt", "hello there");

        S3Object s3Object = amazonS3.getObject(TEST_BUCKET_NAME, "1.txt");
        String eTag = s3Object.getObjectMetadata().getETag();

        s3Object = amazonS3.getObject(new GetObjectRequest(TEST_BUCKET_NAME, "1.txt")
                .withNonmatchingETagConstraint(eTag));
        assertNull(s3Object);

        amazonS3.putObject(TEST_BUCKET_NAME, "1.txt", "hello there!!!");

        s3Object = amazonS3.getObject(new GetObjectRequest(TEST_BUCKET_NAME, "1.txt")
                .withNonmatchingETagConstraint(eTag));
        assertNotNull(s3Object);

        ObjectListing objectListing = amazonS3.listObjects(TEST_BUCKET_NAME);
        List<S3ObjectSummary> objectSummaries = objectListing.getObjectSummaries();
        assertEquals(1, objectSummaries.size());
        assertEquals("1.txt", objectSummaries.get(0).getKey());

        List<DeleteObjectsRequest.KeyVersion> keys = objectSummaries.stream()
                .map(s -> new DeleteObjectsRequest.KeyVersion(s.getKey()))
                .collect(Collectors.toList());
        amazonS3.deleteObjects(new DeleteObjectsRequest(TEST_BUCKET_NAME)
                .withKeys(keys));

        amazonS3.deleteBucket(TEST_BUCKET_NAME);
    }
}