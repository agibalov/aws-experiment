package me.loki2302;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.*;
import com.amazonaws.util.IOUtils;
import org.junit.Test;

import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;

public class DummyTest {
    private final static String TEST_BUCKET_NAME = "weufhiewurhi23uhr23r23";

    @Test
    public void basicScenario() throws IOException {
        AmazonS3 amazonS3 = AmazonS3ClientBuilder.defaultClient();

        amazonS3.createBucket(TEST_BUCKET_NAME);
        try {
            amazonS3.putObject(
                    TEST_BUCKET_NAME,
                    "1.txt",
                    "hello there");

            S3Object s3Object = amazonS3.getObject(TEST_BUCKET_NAME, "1.txt");
            String contentString = IOUtils.toString(s3Object.getObjectContent());
            assertEquals("hello there", contentString);

            ObjectListing objectListing = amazonS3.listObjects(TEST_BUCKET_NAME);
            assertEquals(1, objectListing.getObjectSummaries().size());
            assertEquals("1.txt", objectListing.getObjectSummaries().get(0).getKey());

            List<DeleteObjectsRequest.KeyVersion> keys = objectListing.getObjectSummaries().stream()
                    .map(s -> new DeleteObjectsRequest.KeyVersion(s.getKey()))
                    .collect(Collectors.toList());
            amazonS3.deleteObjects(new DeleteObjectsRequest(TEST_BUCKET_NAME)
                    .withKeys(keys));
        } finally {
            amazonS3.deleteBucket(TEST_BUCKET_NAME);
        }
    }
}
