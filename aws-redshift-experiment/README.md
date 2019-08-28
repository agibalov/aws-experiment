# aws-redshift-experiment

It deploys a Redshift cluster, imports the data from a CSV file in S3 bucket, and aggregates it, and saves the results to CSV files in the same S3 bucket.

* `./tool.sh deploy` to deploy.
* `./tool.sh undeploy` to undeploy.
* `./tool.sh test` to run test. After the test, a few `average-*` files should appear in the S3 bucket.
