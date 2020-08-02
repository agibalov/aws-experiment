output "app_url" {
  value = "https://${aws_route53_record.app.fqdn}"
}
