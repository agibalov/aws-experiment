package myproject;

import com.pulumi.Context;
import com.pulumi.Pulumi;
import com.pulumi.asset.StringAsset;
import com.pulumi.aws.iam.IamFunctions;
import com.pulumi.aws.iam.inputs.GetPolicyDocumentArgs;
import com.pulumi.aws.iam.inputs.GetPolicyDocumentStatementArgs;
import com.pulumi.aws.iam.inputs.GetPolicyDocumentStatementPrincipalArgs;
import com.pulumi.aws.iam.outputs.GetPolicyDocumentResult;
import com.pulumi.aws.s3.*;
import com.pulumi.aws.s3.inputs.BucketWebsiteConfigurationV2ErrorDocumentArgs;
import com.pulumi.aws.s3.inputs.BucketWebsiteConfigurationV2IndexDocumentArgs;
import com.pulumi.resources.CustomResourceOptions;

public class App {
    public static void main(String[] args) {
        Pulumi.run(App::stack);
    }

    private static void stack(Context ctx) {
        var bucket = new BucketV2("website-bucket", BucketV2Args.builder()
                .build());

        var bucketPublicAccessBlock = new BucketPublicAccessBlock("bucket-public-access-block",
                BucketPublicAccessBlockArgs.builder()
                        .bucket(bucket.id())
                        .blockPublicAcls(true)
                        .blockPublicPolicy(false)
                        .ignorePublicAcls(true)
                        .restrictPublicBuckets(false)
                        .build());

        var policyDocumentOutput = bucket.arn().apply(bucketArn -> IamFunctions.getPolicyDocument(GetPolicyDocumentArgs.builder()
                .statements(GetPolicyDocumentStatementArgs.builder()
                        .principals(GetPolicyDocumentStatementPrincipalArgs.builder()
                                .type("AWS")
                                .identifiers("*")
                                .build())
                        .actions("s3:GetObject")
                        .effect("Allow")
                        .resources(
                                bucketArn,
                                String.format("%s/*", bucketArn)
                        )
                        .build())
                .build()));

        new BucketPolicy("bucket-policy",
                BucketPolicyArgs.builder()
                        .bucket(bucket.id())
                        .policy(policyDocumentOutput.applyValue(GetPolicyDocumentResult::json))
                        .build(),
                CustomResourceOptions.builder()
                        .dependsOn(bucketPublicAccessBlock)
                        .build());

        var indexObject = new BucketObject("index", BucketObjectArgs.builder()
                .bucket(bucket.id())
                .key("index.html")
                .source(new StringAsset("<h1>hello world</h1>"))
                .contentType("text/html")
                .build());

        var errorObject = new BucketObject("error", BucketObjectArgs.builder()
                .bucket(bucket.id())
                .key("error.html")
                .source(new StringAsset("<h1>Error!</h1>"))
                .contentType("text/html")
                .build());

        var websiteConfiguration = new BucketWebsiteConfigurationV2("website-configuration",
                BucketWebsiteConfigurationV2Args.builder()
                        .bucket(bucket.id())
                        .indexDocument(BucketWebsiteConfigurationV2IndexDocumentArgs.builder()
                                .suffix(indexObject.key())
                                .build())
                        .errorDocument(BucketWebsiteConfigurationV2ErrorDocumentArgs.builder()
                                .key(errorObject.key())
                                .build())
                        .build());

        ctx.export("bucket", bucket.bucket());
        ctx.export("websiteUrl", websiteConfiguration.websiteEndpoint()
                .applyValue(endpoint -> String.format("http://%s", endpoint)));
    }
}
