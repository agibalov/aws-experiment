# ecs-experiment

**NOTE: requires AWS CLI v2**

An ECS hello world, illustrates both EC2 and Fargate launch types.

* `./tool.sh deploy-ecs` and `./tool.sh undeploy-ecs` to deploy and undeploy the ECS stack. This includes VPC, SGs, ECS cluster, ECR, etc. This part is common no matter if you want EC2 or Fargate.

The following commands require the ECS stack to be deployed first.

* `./tool.sh deploy-ec2-app` and `./tool.sh undeploy-ec2-app` to deploy and undeploy the app with EC2 launch type. The stack name is "Ec2App" and it exposes the app URL as `Url` output.
* `./tool.sh deploy-fargate-app` and `./tool.sh undeploy-fargate-app` to deploy and undeploy the app with Fargate launch type. The stack name is "FargateApp" and it exposes the app URL as `Url` output.
* `./tool.sh deploy-fargate-app-with-dns` and `./tool.sh undeploy-fargate-app-with-dns` to deploy and undeploy the app with Fargate launch type, a nice domain name and HTTPS. The stack name is "FargateAppWithDns" and it exposes the app load balancer URL as `LoadBalancerUrl` and the "nice domain" URL as `CustomDomainNameUrl`. NOTE that when deploying this stack for the first time, it requests the SSL certificate from ACM for the domain. This requires a manual action - going to the inbox and confirming the domain ownership. However, the next deployments don't require this because the certificate remains the same.

## Notes

* The only big difference between EC2 and Fargate launch types is that with EC2 you're responsible for supplying the EC2s (ASG, launch configurations, etc), while with Fargate you don't do this.
* No easy way to get a static IP address for the service - you have to use ALB even if you just have one instance and don't need load balancing. However, if what you actually need is a static domain name, you just need an `A` DNS record that would be an alias for the load balancer's domain name (see `fargate-app-with-dns.yml`)
