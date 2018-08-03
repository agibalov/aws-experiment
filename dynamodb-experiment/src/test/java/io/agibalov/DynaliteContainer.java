package io.agibalov;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import lombok.SneakyThrows;
import org.junit.rules.ExternalResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.images.builder.ImageFromDockerfile;

public class DynaliteContainer extends ExternalResource {
    private static final Logger LOGGER = LoggerFactory.getLogger(DynaliteContainer.class);
    private final GenericContainer delegate;

    @SneakyThrows
    public DynaliteContainer() {
        this.delegate = new GenericContainer(new ImageFromDockerfile("my-dynalite")
                .withDockerfileFromBuilder(dockerfileBuilder -> dockerfileBuilder
                        .from("node:10.8-alpine")
                        .run("npm install -g dynalite@2.0.0")
                        .cmd("dynalite")))
                .withExposedPorts(4567)
                .withLogConsumer(new Slf4jLogConsumer(LOGGER));
    }

    public AwsClientBuilder.EndpointConfiguration getEndpointConfiguration() {
        return new AwsClientBuilder.EndpointConfiguration("http://" +
                this.delegate.getContainerIpAddress() + ":" +
                this.delegate.getMappedPort(4567), null);
    }

    public AWSCredentialsProvider getCredentials() {
        return new AWSStaticCredentialsProvider(new BasicAWSCredentials("dummy", "dummy"));
    }

    @Override
    protected void before() {
        try {
            delegate.start();
        } catch (Throwable t) {
            t.printStackTrace();
            throw t;
        }
    }

    @Override
    protected void after() {
        delegate.stop();
    }
}
