# vpc-experiment

## nat.yml

A set up with one public subnet and one private subnet. Private subnet uses a NAT gateway to talk to the internet.

* `./tool.sh deploy` and `./tool.sh undeploy` to deploy and undeploy the stack.
* `./tool.sh ssh-to-instance-a` to SSH to InstanceA.

The scenario is:

1. SSH to instance A.
2. Try `curl retask.me`, `ping <put ip of instance B here>` and `aws s3api list-buckets` - everything should work.
3. From instance A do `ssh -i key.pem ec2-user@<put ip of instance B here>` and try the same there (instead of making it ping itself, make it ping instance A) - everything should work as well.
4. Now, you may remove the NAT gateway and co, reconnect to instance B and try all the same commands. Only `ping <instance A>` should work.
