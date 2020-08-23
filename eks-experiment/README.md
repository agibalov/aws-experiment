# eks-experiment

An AWS EKS hello world.

## Prerequisites

* AWS CLI v2
* Terraform v0.12.26
* openssl CLI
* Docker
* (optional) kubectl

## How to deploy and run

* `./tool.sh init` and `./tool.sh deinit` to create and destroy an S3 bucket where Terraform will store the state files.
* `./tool.sh deploy-layer1` and `./tool.sh undeploy-layer1` to deploy and undeploy Layer 1 resources: DNS zone and Kubernetes deployment role.
* `./tool.sh deploy-layer2` and `./tool.sh undeploy-layer2` to deploy and undeploy Layer 2 resources: VPC and EKS cluster.
* `envTag=<EnvTag> ./tool.sh deploy-layer3` and `envTag=<EnvTag>  ./tool.sh undeploy-layer3` to deploy and undeploy Layer 3 resources: Kubernetes deployment, service, DNS records. Make sure to `./gradlew clean bootJar` before deploying Layer 3.
* `envTag=<EnvTag> ./tool.sh deploy-pipeline` and `envTag=<EnvTag>  ./tool.sh undeploy-pipeline` to deploy and undeploy Pipeline resources: CodeBuild project, log group, etc. The pipeline only deploys Layer 3, you have to deploy Layers 1 and 2 manually.
* `./tool.sh update-kubeconfig` to update your kubectl config. After it you can use `kubectl` to interact with Kubernetes cluster directly: `kubectl get pods`, `kubectl logs <POD> --follow`, etc.

## Notes

1. You can only interact with EKS cluster if you use the same very role used to create that EKS cluster. If you create a cluster as IAM user A and then decide to set up a build pipeline, it won't have access to the cluster unless you make it use the user A's credentials. The solution is to create a special IAM role (see "kubernetes_deployment_role") and use it to create and then interact with the cluster.  
2. Because [STS doesn't allow AWS root users to assume roles](https://docs.aws.amazon.com/cli/latest/reference/sts/assume-role.html), you have to use an IAM user credentials when running `tool.sh`.
3. For Java AWS client authentication to work in this scenario (`WebIdentityTokenCredentialsProvider`), you have to have `aws-java-sdk-sts` as a dependency.  
3. **Not done:** redirect HTTP -> HTTPS.
4. **Not done:** CloudWatch logs.
