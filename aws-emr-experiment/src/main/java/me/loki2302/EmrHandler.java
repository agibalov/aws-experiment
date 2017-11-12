package me.loki2302;

import java.util.Arrays;
import java.util.stream.Collectors;

public class EmrHandler {
    public static void main(String[] args) {
        System.out.printf("hello world! args=%s\n", Arrays.stream(args)
                .collect(Collectors.joining(",")));

        System.out.println("\n*** Environment variables");
        System.getenv().forEach((k, v) -> System.out.printf("%s=%s\n", k, v));

        System.out.println("\n*** System properties");
        System.getProperties().forEach((k, v) -> System.out.printf("%s=%s\n", k, v));
    }
}
