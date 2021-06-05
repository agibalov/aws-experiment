package io.agibalov.v1;

import com.amazonaws.services.s3.AmazonS3;

public interface BucketV1CleanUpStrategy {
    void cleanUp(AmazonS3 amazonS3, String bucketName);
}
