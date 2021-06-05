package io.agibalov;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.*;
import com.amazonaws.util.IOUtils;
import io.agibalov.v1.S3BucketV1;
import org.junit.Rule;
import org.junit.Test;

import java.io.IOException;
import java.util.List;

import static org.junit.Assert.*;
import static org.junit.Assume.assumeTrue;

public class DummyV1Test {
    @Rule
    public final AmazonS3Provider amazonS3Provider = new AmazonS3Provider();

    @Test
    public void basicScenario() throws IOException {
        AmazonS3 amazonS3 = amazonS3Provider.getAmazonS3();
        try (S3BucketV1 bucket = amazonS3Provider.getS3BucketV1()) {
            PutObjectResult putObjectResult = amazonS3.putObject(bucket.getBucketName(), "1.txt", "hello there");
            String eTag1 = putObjectResult.getETag();

            putObjectResult = amazonS3.putObject(bucket.getBucketName(), "1.txt", "hello there!!!");
            String eTag2 = putObjectResult.getETag();
            assertNotEquals(eTag1, eTag2);

            S3Object s3Object = amazonS3.getObject(bucket.getBucketName(), "1.txt");
            assertEquals(eTag2, s3Object.getObjectMetadata().getETag());

            String contentString = IOUtils.toString(s3Object.getObjectContent());
            assertEquals("hello there!!!", contentString);

            ObjectListing objectListing = amazonS3.listObjects(bucket.getBucketName());
            List<S3ObjectSummary> objectSummaries = objectListing.getObjectSummaries();
            assertEquals(1, objectSummaries.size());
            assertEquals("1.txt", objectSummaries.get(0).getKey());
        }
    }

    @Test
    public void versioningScenario() throws IOException {
        assumeTrue("Minio doesn't support versions",
                !(amazonS3Provider.getApiProvider() instanceof AmazonS3Provider.MinioApiProvider));

        AmazonS3 amazonS3 = amazonS3Provider.getAmazonS3();
        try (S3BucketV1 bucket = amazonS3Provider.getS3BucketV1()) {
            amazonS3.setBucketVersioningConfiguration(new SetBucketVersioningConfigurationRequest(
                    bucket.getBucketName(),
                    new BucketVersioningConfiguration()
                            .withStatus(BucketVersioningConfiguration.ENABLED)));

            PutObjectResult putObjectResult1 = amazonS3.putObject(
                    bucket.getBucketName(), "1.txt", "hello there version one");
            String version1 = putObjectResult1.getVersionId();

            PutObjectResult putObjectResult2 = amazonS3.putObject(
                    bucket.getBucketName(), "1.txt", "hello there version two");
            String version2 = putObjectResult2.getVersionId();

            S3Object s3Object = amazonS3.getObject(bucket.getBucketName(), "1.txt");
            assertEquals(version2, s3Object.getObjectMetadata().getVersionId());
            assertEquals("hello there version two", IOUtils.toString(s3Object.getObjectContent()));

            S3Object s3ObjectV1 = amazonS3.getObject(new GetObjectRequest(bucket.getBucketName(), "1.txt")
                    .withVersionId(version1));
            assertEquals(version1, s3ObjectV1.getObjectMetadata().getVersionId());
            assertEquals("hello there version one", IOUtils.toString(s3ObjectV1.getObjectContent()));

            VersionListing versionListing = amazonS3.listVersions(new ListVersionsRequest()
                    .withBucketName(bucket.getBucketName()));
            List<S3VersionSummary> versionSummaries = versionListing.getVersionSummaries();
            assertEquals(2, versionSummaries.size());
            assertTrue(versionSummaries.stream().anyMatch(s -> s.getVersionId().equals(version1)));
            assertTrue(versionSummaries.stream().anyMatch(s -> s.getVersionId().equals(version2)));
        }
    }

    @Test
    public void eTagConstraintScenario() {
        assumeTrue("LocalStack doesn't support etags",
                !(amazonS3Provider.getApiProvider() instanceof AmazonS3Provider.LocalStackApiProvider));

        AmazonS3 amazonS3 = amazonS3Provider.getAmazonS3();
        try (S3BucketV1 bucket = amazonS3Provider.getS3BucketV1()) {
            amazonS3.putObject(bucket.getBucketName(), "1.txt", "hello there");

            S3Object s3Object = amazonS3.getObject(bucket.getBucketName(), "1.txt");
            String eTag = s3Object.getObjectMetadata().getETag();

            s3Object = amazonS3.getObject(new GetObjectRequest(bucket.getBucketName(), "1.txt")
                    .withNonmatchingETagConstraint(eTag));
            assertNull(s3Object);

            amazonS3.putObject(bucket.getBucketName(), "1.txt", "hello there!!!");

            s3Object = amazonS3.getObject(new GetObjectRequest(bucket.getBucketName(), "1.txt")
                    .withNonmatchingETagConstraint(eTag));
            assertNotNull(s3Object);

            ObjectListing objectListing = amazonS3.listObjects(bucket.getBucketName());
            List<S3ObjectSummary> objectSummaries = objectListing.getObjectSummaries();
            assertEquals(1, objectSummaries.size());
            assertEquals("1.txt", objectSummaries.get(0).getKey());
        }
    }
}
