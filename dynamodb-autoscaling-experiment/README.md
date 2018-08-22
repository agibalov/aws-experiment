# dynamodb-autoscaling-experiment

* `./tool deploy` to deploy the stack.
* `./tool undeploy` to undeploy the stack.
* `./gradlew clean test` to run the test.

**NOTE: scaling doesn't happen in real time, this test is not actually a test, but a demo, and its execution time is set to 30 minutes.** The scenario is: have provisioned WCU set to 1 initially, do a lot of `putItem()` requests, see AWS increases the provisioned WCU.

![demo](https://github.com/agibalov/aws-experiment/raw/master/dynamodb-autoscaling-experiment/demo.png)

https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/AutoScaling.html:

> **Important**
  Currently, Auto Scaling does not scale down your provisioned capacity if your table’s consumed capacity becomes zero. As a workaround, you can send requests to the table until Auto Scaling scales down to the minimum capacity, or change the policy to reduce the maximum provisioned capacity to be the same as the minimum provisioned capacity.
