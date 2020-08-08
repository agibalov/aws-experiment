resource "aws_cloudwatch_log_group" "app" {
  name = var.app_env_tag
  retention_in_days = 1
}

resource "aws_iam_role" "app_execution" {
  assume_role_policy = jsonencode({
    Version = "2012-10-17",
    Statement = [{
      Principal = {
        Service = "ecs-tasks.amazonaws.com"
      },
      Effect = "Allow",
      Action = "sts:AssumeRole"
    }]
  })
}

resource "aws_iam_role_policy_attachment" "app_execution_role_AmazonECSTaskExecutionRolePolicy_attachment" {
  role = aws_iam_role.app_execution.name
  policy_arn = "arn:aws:iam::aws:policy/service-role/AmazonECSTaskExecutionRolePolicy"
}

resource "aws_iam_role" "app_task" {
  assume_role_policy = jsonencode({
    Version = "2012-10-17",
    Statement = [{
      Principal = {
        Service = "ecs-tasks.amazonaws.com"
      },
      Effect = "Allow",
      Action = "sts:AssumeRole"
    }]
  })
}

resource "aws_ecs_cluster" "cluster" {
  name = var.app_env_tag
}

data "aws_region" "current" {
}

locals {
  container_name = "app"
  container_port = 8080
  http_listener_port = 80
  https_listener_port = 443
}

resource "aws_ecs_task_definition" "app" {
  depends_on = [null_resource.image_in_ecr, null_resource.db_migration]
  cpu = 512
  memory = 1024
  execution_role_arn = aws_iam_role.app_execution.arn
  task_role_arn = aws_iam_role.app_task.arn
  family = var.app_env_tag
  network_mode = "awsvpc"
  requires_compatibilities = ["FARGATE"]
  container_definitions = jsonencode([{
    name = local.container_name
    portMappings = [{
      containerPort = local.container_port
    }]
    logConfiguration = {
      logDriver = "awslogs"
      options = {
        "awslogs-group" = aws_cloudwatch_log_group.app.name
        "awslogs-region" = data.aws_region.current.name
        "awslogs-stream-prefix" = "ecs"
      }
    }
    image = "${aws_ecr_repository.ecr.repository_url}:${local.build_tag}"
    environment = [
      { name = "APP_ENV_TAG", value = var.app_env_tag },
      { name = "MYSQL_HOST", value = data.terraform_remote_state.shared.outputs.db_host },
      { name = "MYSQL_PORT", value = tostring(data.terraform_remote_state.shared.outputs.db_port) },
      { name = "MYSQL_DATABASE", value = mysql_database.db.name },
      { name = "MYSQL_PROPERTIES", value = "" },
      { name = "MYSQL_USERNAME", value = mysql_user.user.user },
      { name = "MYSQL_PASSWORD", value = random_password.password.result }
    ]
  }])
}

resource "aws_ecs_service" "app" {
  name = "app"
  depends_on = [aws_lb_listener.http, aws_lb_listener.https]
  cluster = aws_ecs_cluster.cluster.id
  task_definition = aws_ecs_task_definition.app.arn
  desired_count = 1
  launch_type = "FARGATE"
  network_configuration {
    assign_public_ip = true
    security_groups = [aws_security_group.ecs.id]
    subnets = data.terraform_remote_state.shared.outputs.public_subnet_ids
  }
  load_balancer {
    container_name = local.container_name
    container_port = local.container_port
    target_group_arn = aws_alb_target_group.app.arn
  }
}

resource "aws_security_group" "alb" {
  name = "${var.app_env_tag}-alb"
  vpc_id = data.terraform_remote_state.shared.outputs.vpc_id
  ingress {
    protocol = "tcp"
    from_port = local.http_listener_port
    to_port = local.http_listener_port
    cidr_blocks = ["0.0.0.0/0"]
  }
  ingress {
    protocol = "tcp"
    from_port = local.https_listener_port
    to_port = local.https_listener_port
    cidr_blocks = ["0.0.0.0/0"]
  }
  egress {
    protocol = "tcp"
    from_port = local.container_port
    to_port = local.container_port
    cidr_blocks = ["0.0.0.0/0"]
  }
}

resource "aws_security_group" "ecs" {
  name = "${var.app_env_tag}-ecs"
  vpc_id = data.terraform_remote_state.shared.outputs.vpc_id
  ingress {
    protocol = "tcp"
    from_port = local.container_port
    to_port = local.container_port
    security_groups = [aws_security_group.alb.id]
  }
  egress {
    protocol = "-1"
    from_port = 0
    to_port = 0
    cidr_blocks = ["0.0.0.0/0"]
  }
}

resource "aws_lb" "alb" {
  load_balancer_type = "application"
  security_groups = [aws_security_group.alb.id]
  subnets = data.terraform_remote_state.shared.outputs.public_subnet_ids
  internal = false
  idle_timeout = 300
}

resource "aws_lb_listener" "http" {
  load_balancer_arn = aws_lb.alb.arn
  port = local.http_listener_port
  protocol = "HTTP"
  default_action {
    type = "redirect"
    redirect {
      host = "#{host}"
      path = "/#{path}"
      port = local.https_listener_port
      protocol = "HTTPS"
      query = "#{query}"
      status_code = "HTTP_301"
    }
  }
}

resource "aws_lb_listener" "https" {
  load_balancer_arn = aws_lb.alb.arn
  port = local.https_listener_port
  protocol = "HTTPS"
  certificate_arn = data.terraform_remote_state.dns.outputs.certificate_arn
  default_action {
    type = "forward"
    target_group_arn = aws_alb_target_group.app.arn
  }
}

resource "aws_alb_target_group" "app" {
  vpc_id = data.terraform_remote_state.shared.outputs.vpc_id
  port = local.container_port
  protocol = "HTTP"
  health_check {
    interval = 15
    path = "/"
    protocol = "HTTP"
    timeout = 2
    healthy_threshold = 10
    unhealthy_threshold = 10
  }
  deregistration_delay = 10
  target_type = "ip"
}

resource "aws_route53_record" "app" {
  zone_id = data.terraform_remote_state.dns.outputs.zone_id
  name = var.app_env_tag == "dev" ? "" : var.app_env_tag
  type = "A"
  alias {
    evaluate_target_health = false
    name = aws_lb.alb.dns_name
    zone_id = aws_lb.alb.zone_id
  }
}
