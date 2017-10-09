package me.loki2302;

import com.amazonaws.serverless.exceptions.ContainerInitializationException;
import com.amazonaws.serverless.proxy.internal.model.AwsProxyRequest;
import com.amazonaws.serverless.proxy.internal.model.AwsProxyResponse;
import com.amazonaws.serverless.proxy.spring.SpringLambdaContainerHandler;
import com.amazonaws.services.lambda.runtime.Context;
import org.springframework.http.HttpHeaders;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.EnumSet;

public class AwsLambdaHandler {
    private static SpringLambdaContainerHandler<AwsProxyRequest, AwsProxyResponse> handler;

    public static AwsProxyResponse handleRequest(AwsProxyRequest input, Context context) {
        if (handler == null) {
            try {
                handler = SpringLambdaContainerHandler.getAwsProxyHandler(AppConfig.class);

                // This entire block of code (as well as NaiveCorsFilter) only exists
                // because SpringLambdaContainerHandler has some issue with filter chain support:
                // https://github.com/awslabs/aws-serverless-java-container/issues/66
                // Once resolved, remove all this code and switch to Spring's recommended approach.
                // 1. @CrossOrigin annotation works fine, but I can't use it to allow CORS for Swagger
                // 2. WebMvcConfigurer::addCorsMappings should be the way to go, but it doesn't work as of 0.7
                // 3. CorsFilter doesn't work either
                handler.onStartup(startupHandler -> {
                    Filter naiveCorsFilter = new NaiveCorsFilter();
                    FilterRegistration.Dynamic registration = startupHandler.addFilter(
                            "NaiveCorsFilter", naiveCorsFilter);
                    registration.addMappingForUrlPatterns(
                            EnumSet.of(DispatcherType.REQUEST), true, "/*");
                });
            } catch (ContainerInitializationException e) {
                throw new RuntimeException(e);
            }
        }

        return handler.proxy(input, context);
    }

    public static class NaiveCorsFilter implements Filter {
        @Override
        public void init(FilterConfig filterConfig) throws ServletException {
        }

        @Override
        public void doFilter(
                ServletRequest servletRequest,
                ServletResponse servletResponse,
                FilterChain chain) throws IOException, ServletException {

            HttpServletRequest request = (HttpServletRequest)servletRequest;
            String origin = request.getHeader(HttpHeaders.ORIGIN);
            if(origin != null) {
                String accessControlRequestHeaders = request.getHeader(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS);
                String accessControlRequestMethod = request.getHeader(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD);

                HttpServletResponse response = (HttpServletResponse)servletResponse;
                response.setHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, origin);
                response.setHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS, accessControlRequestHeaders);
                response.setHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS, accessControlRequestMethod);
                return;
            }

            chain.doFilter(servletRequest, servletResponse);
        }

        @Override
        public void destroy() {
        }
    }
}
