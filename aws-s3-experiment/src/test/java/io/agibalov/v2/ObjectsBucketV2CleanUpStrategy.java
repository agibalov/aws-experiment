package io.agibalov.v2;

import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.util.List;
import java.util.stream.Collectors;

public class ObjectsBucketV2CleanUpStrategy implements BucketV2CleanUpStrategy {
    @Override
    public void cleanUp(S3Client s3Client, String bucketName) {
        ListObjectsResponse listObjectsResponse = s3Client.listObjects(ListObjectsRequest.builder()
                .bucket(bucketName)
                .build());

        List<S3Object> contents = listObjectsResponse.contents();
        if (!contents.isEmpty()) {
            s3Client.deleteObjects(DeleteObjectsRequest.builder()
                    .bucket(bucketName)
                    .delete(Delete.builder()
                            .objects(contents.stream()
                                    .map(c -> ObjectIdentifier.builder()
                                            .key(c.key())
                                            .build())
                                    .collect(Collectors.toList()))
                            .build())
                    .build());
        }
    }
}
