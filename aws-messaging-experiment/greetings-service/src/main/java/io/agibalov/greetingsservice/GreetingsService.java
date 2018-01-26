package io.agibalov.greetingsservice;

import com.amazonaws.services.lambda.runtime.events.KinesisEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class GreetingsService {
    private final static Logger LOGGER = LoggerFactory.getLogger(GreetingsService.class);

    public void handleKinesisEvent(KinesisEvent kinesisEvent) {
        LOGGER.info("GreetingService started");
        try {
            List<KinesisEvent.KinesisEventRecord> kinesisEventRecords = kinesisEvent.getRecords();
            for (KinesisEvent.KinesisEventRecord kinesisEventRecord : kinesisEventRecords) {
                LOGGER.info("Processing event {}", kinesisEventRecord.getEventID());
            }

            LOGGER.info("GreetingService succeeded");
        } catch (Throwable t) {
            LOGGER.error("GreetingService failed", t);
        } finally {
            LOGGER.info("GreetingService finished");
        }
    }
}
