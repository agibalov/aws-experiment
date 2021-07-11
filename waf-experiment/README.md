# waf-experiment

An AWS WAF hello world. Demonstrates:

* AWS managed rule set
* Rate limiting

## How to deploy and undeploy

* `./tool.sh deploy-ecs` and `./tool.sh undeploy-ecs` to deploy and undeploy the ecs stack. This includes VPC, SGs, ECS cluster, ECR, etc.
* `./tool.sh deploy-app` and `./tool.sh undeploy-app` to deploy and undeploy the app stack.

## AWSManagedRulesCommonRuleSet

Look up the app stack's `Url` output and try going to:

* `/` - should work fine
* `/<script>` - 403
* `/<script1>` - should work fine
* `/?x=123` - should work fine
* `/?x=<script>` - 403

etc. See `AWSManagedRulesCommonRuleSet` [here](https://docs.aws.amazon.com/waf/latest/developerguide/aws-managed-rule-groups-list.html).

## Rate limiting

* Run `./tool.sh test`
* See how all responses are 200
* After 100 (more like 300) requests, you should start seeing the 429s
* If you go to `/` now, the page should say "Hey stop it!", and there will be a response header `MyWebAclMessage` saying `That's too much!`

## Notes

* WAF itself doesn't introduce any new endpoints - it just magically gets applied to the existing LB.
