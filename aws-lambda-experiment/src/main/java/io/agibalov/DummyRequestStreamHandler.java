package io.agibalov;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class DummyRequestStreamHandler implements RequestStreamHandler {
    @Override
    public void handleRequest(
            InputStream input,
            OutputStream output,
            Context context) throws IOException {

        LambdaLogger lambdaLogger = context.getLogger();
        lambdaLogger.log(String.format("Got request!"));

        ObjectMapper objectMapper = new ObjectMapper();

        LambdaResult lambdaResult = new LambdaResult();
        lambdaResult.statusCode = "200";
        lambdaResult.headers = new HashMap<>();
        lambdaResult.body = "I am Java! " + new Date();

        objectMapper.writeValue(output, lambdaResult);
    }

    public static class LambdaResult {
        public String statusCode;
        public Map<String, Object> headers;
        public String body;
    }
}
