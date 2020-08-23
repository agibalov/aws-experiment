output "zone_suffix" {
  value = var.zone_name
}

output "zone_id" {
  value = aws_route53_zone.zone.id
}

output "certificate_arn" {
  value = aws_acm_certificate.certificate.arn
}

output "kubernetes_deployment_role_arn" {
  value = aws_iam_role.kubernetes_deployment_role.arn
}
