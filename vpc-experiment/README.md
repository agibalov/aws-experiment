# vpc-experiment

* `./tool.sh deploy <setup>` and `./tool.sh undeploy` to deploy and undeploy the stack. `<setup>` can be one of `nat-gateway`, `nat-instance` and `vpc-endpoint`. Note: only one stack can be deployed at the same time.
* `./tool.sh ssh-to-instance-a` to SSH to InstanceA.

The scenario is:

1. SSH to instance A.
2. Try `curl retask.me`, `ping <put ip of instance B here>` and `aws s3api list-buckets` - everything should work.
3. From instance A do `ssh -i key.pem ec2-user@<put ip of instance B here>` and try the same there (instead of making it ping itself, make it ping instance A) - everything should work as well.
4. Now, you may remove the NAT gateway (NAT instance), reconnect to instance B and try all the same commands. Only `ping <instance A>` should work.

## nat-gateway

A set up with one public subnet, one private subnet and a NAT gateway.

## nat-instance

A set up with one public subnet, one private subnet and a NAT instance.

## vpc-endpoint

A set up with one public subnet, one private subnet and an S3 VPC Endpoint. While `curl retask.me` sure won't work, `aws s3api list-buckets` works.
