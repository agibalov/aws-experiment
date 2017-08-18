package me.loki2302;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static me.loki2302.awstestability.AwsTestability.testString;

public class MyLambdaHandler {
    public void handle(Map<String, String> request) throws InterruptedException {
        System.out.println("Hello world! " + new Date());

        System.out.println(testString(new HashMap<String, String>() {{
            put("status", "working");
            put("step", "1");
        }}));

        Thread.sleep(10000);

        System.out.println("Still working on it...");

        Thread.sleep(10000);

        System.out.println(testString(new HashMap<String, String>() {{
            put("status", "done");
            put("step", "2");
        }}));

        Thread.sleep(10000);
        System.out.println("Done");
    }
}
