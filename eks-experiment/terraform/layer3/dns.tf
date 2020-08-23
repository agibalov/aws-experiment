data "aws_elb_hosted_zone_id" "current" {
}

resource "aws_route53_record" "eks_experiment_app" {
  zone_id = data.terraform_remote_state.layer1.outputs.zone_id
  name = var.env_tag == "dev" ? "" : var.env_tag
  type = "A"

  alias {
    evaluate_target_health = false
    name = kubernetes_service.aws_eks_experiment.load_balancer_ingress[0].hostname
    zone_id = data.aws_elb_hosted_zone_id.current.id
  }
}
