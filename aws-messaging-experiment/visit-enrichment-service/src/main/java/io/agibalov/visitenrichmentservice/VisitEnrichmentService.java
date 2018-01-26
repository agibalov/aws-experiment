package io.agibalov.visitenrichmentservice;

import com.amazonaws.services.kinesis.AmazonKinesis;
import com.amazonaws.services.kinesis.AmazonKinesisClientBuilder;
import com.amazonaws.services.lambda.runtime.events.KinesisEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.List;

public class VisitEnrichmentService {
    private final static Logger LOGGER = LoggerFactory.getLogger(VisitEnrichmentService.class);

    public void handleKinesisEvent(KinesisEvent kinesisEvent) {
        LOGGER.info("VisitEnrichmentService started");
        try {
            String enrichedVisitsKinesisStreamName = System.getenv("ENRICHED_VISITS_KINESIS_STREAM_NAME");
            AmazonKinesis amazonKinesis = AmazonKinesisClientBuilder.defaultClient();

            List<KinesisEvent.KinesisEventRecord> kinesisEventRecords = kinesisEvent.getRecords();
            for (KinesisEvent.KinesisEventRecord kinesisEventRecord : kinesisEventRecords) {
                LOGGER.info("Processing record {}", kinesisEventRecord.getEventID());

                amazonKinesis.putRecord(
                        enrichedVisitsKinesisStreamName,
                        ByteBuffer.wrap("hi there".getBytes()),
                        enrichedVisitsKinesisStreamName);
            }

            LOGGER.info("VisitEnrichmentService succeeded");
        } catch (Throwable t) {
            LOGGER.error("VisitEnrichmentService failed", t);
        } finally {
            LOGGER.info("VisitEnrichmentService finished");
        }
    }
}
