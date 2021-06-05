package io.agibalov;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import io.agibalov.v1.BucketV1CleanUpStrategy;
import io.agibalov.v1.ObjectsBucketV1CleanUpStrategy;
import io.agibalov.v1.S3BucketV1;
import io.agibalov.v1.VersionsBucketV1CleanUpStrategy;
import io.agibalov.v2.BucketV2CleanUpStrategy;
import io.agibalov.v2.ObjectsBucketV2CleanUpStrategy;
import io.agibalov.v2.S3BucketV2;
import io.agibalov.v2.VersionsBucketV2CleanUpStrategy;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.net.URI;
import java.util.UUID;

public class AmazonS3Provider implements TestRule {
    private ApiProvider apiProvider;

    public AmazonS3Provider() {
        apiProvider = decideApiProvider(System.getenv("API_PROVIDER"));
    }

    public ApiProvider getApiProvider() {
        return apiProvider;
    }

    public AmazonS3 getAmazonS3() {
        return apiProvider.getAmazonS3();
    }

    public S3Client getS3Client() {
        return apiProvider.getS3Client();
    }

    public S3Presigner getS3Presigner() {
        return apiProvider.getS3Presigner();
    }

    public S3BucketV1 getS3BucketV1() {
        return new S3BucketV1(getAmazonS3(), UUID.randomUUID().toString(), apiProvider.getBucketV1CleanUpStrategy());
    }

    public S3BucketV2 getS3BucketV2() {
        return new S3BucketV2(getS3Client(), UUID.randomUUID().toString(), apiProvider.getBucketV2CleanUpStrategy());
    }

    @Override
    public Statement apply(Statement base, Description description) {
        return new Statement() {
            @Override
            public void evaluate() throws Throwable {
                apiProvider.start();
                try {
                    base.evaluate();
                } finally {
                    apiProvider.stop();
                }
            }
        };
    }

    private static ApiProvider decideApiProvider(String providerCode) {
        if (providerCode.equals("aws")) {
            return new AwsApiProvider();
        }

        if (providerCode.equals("localstack")) {
            return new LocalStackApiProvider();
        }

        if (providerCode.equals("minio")) {
            return new MinioApiProvider();
        }

        throw new RuntimeException(String.format("Unknown provider %s", providerCode));
    }

    public interface ApiProvider {
        void start();
        AmazonS3 getAmazonS3();
        S3Client getS3Client();
        S3Presigner getS3Presigner();
        BucketV1CleanUpStrategy getBucketV1CleanUpStrategy();
        BucketV2CleanUpStrategy getBucketV2CleanUpStrategy();
        void stop();
    }

    public static class MinioApiProvider implements ApiProvider {
        private final static String ACCESS_KEY = "dummyaccesskey";
        private final static String SECRET_KEY = "dummysecretkey";

        private GenericContainer container;

        public MinioApiProvider() {
            container = new GenericContainer("minio/minio:RELEASE.2021-05-27T22-06-31Z")
                    .withExposedPorts(9000)
                    .withEnv("MINIO_ACCESS_KEY", ACCESS_KEY)
                    .withEnv("MINIO_SECRET_KEY", SECRET_KEY)
                    .withCommand("server", "/home/shared")
                    .waitingFor(Wait.forLogMessage("^Browser Access.+", 1));
        }

        @Override
        public void start() {
            container.start();
        }

        @Override
        public AmazonS3 getAmazonS3() {
            return AmazonS3ClientBuilder.standard()
                    .withEndpointConfiguration(new AwsClientBuilder.EndpointConfiguration(
                            getEndpointUri().toString(), Region.US_EAST_1.toString()))
                    .withCredentials(new AWSStaticCredentialsProvider(new BasicAWSCredentials(ACCESS_KEY, SECRET_KEY)))
                    .enablePathStyleAccess()
                    .build();
        }

        @Override
        public S3Client getS3Client() {
            return S3Client.builder()
                    .endpointOverride(getEndpointUri())
                    .region(Region.US_EAST_1)
                    .credentialsProvider(StaticCredentialsProvider.create(
                            AwsBasicCredentials.create(ACCESS_KEY, SECRET_KEY)))
                    .serviceConfiguration(S3Configuration.builder()
                            .pathStyleAccessEnabled(true)
                            .build())
                    .build();
        }

