package io.agibalov.v2;

import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.util.stream.Collectors;

public class VersionsBucketV2CleanUpStrategy implements BucketV2CleanUpStrategy {
    @Override
    public void cleanUp(S3Client s3Client, String bucketName) {
        ListObjectVersionsResponse listObjectVersionsResponse = s3Client.listObjectVersions(
                ListObjectVersionsRequest.builder()
                        .bucket(bucketName)
                        .build());
        if (listObjectVersionsResponse.hasVersions()) {
            s3Client.deleteObjects(DeleteObjectsRequest.builder()
                    .bucket(bucketName)
                    .delete(Delete.builder()
                            .objects(listObjectVersionsResponse.versions().stream()
                                    .map(v -> ObjectIdentifier.builder()
                                            .key(v.key())
                                            .versionId(v.versionId())
                                            .build())
                                    .collect(Collectors.toList()))
                            .build())
                    .build());
        }

        if (listObjectVersionsResponse.hasDeleteMarkers()) {
            s3Client.deleteObjects(DeleteObjectsRequest.builder()
                    .bucket(bucketName)
                    .delete(Delete.builder()
                            .objects(listObjectVersionsResponse.deleteMarkers().stream()
                                    .map(v -> ObjectIdentifier.builder()
                                            .key(v.key())
                                            .versionId(v.versionId())
                                            .build())
                                    .collect(Collectors.toList()))
                            .build())
                    .build());
        }
    }
}
