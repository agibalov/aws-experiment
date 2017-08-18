package me.loki2302.awstestability;

import com.amazonaws.services.logs.AWSLogs;
import com.amazonaws.services.logs.AWSLogsClientBuilder;
import com.amazonaws.services.logs.model.FilterLogEventsRequest;
import com.amazonaws.services.logs.model.FilterLogEventsResult;
import com.amazonaws.services.logs.model.FilteredLogEvent;
import org.junit.rules.ExternalResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.TimeoutException;

public class CloudWatchLogs extends ExternalResource {
    private final static Logger LOGGER = LoggerFactory.getLogger(CloudWatchLogs.class);
    private long testStartTimestamp;
    private Map<String, Long> logTimesByLogGroupNames;

    @Override
    protected void before() throws Throwable {
        testStartTimestamp = System.currentTimeMillis();
        logTimesByLogGroupNames = new HashMap<>();
    }

    public <T> T read(String logGroupName, Class<T> clazz, Duration waitDuration) throws TimeoutException {
        logTimesByLogGroupNames.putIfAbsent(logGroupName, testStartTimestamp);
        long startTimestamp = logTimesByLogGroupNames.get(logGroupName);

        AWSLogs awsLogs = AWSLogsClientBuilder.defaultClient();

        Set<String> eventIds = new HashSet<>();
        String nextToken = null;
        long t0 = System.currentTimeMillis();
        while(System.currentTimeMillis() - t0 < waitDuration.toMillis()) {
            FilterLogEventsRequest filterLogEventsRequest = new FilterLogEventsRequest();
            filterLogEventsRequest.setLogGroupName(logGroupName);
            filterLogEventsRequest.setStartTime(startTimestamp);
            filterLogEventsRequest.setNextToken(nextToken);
            FilterLogEventsResult filterLogEventsResult = awsLogs.filterLogEvents(filterLogEventsRequest);
            List<FilteredLogEvent> filteredLogEvents = filterLogEventsResult.getEvents();
            for (FilteredLogEvent filteredLogEvent : filteredLogEvents) {
                String eventId = filteredLogEvent.getEventId();
                if(eventIds.contains(eventId)) {
                    continue;
                }

                eventIds.add(eventId);

                String message = filteredLogEvent.getMessage().trim();
                LOGGER.info("CloudWatch: {}", message);
                if(AwsTestability.hasMarker(message)) {
                    T result = AwsTestability.readString(message, clazz);
                    LOGGER.info("Extracted: {}", result);
                    startTimestamp = System.currentTimeMillis();
                    logTimesByLogGroupNames.put(logGroupName, startTimestamp);
                    return result;
                }
            }

            String token = filterLogEventsResult.getNextToken();
            if (token == null) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            } else {
                nextToken = token;
            }
        }

        throw new TimeoutException();
    }
}