        @Override
        public S3Presigner getS3Presigner() {
            return S3Presigner.builder()
                    .endpointOverride(getEndpointUri())
                    .region(Region.US_EAST_1)
                    .credentialsProvider(StaticCredentialsProvider.create(
                            AwsBasicCredentials.create(ACCESS_KEY, SECRET_KEY)))
                    .serviceConfiguration(S3Configuration.builder()
                            .pathStyleAccessEnabled(true)
                            .build())
                    .build();
        }

        @Override
        public BucketV1CleanUpStrategy getBucketV1CleanUpStrategy() {
            // Minio doesn't support versioning
            return new ObjectsBucketV1CleanUpStrategy();
        }

        @Override
        public BucketV2CleanUpStrategy getBucketV2CleanUpStrategy() {
            // Minio doesn't support versioning
            return new ObjectsBucketV2CleanUpStrategy();
        }

        private URI getEndpointUri() {
            String ipAddress = container.getContainerIpAddress();
            int mappedPort = container.getMappedPort(9000);
            return URI.create(String.format("http://%s:%d", ipAddress, mappedPort));
        }

        @Override
        public void stop() {
            container.stop();
        }
    }

    public static class LocalStackApiProvider implements ApiProvider {
        private LocalStackContainer localStackContainer;

        @Override
        public void start() {
            localStackContainer = new LocalStackContainer()
                    .withServices(LocalStackContainer.Service.S3);
            localStackContainer.start();
        }

        @Override
        public AmazonS3 getAmazonS3() {
            return AmazonS3ClientBuilder.standard()
                    .withEndpointConfiguration(localStackContainer
                            .getEndpointConfiguration(LocalStackContainer.Service.S3))
                    .withCredentials(localStackContainer.getDefaultCredentialsProvider())
                    .build();
        }

        @Override
        public S3Client getS3Client() {
            return S3Client.builder()
                    .endpointOverride(localStackContainer.getEndpointOverride(LocalStackContainer.Service.S3))
                    .region(Region.US_EAST_1)
                    .credentialsProvider(StaticCredentialsProvider.create(
                            AwsBasicCredentials.create(
                                    localStackContainer.getAccessKey(),
                                    localStackContainer.getSecretKey())))
                    .build();
        }

        @Override
        public S3Presigner getS3Presigner() {
            return S3Presigner.builder()
                    .endpointOverride(localStackContainer.getEndpointOverride(LocalStackContainer.Service.S3))
                    .region(Region.US_EAST_1)
                    .credentialsProvider(StaticCredentialsProvider.create(
                            AwsBasicCredentials.create(
                                    localStackContainer.getAccessKey(),
                                    localStackContainer.getSecretKey())))
                    .build();
        }

        @Override
        public BucketV1CleanUpStrategy getBucketV1CleanUpStrategy() {
            return new VersionsBucketV1CleanUpStrategy();
        }

        @Override
        public BucketV2CleanUpStrategy getBucketV2CleanUpStrategy() {
            return new VersionsBucketV2CleanUpStrategy();
        }

        @Override
        public void stop() {
            localStackContainer.stop();
        }
    }

    public static class AwsApiProvider implements ApiProvider {
        @Override
        public void start() {
        }

        @Override
        public AmazonS3 getAmazonS3() {
            return AmazonS3ClientBuilder.standard()
                    .withRegion(Regions.US_EAST_1)
                    .build();
        }

        @Override
        public S3Client getS3Client() {
            return S3Client.builder()
                    .region(Region.US_EAST_1)
                    .build();
        }

        @Override
        public S3Presigner getS3Presigner() {
            return S3Presigner.builder()
                    .region(Region.US_EAST_1)
                    .build();
        }

        @Override
        public BucketV1CleanUpStrategy getBucketV1CleanUpStrategy() {
            return new VersionsBucketV1CleanUpStrategy();
        }

        @Override
        public BucketV2CleanUpStrategy getBucketV2CleanUpStrategy() {
            return new VersionsBucketV2CleanUpStrategy();
        }

        @Override
        public void stop() {
        }
    }
}
