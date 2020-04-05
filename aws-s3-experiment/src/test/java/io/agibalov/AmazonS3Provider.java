package io.agibalov;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import org.testcontainers.containers.localstack.LocalStackContainer;

public class AmazonS3Provider implements TestRule {
    private LocalStackContainer localStackContainer;
    private AmazonS3 amazonS3;

    public AmazonS3Provider() {
        localStackContainer = isRunningAgainstAws() ? null : new LocalStackContainer()
                .withServices(LocalStackContainer.Service.S3);
    }

    public boolean isRunningAgainstAws() {
        return System.getProperty("AWS") != null;
    }

    public AmazonS3 getAmazonS3() {
        if(localStackContainer != null) {
            return AmazonS3ClientBuilder.standard()
                    .withEndpointConfiguration(localStackContainer.getEndpointConfiguration(LocalStackContainer.Service.S3))
                    .withCredentials(localStackContainer.getDefaultCredentialsProvider())
                    .build();
        }
        return amazonS3;
    }

    @Override
    public Statement apply(Statement base, Description description) {
        if(localStackContainer != null) {
            return localStackContainer.apply(base, description);
        }
        return new Statement() {
            @Override
            public void evaluate() throws Throwable {
                amazonS3 = AmazonS3ClientBuilder.defaultClient();
                try {
                    base.evaluate();
                } finally {
                    amazonS3.shutdown();
                }
            }
        };
    }
}
