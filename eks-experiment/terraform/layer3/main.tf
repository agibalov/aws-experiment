terraform {
  backend "s3" {
  }
}

provider "aws" {
  version = "~> 2.0"
}

data "terraform_remote_state" "layer1" {
  backend = "s3"
  config = {
    bucket = var.state_bucket
    key = var.layer1_state_key
  }
}

data "terraform_remote_state" "layer2" {
  backend = "s3"
  config = {
    bucket = var.state_bucket
    key = var.layer2_state_key
  }
}

locals {
  service_account_name = "${var.env_tag}-service-account"
}
