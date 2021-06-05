package io.agibalov.v1;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.amazonaws.services.s3.model.HeadBucketRequest;
import com.amazonaws.waiters.WaiterParameters;

import java.io.Closeable;

public class S3BucketV1 implements Closeable {
    private final AmazonS3 amazonS3;
    private final String bucketName;
    private final BucketV1CleanUpStrategy bucketV1CleanUpStrategy;

    public S3BucketV1(AmazonS3 amazonS3, String bucketName, BucketV1CleanUpStrategy bucketV1CleanUpStrategy) {
        this.amazonS3 = amazonS3;
        this.bucketName = bucketName;
        this.bucketV1CleanUpStrategy = bucketV1CleanUpStrategy;

        deleteBucket(amazonS3, bucketName, bucketV1CleanUpStrategy);

        amazonS3.createBucket(bucketName);
        amazonS3.waiters().bucketExists()
                .run(new WaiterParameters<>(new HeadBucketRequest(bucketName)));
    }

    public String getBucketName() {
        return bucketName;
    }

    @Override
    public void close() {
        deleteBucket(amazonS3, bucketName, bucketV1CleanUpStrategy);
    }

    private static void deleteBucket(
            AmazonS3 amazonS3,
            String bucketName,
            BucketV1CleanUpStrategy bucketV1CleanUpStrategy) {

        try {
            bucketV1CleanUpStrategy.cleanUp(amazonS3, bucketName);

            amazonS3.deleteBucket(bucketName);
            amazonS3.waiters().bucketNotExists()
                    .run(new WaiterParameters<>(new HeadBucketRequest(bucketName)));
        } catch (AmazonS3Exception e) {
            if (e.getStatusCode() == 404) {
                return;
            }

            throw e;
        }
    }
}
