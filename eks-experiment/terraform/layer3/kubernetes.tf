provider "aws" {
  alias = "kubernetes_aws"
  assume_role {
    role_arn = data.terraform_remote_state.layer1.outputs.kubernetes_deployment_role_arn
  }
}

data "aws_eks_cluster_auth" "cluster" {
  provider = aws.kubernetes_aws
  name = data.terraform_remote_state.layer2.outputs.cluster_name
}

provider "kubernetes" {
  load_config_file = false
  host = data.terraform_remote_state.layer2.outputs.cluster_endpoint
  cluster_ca_certificate = base64decode(data.terraform_remote_state.layer2.outputs.cluster_certificate_authority_data)
  token = data.aws_eks_cluster_auth.cluster.token
}

resource "kubernetes_service_account" "service_account" {
  metadata {
    name = local.service_account_name
    annotations = {
      "eks.amazonaws.com/role-arn": aws_iam_role.pod_role.arn
    }
  }
  automount_service_account_token = true
}

resource "kubernetes_deployment" "aws_eks_experiment" {
  metadata {
    name = "${var.env_tag}-eks-experiment"
  }

  spec {
    replicas = 1
    selector {
      match_labels = {
        app = "${var.env_tag}-eks-experiment"
      }
    }
    template {
      metadata {
        labels = {
          app = "${var.env_tag}-eks-experiment"
        }
      }
      spec {
        service_account_name = kubernetes_service_account.service_account.metadata[0].name
        automount_service_account_token = true

        // Containers that don't run as root fail to read the token file
        // https://github.com/kubernetes-sigs/external-dns/pull/1185#issuecomment-530439786
        security_context {
          fs_group = 65534
        }

        container {
          image = "nginx"
          name  = "app"

          resources {
            limits {
              cpu = "0.5"
              memory = "1024Mi"
            }
          }

          port {
            protocol = "TCP"
            #container_port = 8080
            container_port = 80
          }
        }
      }
    }
  }
}

resource "kubernetes_service" "aws_eks_experiment" {
  metadata {
    name = "${var.env_tag}-eks-experiment"
    annotations = {
      "service.beta.kubernetes.io/aws-load-balancer-ssl-cert" = data.terraform_remote_state.layer1.outputs.certificate_arn
      "service.beta.kubernetes.io/aws-load-balancer-backend-protocol" = "http"
      "service.beta.kubernetes.io/aws-load-balancer-ssl-ports" = "https"
    }
  }

  spec {
    type = "LoadBalancer"
    port {
      name = "http"
      port = 80
      #target_port = 8080
      target_port = 80
    }
    port {
      name = "https"
      port = 443
      #target_port = 8080
      target_port = 80
    }
    selector = {
      app = "${var.env_tag}-eks-experiment"
    }
  }
}
