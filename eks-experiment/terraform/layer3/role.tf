data "aws_eks_cluster" "cluster" {
  name = data.terraform_remote_state.layer2.outputs.cluster_name
}

data "aws_caller_identity" "current" {
}

locals {
  cluster_oidc_issuer = data.aws_eks_cluster.cluster.identity[0].oidc[0].issuer
}

resource "aws_iam_role" "pod_role" {
  assume_role_policy = jsonencode({
    Version = "2012-10-17",
    Statement = [{
      Principal = {
        Federated = "arn:aws:iam::${data.aws_caller_identity.current.account_id}:oidc-provider/${replace(local.cluster_oidc_issuer, "https://", "")}"
      },
      Effect = "Allow",
      Action = "sts:AssumeRoleWithWebIdentity",
      Condition = {
        StringEquals = {
          "${replace(local.cluster_oidc_issuer, "https://", "")}:sub" = "system:serviceaccount:default:${local.service_account_name}"
        }
      }
    }]
  })
}

resource "aws_iam_role_policy" "pod_role_list_buckets_policy" {
  name = "AllowListBuckets"
  role = aws_iam_role.pod_role.name
  policy = jsonencode({
    Version = "2012-10-17",
    Statement = [{
      Effect = "Allow",
      Action = "s3:ListAllMyBuckets",
      Resource = "*"
    }]
  })
}
