package io.agibalov.v2;

import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.DeleteBucketRequest;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import software.amazon.awssdk.services.s3.model.NoSuchBucketException;

import java.io.Closeable;

public class S3BucketV2 implements Closeable {
    private final S3Client s3Client;
    private final String bucketName;
    private final BucketV2CleanUpStrategy bucketV2CleanUpStrategy;

    public S3BucketV2(S3Client s3Client, String bucketName, BucketV2CleanUpStrategy bucketV2CleanUpStrategy) {
        this.s3Client = s3Client;
        this.bucketName = bucketName;
        this.bucketV2CleanUpStrategy = bucketV2CleanUpStrategy;

        deleteBucket(s3Client, bucketName, bucketV2CleanUpStrategy);

        s3Client.createBucket(CreateBucketRequest.builder()
                .bucket(bucketName)
                .build());
        s3Client.waiter().waitUntilBucketExists(HeadBucketRequest.builder()
                .bucket(bucketName)
                .build());
    }

    public String getBucketName() {
        return bucketName;
    }

    @Override
    public void close() {
        deleteBucket(s3Client, bucketName, bucketV2CleanUpStrategy);
    }

    private static void deleteBucket(
            S3Client s3Client,
            String bucketName,
            BucketV2CleanUpStrategy bucketV2CleanUpStrategy) {

        try {
            bucketV2CleanUpStrategy.cleanUp(s3Client, bucketName);

            s3Client.deleteBucket(DeleteBucketRequest.builder()
                    .bucket(bucketName)
                    .build());

            s3Client.waiter().waitUntilBucketNotExists(HeadBucketRequest.builder()
                    .bucket(bucketName)
                    .build());
        } catch (NoSuchBucketException e) {
            return;
        }
    }
}
