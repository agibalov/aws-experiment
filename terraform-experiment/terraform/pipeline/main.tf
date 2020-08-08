terraform {
  backend "s3" {
  }
}

provider "aws" {
  version = "~> 2.0"
}

resource "aws_cloudwatch_log_group" "project_log_group" {
  name = "${var.app_env_tag}-project"
  retention_in_days = 1
}

resource "aws_iam_role" "project_service_role" {
  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = {
      Effect = "Allow"
      Principal = {
        Service = "codebuild.amazonaws.com"
      },
      Action = "sts:AssumeRole"
    }
  })
}

resource "aws_iam_role_policy" "project_service_role_allow_everything_policy" {
  role = aws_iam_role.project_service_role.id
  name = "AllowEverything"
  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Effect = "Allow"
      Action = "*"
      Resource = "*"
    }]
  })
}

resource "aws_codebuild_project" "project" {
  name = "${var.app_env_tag}-project"
  build_timeout = 60
  artifacts {
    type = "NO_ARTIFACTS"
  }
  environment {
    compute_type = "BUILD_GENERAL1_SMALL"
    image = "aws/codebuild/standard:3.0"
    type = "LINUX_CONTAINER"
    privileged_mode = true
    environment_variable {
      name = "SHARED_ENV_TAG"
      value = var.shared_env_tag
    }
    environment_variable {
      name = "APP_ENV_TAG"
      value = var.app_env_tag
    }
    environment_variable {
      name = "TF_CLI_ARGS"
      value = "-no-color"
    }
  }
  service_role = aws_iam_role.project_service_role.arn
  source {
    type = "GITHUB"
    location = "https://github.com/agibalov/aws-experiment.git"
    buildspec = "terraform-experiment/buildspec.yml"
  }
  source_version = var.branch_name
  logs_config {
    cloudwatch_logs {
      status = "ENABLED"
      group_name = aws_cloudwatch_log_group.project_log_group.name
    }
  }
}

resource "aws_codebuild_webhook" "project_webhook" {
  project_name = aws_codebuild_project.project.name
  filter_group {
    filter {
      type = "EVENT"
      pattern = "PUSH"
    }
    filter {
      type = "HEAD_REF"
      pattern = var.branch_name
    }
  }
}
