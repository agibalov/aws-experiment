package io.agibalov;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.cloudformation.AmazonCloudFormation;
import com.amazonaws.services.cloudformation.AmazonCloudFormationClientBuilder;
import com.amazonaws.services.cloudformation.model.DescribeStacksRequest;
import com.amazonaws.services.cloudformation.model.DescribeStacksResult;
import com.amazonaws.services.cloudformation.model.Stack;
import com.amazonaws.services.kms.AWSKMS;
import com.amazonaws.services.kms.AWSKMSClientBuilder;
import com.amazonaws.services.kms.model.*;
import org.junit.Test;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class KmsTest {
    @Test
    public void rootCanEncryptAndDecryptAndUserACanOnlyDecrypt() {
        AmazonCloudFormation amazonCloudFormationClient = AmazonCloudFormationClientBuilder.standard()
                .withRegion(Regions.US_EAST_1)
                .build();

        DescribeStacksResult describeStacksResult = amazonCloudFormationClient.describeStacks(
                new DescribeStacksRequest().withStackName("kms"));
        Stack stack = describeStacksResult.getStacks().get(0);
        Map<String, String> stackOutputs = stack.getOutputs().stream()
                .collect(Collectors.toMap(o -> o.getOutputKey(), o -> o.getOutputValue()));

        String kmsKeyId = stackOutputs.get("KmsKeyId");

        AWSKMS awskms = AWSKMSClientBuilder.standard()
                .withRegion(Regions.US_EAST_1)
                .build();
        EncryptResult encryptResult = awskms.encrypt(new EncryptRequest()
                .withKeyId(kmsKeyId)
                .withPlaintext(ByteBuffer.wrap("hello world!".getBytes(StandardCharsets.UTF_8))));

        ByteBuffer cipherTextBlob = encryptResult.getCiphertextBlob();

        DecryptResult decryptResult = awskms.decrypt(new DecryptRequest()
                .withCiphertextBlob(cipherTextBlob));
        assertEquals("hello world!",
                new String(decryptResult.getPlaintext().array(), StandardCharsets.UTF_8));


        AWSKMS userAAwsKms = AWSKMSClientBuilder.standard()
                .withCredentials(new AWSStaticCredentialsProvider(new BasicAWSCredentials(
                        stackOutputs.get("UserAAccessKeyId"),
                        stackOutputs.get("UserASecretAccessKey"))))
                .withRegion(Regions.US_EAST_1)
                .build();

        try {
            userAAwsKms.encrypt(new EncryptRequest()
                    .withKeyId(kmsKeyId)
                    .withPlaintext(ByteBuffer.wrap("hello world!".getBytes(StandardCharsets.UTF_8))));
            fail();
        } catch (AWSKMSException e) {
            // intentionally blank
        }

        DecryptResult userADecryptResult = userAAwsKms.decrypt(new DecryptRequest()
                .withCiphertextBlob(cipherTextBlob));
        assertEquals("hello world!",
                new String(userADecryptResult.getPlaintext().array(), StandardCharsets.UTF_8));
    }
}
