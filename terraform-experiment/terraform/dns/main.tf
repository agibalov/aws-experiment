variable "parent_zone_name" {
  type = string
  default = "agibalov.io"
}

variable "zone_name" {
  type = string
  default = "tf-experiment.agibalov.io"
}

terraform {
  backend "s3" {
  }
}

provider "aws" {
  version = "~> 2.0"
}

resource "aws_route53_zone" "zone" {
  name = "${var.zone_name}."
}

data "aws_route53_zone" "parent_zone" {
  name = "${var.parent_zone_name}."
}

resource "aws_route53_record" "ns" {
  zone_id = data.aws_route53_zone.parent_zone.zone_id
  name = var.zone_name
  type = "NS"
  ttl = 30
  records = aws_route53_zone.zone.name_servers[*]
}

resource "aws_acm_certificate" "certificate" {
  domain_name = var.zone_name
  subject_alternative_names = ["*.${var.zone_name}"]
  validation_method = "DNS"
}

resource "aws_acm_certificate_validation" "certificate_validation" {
  certificate_arn = aws_acm_certificate.certificate.arn
  validation_record_fqdns = [aws_route53_record.certificate_validation_record.fqdn]
}

resource "aws_route53_record" "certificate_validation_record" {
  zone_id = aws_route53_zone.zone.zone_id
  name = aws_acm_certificate.certificate.domain_validation_options[0].resource_record_name
  type = aws_acm_certificate.certificate.domain_validation_options[0].resource_record_type
  ttl = 60
  records = [aws_acm_certificate.certificate.domain_validation_options[0].resource_record_value]
}

output "zone_suffix" {
  value = var.zone_name
}

output "zone_id" {
  value = aws_route53_zone.zone.id
}

output "certificate_arn" {
  value = aws_acm_certificate.certificate.arn
}
