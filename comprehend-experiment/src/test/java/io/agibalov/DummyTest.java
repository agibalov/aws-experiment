package io.agibalov;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.comprehend.AmazonComprehend;
import com.amazonaws.services.comprehend.AmazonComprehendClientBuilder;
import com.amazonaws.services.comprehend.model.*;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class DummyTest {
    private final static String TEXT = "San Francisco, officially City and County of San Francisco and " +
            "colloquially known as SanFran, is a city in—and the cultural, commercial, and financial " +
            "center of—Northern California. San Francisco is the 13th most populous city in the " +
            "United States, and the fourth most populous in California, with 883,305 residents as " +
            "of 2018. It covers an area of about 46.89 square miles (121.4 km2), mostly at the north " +
            "end of the San Francisco Peninsula in the San Francisco Bay Area, making it the second most " +
            "densely populated large U.S. city, and the fifth most densely populated U.S. county, behind " +
            "only four of the five New York City boroughs. San Francisco is the 12th-largest metropolitan " +
            "statistical area in the United States, with 4,729,484 people in 2018. With San Jose, it " +
            "forms the fifth most populous combined statistical area in the United States, the San Jose–San " +
            "Francisco–Oakland, CA Combined Statistical Area (9.67 million residents in 2018).";

    @Test
    public void itShouldExtractNamedEntities() {
        AmazonComprehend amazonComprehend = AmazonComprehendClientBuilder.standard()
                .withRegion(Regions.US_EAST_1)
                .build();

        DetectEntitiesResult detectEntitiesResult = amazonComprehend.detectEntities(new DetectEntitiesRequest()
                .withLanguageCode(LanguageCode.En)
                .withText(TEXT));

        List<Entity> entities = detectEntitiesResult.getEntities();
        for(Entity entity : entities) {
            System.out.printf("\"%s\" %s %f\n", entity.getText(), entity.getType(), entity.getScore());
        }

        assertTrue(entities.stream().anyMatch(
                e -> e.getText().equals("San Francisco") && e.getType().equals("LOCATION")));
        assertTrue(entities.stream().anyMatch(
                e -> e.getText().equals("2018") && e.getType().equals("DATE")));
    }

    @Test
    public void itShouldExtractKeyPhrases() {
        AmazonComprehend amazonComprehend = AmazonComprehendClientBuilder.standard()
                .withRegion(Regions.US_EAST_1)
                .build();

        DetectKeyPhrasesResult detectKeyPhrasesResult = amazonComprehend.detectKeyPhrases(
                new DetectKeyPhrasesRequest()
                        .withLanguageCode(LanguageCode.En)
                        .withText(TEXT));
        System.out.printf("%s\n", detectKeyPhrasesResult);

        List<KeyPhrase> keyPhrases = detectKeyPhrasesResult.getKeyPhrases();
        for(KeyPhrase keyPhrase : keyPhrases) {
            System.out.printf("\"%s\" %f\n", keyPhrase.getText(), keyPhrase.getScore());
        }

        assertTrue(keyPhrases.stream().anyMatch(p -> p.getText().equals("the 13th most populous city")));
    }

    @Test
    public void itShouldDetectSentiment() {
        AmazonComprehend amazonComprehend = AmazonComprehendClientBuilder.standard()
                .withRegion(Regions.US_EAST_1)
                .build();

        BatchDetectSentimentResult batchDetectSentimentResult = amazonComprehend.batchDetectSentiment(
                new BatchDetectSentimentRequest()
                        .withLanguageCode(LanguageCode.En)
                        .withTextList(
                                "San Francisco was founded on June 29, 1776",
                                "I like pizza! Pizza is the best!",
                                "I hate Mondays"
                        ));

        assertEquals(Arrays.asList("NEUTRAL", "POSITIVE", "NEGATIVE"),
                batchDetectSentimentResult.getResultList().stream()
                        .map(r -> r.getSentiment()).collect(Collectors.toList()));
    }
}
