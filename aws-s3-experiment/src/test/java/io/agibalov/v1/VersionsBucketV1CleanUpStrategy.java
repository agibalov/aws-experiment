package io.agibalov.v1;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.DeleteObjectsRequest;
import com.amazonaws.services.s3.model.ListVersionsRequest;
import com.amazonaws.services.s3.model.S3VersionSummary;
import com.amazonaws.services.s3.model.VersionListing;

import java.util.List;
import java.util.stream.Collectors;

public class VersionsBucketV1CleanUpStrategy implements BucketV1CleanUpStrategy {
    @Override
    public void cleanUp(AmazonS3 amazonS3, String bucketName) {
        VersionListing versionListing = amazonS3.listVersions(new ListVersionsRequest()
                .withBucketName(bucketName));
        List<S3VersionSummary> versionSummaries = versionListing.getVersionSummaries();
        if (!versionSummaries.isEmpty()) {
            amazonS3.deleteObjects(new DeleteObjectsRequest(bucketName)
                    .withKeys(versionSummaries.stream()
                            .map(v -> new DeleteObjectsRequest.KeyVersion(v.getKey(), v.getVersionId()))
                            .collect(Collectors.toList())));
        }
    }
}
