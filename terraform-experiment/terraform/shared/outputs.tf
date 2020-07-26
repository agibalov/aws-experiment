output "vpc_id" {
  value = aws_vpc.vpc.id
}

output "public_subnet_ids" {
  value = aws_subnet.public[*].id
}

output "db_host" {
  value = aws_db_instance.db_instance.address
}

output "db_port" {
  value = aws_db_instance.db_instance.port
}

output "db_master_username" {
  value = aws_db_instance.db_instance.username
}

output "db_master_password" {
  value = aws_db_instance.db_instance.password
}
