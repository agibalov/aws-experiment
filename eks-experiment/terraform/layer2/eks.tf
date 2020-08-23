provider "aws" {
  alias = "kubernetes_aws"
  assume_role {
    role_arn = data.terraform_remote_state.layer1.outputs.kubernetes_deployment_role_arn
  }
}

resource "aws_eks_cluster" "cluster" {
  provider = aws.kubernetes_aws

  name = local.cluster_name
  role_arn = aws_iam_role.cluster_role.arn
  vpc_config {
    subnet_ids = aws_subnet.public[*].id
  }
}

resource "aws_iam_role" "cluster_role" {
  assume_role_policy = jsonencode({
    Version = "2012-10-17",
    Statement = [{
      Principal = {
        Service = "eks.amazonaws.com"
      },
      Effect = "Allow",
      Action = "sts:AssumeRole"
    }]
  })
}

resource "aws_iam_role_policy_attachment" "cluster_role_AmazonEKSClusterPolicy_attachment" {
  policy_arn = "arn:aws:iam::aws:policy/AmazonEKSClusterPolicy"
  role = aws_iam_role.cluster_role.name
}

resource "aws_iam_role_policy_attachment" "cluster_role_AmazonEKSServicePolicy_attachment" {
  policy_arn = "arn:aws:iam::aws:policy/AmazonEKSServicePolicy"
  role = aws_iam_role.cluster_role.name
}

resource "aws_eks_node_group" "node_group" {
  cluster_name = aws_eks_cluster.cluster.name
  node_group_name = "node-group-1"
  node_role_arn = aws_iam_role.node_role.arn
  subnet_ids = aws_subnet.public[*].id
  instance_types = ["t3.medium"]
  scaling_config {
    desired_size = 1
    max_size = 1
    min_size = 1
  }
}

resource "aws_iam_role" "node_role" {
  assume_role_policy = jsonencode({
    Version = "2012-10-17",
    Statement = [{
      Principal = {
        Service = "ec2.amazonaws.com"
      },
      Effect = "Allow",
      Action = "sts:AssumeRole"
    }]
  })
}

resource "aws_iam_role_policy_attachment" "node_role_AmazonEKSWorkerNodePolicy_attachment" {
  policy_arn = "arn:aws:iam::aws:policy/AmazonEKSWorkerNodePolicy"
  role = aws_iam_role.node_role.name
}

resource "aws_iam_role_policy_attachment" "node_role_AmazonEKS_CNI_Policy_attachment" {
  policy_arn = "arn:aws:iam::aws:policy/AmazonEKS_CNI_Policy"
  role = aws_iam_role.node_role.name
}

resource "aws_iam_role_policy_attachment" "node_role_AmazonEC2ContainerRegistryReadOnly_attachment" {
  policy_arn = "arn:aws:iam::aws:policy/AmazonEC2ContainerRegistryReadOnly"
  role = aws_iam_role.node_role.name
}

data "aws_region" "current" {
}

data "external" "thumbprint" {
  program = ["${path.module}/thumbprint.sh", data.aws_region.current.name]
}

// https://github.com/terraform-providers/terraform-provider-aws/issues/10104#issuecomment-632309246
resource "aws_iam_openid_connect_provider" "openid_connect_provider" {
  client_id_list = ["sts.amazonaws.com"]
  thumbprint_list = [data.external.thumbprint.result.thumbprint]
  url = aws_eks_cluster.cluster.identity[0].oidc[0].issuer
}
