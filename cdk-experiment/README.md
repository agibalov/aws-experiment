# cdk-experiment

An AWS CDK hello world - creates an S3-based website.

1. Install AWS CDK globally: `npm install -g aws-cdk`
2. Install the local dependencies: `npm install`
2. Set the env variables:
   ```
   export AWS_PROFILE=...
   export AWS_REGION=...
   ```
3. Bootstrap the environment: `npm run build && cdk bootstrap`. This will create a stack named "CDKToolkit".
4. (after every change) Deploy the app: `npm run build && cdk deploy`. This will create a stack named "WebsiteStack".
5. To undeploy the app: `cdk destroy`
6. **NOTE:** there's no such thing as "unbootstrap", so you'll have to delete the "CDKToolkit" stack manually, see https://github.com/awslabs/aws-cdk/issues/986
