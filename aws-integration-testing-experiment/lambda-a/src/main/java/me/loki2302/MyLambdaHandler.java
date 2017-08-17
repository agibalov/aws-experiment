package me.loki2302;

import java.util.Date;
import java.util.Map;

public class MyLambdaHandler {
    public void handle(Map<String, String> request) throws InterruptedException {
        System.out.println("Hello world! " + new Date());
        System.out.printf("Got this request: %s\n", request);
        Thread.sleep(10000);
        System.out.println("Still working on it...");
        Thread.sleep(10000);
        System.out.println("Here it is -> MY_MARKER_ONE");
        Thread.sleep(10000);
        System.out.println("Still working on it... #2");
        Thread.sleep(10000);
        System.out.println("Here it is -> MY_MARKER_TWO");
        Thread.sleep(3000);
        System.out.println("Done");
    }
}
