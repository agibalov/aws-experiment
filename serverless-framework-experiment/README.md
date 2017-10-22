### What is this?

While CloudFormation is a great way to describe AWS resources, AWS CLI is a little too hard to use when you just want to "deploy and undeploy". As a result, every time I create a new "experiment", I have to write a deployment tool ([example 1](https://github.com/loki2302/aws-experiment/blob/master/static-website-experiment/tool.sh), [example 2](https://github.com/loki2302/aws-experiment/blob/master/serverless-experiment/tool.sh), [example 3](https://github.com/loki2302/aws-experiment/blob/master/ecs-experiment/tool.sh)). This is my attempt to find out whether [Serverless](https://serverless.com/) is a good way to stop writing those scripts.

### How to run it

* `sls deploy -v` to deploy.
* `sls remove -v` to undeploy.

### Results so far

* **GOOD**. Serverless indeed allows you to  `sls deploy` to deploy everything and `sls remove` to undeploy everything. What I like most, is that they show what actually happens there - CF events and errors.
* **BAD**. Even though they say that ["What goes in this property is raw CloudFormation template syntax"](https://serverless.com/framework/docs/providers/aws/guide/resources/), it's not 100% true: you can't do
  ```
  Value: !GetAtt DummyWebsiteBucket.WebsiteURL
  ```
  you have to do
  ```
  Value: { "Fn::GetAtt": ["DummyWebsiteBucket", "WebsiteURL"] }
  ```
  instead. The situation is even worse with `!Sub`, because whenever you have a CF expression (e.g. `${ThatResource.ThatAttribute}`), Serverless tries to interpret it, which sure result in an error. To work this around, there are plugins like [serverless-cf-vars](https://www.npmjs.com/package/serverless-cf-vars) - they allow you to use CF variables using `#{Something}` instead of `${Something}` (which sure makes your CF template non-CF-compatible).
* **BAD**. Serverless doesn't have built-in support for S3 deployments, but there's a number of (low quality) 3-rd party plugins:
  * [serverless-s3-assets](https://www.npmjs.com/package/serverless-s3-assets) - both deployment and undeployment work, but it doesn't set the valid mime type for uploaded files automatically. As a result, *.html files are served as plain text.
  * [serverless-s3-deploy](https://www.npmjs.com/package/serverless-s3-deploy) - doesn't delete files from S3 bucket before undeployment.
  * [serverless-s3bucket-sync](https://www.npmjs.com/package/serverless-s3bucket-sync) - doesn't delete files from S3 bucket before undeployment.
  * [serverless-sync-s3buckets](https://www.npmjs.com/package/serverless-sync-s3buckets) - doesn't delete files from S3 bucket before undeployment.
  * [serverless-s3-sync](https://www.npmjs.com/package/serverless-s3-sync) - both deployment and undeployment work, but it doesn't set ACL to public-read. As a result, you have to apply a bucket policy to allow public reads.
