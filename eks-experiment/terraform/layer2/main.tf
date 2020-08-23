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
