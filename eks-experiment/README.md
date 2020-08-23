# eks-experiment

An AWS EKS hello world. 

* `./tool.sh init` and `./tool.sh deinit` to create and destroy an S3 bucket where Terraform will store the state files.
* `./tool.sh deploy-layer1` and `./tool.sh undeploy-layer1` to deploy and undeploy Layer 1 resources: DNS zone and Kubernetes deployment role.
* `./tool.sh deploy-layer2` and `./tool.sh undeploy-layer2` to deploy and undeploy Layer 2 resources: VPC and EKS cluster.
* `envTag=<EnvTag> ./tool.sh deploy-layer3` and `envTag=<EnvTag>  ./tool.sh undeploy-layer3` to deploy and undeploy Layer 3 resources: Kubernetes deployment, service, DNS records.

## Notes

1. You can only interact with EKS cluster if you use the same very role used to create that EKS cluster. If you create a cluster as IAM user A and then decide to set up a build pipeline, it won't have access to the cluster unless you make it use the user A's credentials. The solution is to create a special IAM role (see "kubernetes_deployment_role") and use it to create and then interact with the cluster.  
2. Because [STS doesn't allow AWS root users to assume roles](https://docs.aws.amazon.com/cli/latest/reference/sts/assume-role.html), you have to use an IAM user credentials when running `tool.sh`.
3. **Not done:** redirect HTTP -> HTTPS.
4. **Not done:** CloudWatch logs.
