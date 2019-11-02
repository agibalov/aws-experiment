# vpc-flow-logs-experiment

Flow log is a mechanism to explore the traffic within VPC.

* `./tool.sh deploy` to deploy.
* Ping the dummy instance and see your IP address in the logs.
* Try to telnet to dummy instance at port 12345. Telnet will say "connection refused" immediately. See 2 records (request and response) in the log mentioning 12345 and "ACCEPT". That's because `InstanceSecurityGroup` allows 12345.
* Try to telnet to dummy instance at port 54321. Telnet will say "Connection timed out" after some time. See 1 record (request) in the log mentioning 54321 and "REJECT". That's because `InstanceSecurityGroup` doesn't allow 54321.
* `./tool.sh undeploy` to undeploy.

Notes:

1. There's high latency - 10+ minutes
2. You can't specify the log format in `AWS::EC2::FlowLog` and the default format is hard to read. AWS CLI allows you to specify the format.
3. May not be super useful in reality, because it only reports whether the traffic is being delivered to the instance, but it doesn't indicate if the instance actually accepts it or not.  
