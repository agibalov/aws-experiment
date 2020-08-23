data "aws_caller_identity" "current" {
}

resource "aws_iam_role" "kubernetes_deployment_role" {
  name = "eks-experiment-deployer"
  assume_role_policy = jsonencode({
    Version = "2012-10-17",
    Statement = [{
      Principal = {
        AWS = "arn:aws:iam::${data.aws_caller_identity.current.account_id}:root"
      },
      Effect = "Allow",
      Action = "sts:AssumeRole"
    }, {
      Principal = {
        Service = "codebuild.amazonaws.com"
      },
      Effect = "Allow",
      Action = "sts:AssumeRole"
    }]
  })
}

resource "aws_iam_role_policy" "kubernetes_deployment_role_allow_everything_policy" {
  name = "AllowEverything"
  role = aws_iam_role.kubernetes_deployment_role.name
  policy = jsonencode({
    Version = "2012-10-17",
    Statement = [{
      Effect = "Allow",
      Action = "*",
      Resource = "*"
    }]
  })
}
