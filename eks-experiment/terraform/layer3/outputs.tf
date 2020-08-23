output "http_url" {
  value = "http://${aws_route53_record.eks_experiment_app.fqdn}"
}

output "https_url" {
  value = "https://${aws_route53_record.eks_experiment_app.fqdn}"
}
