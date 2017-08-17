package me.loki2302;

import com.amazonaws.services.logs.AWSLogs;
import com.amazonaws.services.logs.AWSLogsClientBuilder;
import com.amazonaws.services.logs.model.FilterLogEventsRequest;
import com.amazonaws.services.logs.model.FilterLogEventsResult;
import com.amazonaws.services.logs.model.FilteredLogEvent;
import org.junit.rules.ExternalResource;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class CloudWatchLogs extends ExternalResource {
    private long startTimestamp;

    @Override
    protected void before() throws Throwable {
        startTimestamp = System.currentTimeMillis();
    }

    public void waitForMarker(String logGroupName, String marker) {
        AWSLogs awsLogs = AWSLogsClientBuilder.defaultClient();

        Set<String> eventIds = new HashSet<>();
        String nextToken = null;
        while(true) {
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

                String streamName = filteredLogEvent.getLogStreamName();
                String message = filteredLogEvent.getMessage().trim();
                System.out.printf("%s: %s\n", streamName, message);
                if(message.contains(marker)) {
                    startTimestamp = System.currentTimeMillis();
                    return;
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
    }
}
