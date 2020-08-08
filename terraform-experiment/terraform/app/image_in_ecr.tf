locals {
  build_tag = split("-", uuid())[0]
}

resource "null_resource" "image_in_ecr" {
  triggers = {
    build_tag = local.build_tag
  }

  provisioner "local-exec" {
    command = "${path.module}/build-and-push-to-ecr.sh"
    environment = {
      REPOSITORY_URL = aws_ecr_repository.ecr.repository_url
      BUILD_TAG = local.build_tag
    }
  }
}
