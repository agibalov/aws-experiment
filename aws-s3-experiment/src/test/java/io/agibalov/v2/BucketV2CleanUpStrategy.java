package io.agibalov.v2;

import software.amazon.awssdk.services.s3.S3Client;

public interface BucketV2CleanUpStrategy {
    void cleanUp(S3Client s3Client, String bucketName);
}
