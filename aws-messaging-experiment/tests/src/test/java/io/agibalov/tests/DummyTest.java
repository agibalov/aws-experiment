package io.agibalov.tests;

import com.amazonaws.services.kinesis.AmazonKinesis;
import com.amazonaws.services.kinesis.AmazonKinesisClientBuilder;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;

public class DummyTest {
    private final static Logger LOGGER = LoggerFactory.getLogger(DummyTest.class);

    @Test
    public void dummy() {
        LOGGER.info("Hello world!");

        AmazonKinesis amazonKinesis = AmazonKinesisClientBuilder.defaultClient();
        String kinesisStreamName = "messaging-experiment-VisitsKinesisStream-9OPAI6YP765O";
        amazonKinesis.putRecord(kinesisStreamName, ByteBuffer.wrap("hi there".getBytes()), kinesisStreamName);
    }
}
