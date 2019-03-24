import cdk = require('@aws-cdk/cdk');
import s3 = require('@aws-cdk/aws-s3');
import s3deploy = require('@aws-cdk/aws-s3-deployment');
import { RemovalPolicy } from '@aws-cdk/cdk';

export class WebsiteStack extends cdk.Stack {
  constructor(scope: cdk.Construct, id: string, props?: cdk.StackProps) {
    super(scope, id, props);

    const websiteBucket = new s3.Bucket(this, 'WebsiteBucket', {
      websiteIndexDocument: 'index.html',
      publicReadAccess: true,
      removalPolicy: RemovalPolicy.Destroy
    });

    new s3deploy.BucketDeployment(this, 'WebsiteBucketDeployment', {
      source: s3deploy.Source.asset('./website'),
      destinationBucket: websiteBucket,
      retainOnDelete: false
    });

    new cdk.CfnOutput(this, 'WebsiteUrl', { value: websiteBucket.bucketWebsiteUrl });
  }
}
