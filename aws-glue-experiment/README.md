# aws-glue-experiment

An AWS Glue hello world that exports data from Mysql database to S3 bucket.

* `./tool.sh deploy` to deploy.
* `./tool.sh undeploy` to undeploy. *This fails because Glue creates a bunch of network interfaces when you run the job, and if you try to delete your subnets/security groups, CF complains that they're in use by those network interfaces. Run `undeploy`, then wait for it report failure, then manually delete the network interfaces, then re-run `undeploy`.*
* `./tool.sh start-job` to start a RDS-to-s3 job. It takes 20-30 minutes to finish. Then go to the bucket and see the contents of `export1`.
