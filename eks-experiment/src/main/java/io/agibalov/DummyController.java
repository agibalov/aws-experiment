package io.agibalov;

import com.amazonaws.auth.*;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.Bucket;
import com.amazonaws.services.securitytoken.AWSSecurityTokenServiceClientBuilder;
import com.amazonaws.services.securitytoken.model.GetCallerIdentityRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RequestMapping("/")
public class DummyController {
    private final AmazonS3 s3;

    public DummyController(AmazonS3 s3) {
        this.s3 = s3;
    }

    @GetMapping
    public ResponseEntity<?> index() {
        String defaultIdentityArn = getIdentityArn(
                "DefaultAWSCredentialsProviderChain",
                DefaultAWSCredentialsProviderChain.getInstance());
        String webIdentityTokenIdentityArn = getIdentityArn(
                "WebIdentityTokenCredentialsProvider",
                WebIdentityTokenCredentialsProvider.create());

        Map<String, Object> message = new HashMap<>();
        message.put("defaultIdentityArn", defaultIdentityArn);
        message.put("webIdentityTokenIdentityArn", webIdentityTokenIdentityArn);

        try {
            List<Bucket> buckets = s3.listBuckets();
            message.put("message", String.format("hello world! %s", Instant.now()));
            message.put("buckets", buckets.stream()
                    .map(b -> b.getName())
                    .collect(Collectors.toList()));
            return ResponseEntity.ok(message);
        } catch (Throwable t) {
            message.put("error", t.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(message);
        }
    }

    private static String getIdentityArn(String providerName, AWSCredentialsProvider credentialsProvider) {
        String identityArn = null;
        try {
            AWSCredentials credentials = credentialsProvider.getCredentials();
            identityArn = AWSSecurityTokenServiceClientBuilder.standard()
                    .withCredentials(new AWSStaticCredentialsProvider(credentials))
                    .build()
                    .getCallerIdentity(new GetCallerIdentityRequest()).getArn();
            log.info("{}: key={} secret={} identityArn={}",
                    providerName,
                    credentials.getAWSAccessKeyId(),
                    credentials.getAWSSecretKey(),
                    identityArn);
        } catch (Throwable t) {
            log.error("{}", providerName, t);
        }
        return identityArn;
    }
}
