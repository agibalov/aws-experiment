package me.loki2302;

import com.amazonaws.serverless.proxy.model.AwsProxyRequest;
import com.amazonaws.serverless.proxy.model.AwsProxyResponse;
import com.amazonaws.serverless.proxy.spring.SpringLambdaContainerHandler;
import com.amazonaws.services.lambda.runtime.Context;
import lombok.SneakyThrows;

public class AwsLambdaHandler {
    private static SpringLambdaContainerHandler<AwsProxyRequest, AwsProxyResponse> handler;

    @SneakyThrows
    public static AwsProxyResponse handleRequest(AwsProxyRequest input, Context context) {
        if (handler == null) {
            handler = SpringLambdaContainerHandler.getAwsProxyHandler(AppConfig.class);
        }

        return handler.proxy(input, context);
    }
}
