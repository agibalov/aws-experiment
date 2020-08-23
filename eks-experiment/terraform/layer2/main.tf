terraform {
  backend "s3" {
  }
}

provider "aws" {
  version = "~> 2.0"
}

locals {
  cluster_name = "eks-experiment"
}

data "terraform_remote_state" "layer1" {
  backend = "s3"
  config = {
    bucket = var.state_bucket
    key = var.layer1_state_key
  }
}
