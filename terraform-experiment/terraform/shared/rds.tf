resource "random_password" "db_instance_master_password" {
  length = 16
  special = false
}

resource "aws_db_instance" "db_instance" {
  identifier = var.shared_env_tag
  engine = "mysql"
  engine_version = "8.0.16"
  allow_major_version_upgrade = true
  instance_class = "db.t3.micro"
  storage_type = "gp2"
  allocated_storage = 5
  username = "master"
  password = random_password.db_instance_master_password.result
  publicly_accessible = true
  db_subnet_group_name = aws_db_subnet_group.db_subnet_group.name
  vpc_security_group_ids = [aws_security_group.db_security_group.id]
  backup_retention_period = 1
  delete_automated_backups = true
  storage_encrypted = false
  skip_final_snapshot = true
}

resource "aws_security_group" "db_security_group" {
  vpc_id = aws_vpc.vpc.id
  description = "Mysql public access"
  ingress {
    protocol = "tcp"
    from_port = 3306
    to_port = 3306
    cidr_blocks = ["0.0.0.0/0"]
  }
}

resource "aws_db_subnet_group" "db_subnet_group" {
  subnet_ids = aws_subnet.public[*].id
}
