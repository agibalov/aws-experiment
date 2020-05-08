package io.agibalov;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.containers.wait.strategy.Wait;

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
        void stop();
    }

    public static class MinioApiProvider implements ApiProvider {
        private final static String ACCESS_KEY = "dummyaccesskey";
        private final static String SECRET_KEY = "dummysecretkey";

        private GenericContainer container;

        public MinioApiProvider() {
            container = new GenericContainer("minio/minio:RELEASE.2020-05-01T22-19-14Z")
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
            String ipAddress = container.getContainerIpAddress();
            int mappedPort = container.getMappedPort(9000);
            String endpointUrl = String.format("http://%s:%d", ipAddress, mappedPort);
            return AmazonS3ClientBuilder.standard()
                    .withEndpointConfiguration(new AwsClientBuilder.EndpointConfiguration(endpointUrl, "us-east-1"))
                    .withCredentials(new AWSStaticCredentialsProvider(new BasicAWSCredentials(ACCESS_KEY, SECRET_KEY)))
                    .enablePathStyleAccess()
                    .build();
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
                    .withEndpointConfiguration(localStackContainer.getEndpointConfiguration(LocalStackContainer.Service.S3))
                    .withCredentials(localStackContainer.getDefaultCredentialsProvider())
                    .build();
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
        public void stop() {
        }
    }
}
