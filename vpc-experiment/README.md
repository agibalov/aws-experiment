# vpc-experiment

* `./tool.sh deploy <setup>` and `./tool.sh undeploy` to deploy and undeploy the stack. `<setup>` can be one of `nat-gateway` and `nat-instance`. Note: only one stack can be deployed at the same time (undeploy nat-gateway before deploying nat-instance)
* `./tool.sh ssh-to-instance-a` to SSH to InstanceA.

The scenario is:

1. SSH to instance A.
2. Try `curl retask.me`, `ping <put ip of instance B here>` and `aws s3api list-buckets` - everything should work.
3. From instance A do `ssh -i key.pem ec2-user@<put ip of instance B here>` and try the same there (instead of making it ping itself, make it ping instance A) - everything should work as well.
4. Now, you may remove the NAT gateway (NAT instance), reconnect to instance B and try all the same commands. Only `ping <instance A>` should work.

## nat-gateway.yml

A set up with one public subnet, one private subnet and a NAT gateway.

## nat-instance.yml

A set up with one public subnet, one private subnet and a NAT instance.
