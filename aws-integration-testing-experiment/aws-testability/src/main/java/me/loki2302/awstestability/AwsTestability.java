package me.loki2302.awstestability;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class AwsTestability {
    private final static Logger LOGGER = LoggerFactory.getLogger(AwsTestability.class);
    private final static String TESTABILITY_START_MARKER_STRING = "9cb76e7c";
    private final static String TESTABILITY_END_MARKER_STRING = "bf9a5a2b04bb";
    private final static ObjectMapper objectMapper = new ObjectMapper();

    public static String testString(Object o) {
        try {
            String s = String.format("%s%s%s",
                    TESTABILITY_START_MARKER_STRING,
                    objectMapper.writeValueAsString(o),
                    TESTABILITY_END_MARKER_STRING);
            LOGGER.debug("For object {} produced string {}", o, s);
            return s;
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public static boolean hasMarker(String s) {
        int startMarkerIndex = s.indexOf(TESTABILITY_START_MARKER_STRING);
        int endMarkerIndex = s.indexOf(TESTABILITY_END_MARKER_STRING);
        return startMarkerIndex != -1 &&
                endMarkerIndex != -1 &&
                startMarkerIndex + TESTABILITY_START_MARKER_STRING.length() < endMarkerIndex;
    }

    public static <T> T readString(String s, Class<T> clazz) {
        int startMarkerIndex = s.indexOf(TESTABILITY_START_MARKER_STRING);
        int endMarkerIndex = s.indexOf(TESTABILITY_END_MARKER_STRING);
        String json = s.substring(
                startMarkerIndex + TESTABILITY_START_MARKER_STRING.length(),
                endMarkerIndex);
        LOGGER.debug("Extracted JSON: {}", json);
        try {
            return objectMapper.readValue(json, clazz);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
