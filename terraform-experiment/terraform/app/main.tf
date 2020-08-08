terraform {
  backend "s3" {
  }
}

provider "aws" {
  version = "~> 2.0"
}

data "terraform_remote_state" "dns" {
  backend = "s3"
  config = {
    bucket = var.state_bucket_name
    key = var.dns_state_key
  }
}

data "terraform_remote_state" "shared" {
  backend = "s3"
  config = {
    bucket = var.state_bucket_name
    key = var.shared_state_key
  }
}
