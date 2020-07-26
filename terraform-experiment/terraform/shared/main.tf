terraform {
  backend "s3" {
  }
}

provider "aws" {
  version = "~> 2.0"
}
