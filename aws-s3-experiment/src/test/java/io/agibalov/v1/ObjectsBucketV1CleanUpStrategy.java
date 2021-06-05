package io.agibalov.v1;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.DeleteObjectsRequest;
import com.amazonaws.services.s3.model.ListObjectsV2Request;
import com.amazonaws.services.s3.model.ListObjectsV2Result;
import com.amazonaws.services.s3.model.S3ObjectSummary;

import java.util.List;
import java.util.stream.Collectors;

public class ObjectsBucketV1CleanUpStrategy implements BucketV1CleanUpStrategy {
    @Override
    public void cleanUp(AmazonS3 amazonS3, String bucketName) {
        ListObjectsV2Result listObjectsV2Result = amazonS3.listObjectsV2(new ListObjectsV2Request()
                .withBucketName(bucketName));
        List<S3ObjectSummary> objectSummaries = listObjectsV2Result.getObjectSummaries();
        if (!objectSummaries.isEmpty()) {
            amazonS3.deleteObjects(new DeleteObjectsRequest(bucketName)
                    .withKeys(objectSummaries.stream()
                            .map(v -> new DeleteObjectsRequest.KeyVersion(v.getKey()))
                            .collect(Collectors.toList())));
        }
    }
}
