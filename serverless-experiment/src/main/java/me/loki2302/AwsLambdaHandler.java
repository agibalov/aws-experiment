package me.loki2302;

import com.amazonaws.serverless.exceptions.ContainerInitializationException;
import com.amazonaws.serverless.proxy.internal.model.AwsProxyRequest;
import com.amazonaws.serverless.proxy.internal.model.AwsProxyResponse;
import com.amazonaws.serverless.proxy.spring.SpringLambdaContainerHandler;
import com.amazonaws.services.lambda.runtime.Context;

public class AwsLambdaHandler {
    private static SpringLambdaContainerHandler<AwsProxyRequest, AwsProxyResponse> handler;

    public static AwsProxyResponse handleRequest(AwsProxyRequest input, Context context) {
        if (handler == null) {
            try {
                handler = SpringLambdaContainerHandler.getAwsProxyHandler(AppConfig.class);
            } catch (ContainerInitializationException e) {
                throw new RuntimeException(e);
            }
        }

        return handler.proxy(input, context);
    }
}
