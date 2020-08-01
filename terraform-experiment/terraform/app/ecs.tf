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

resource "aws_ecs_task_definition" "app" {
  depends_on = [null_resource.image_in_ecr]
  cpu = 512
  memory = 1024
  execution_role_arn = aws_iam_role.app_execution.arn
  task_role_arn = aws_iam_role.app_task.arn
  family = var.app_env_tag
  network_mode = "awsvpc"
  requires_compatibilities = ["FARGATE"]
  container_definitions = jsonencode([{
    name = "app"
    portMappings = [{
      containerPort = 8080 // TODO
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
    environment = [{
      name = "APP_ENV_TAG",
      value = var.app_env_tag
    }]
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
    container_name = "app" // TODO
    container_port = 8080 // TODO
    target_group_arn = aws_alb_target_group.app.arn
  }
}

resource "aws_security_group" "alb" {
  vpc_id = data.terraform_remote_state.shared.outputs.vpc_id
  ingress {
    protocol = "tcp"
    from_port = 80
    to_port = 80
    cidr_blocks = ["0.0.0.0/0"]
  }
}

resource "aws_security_group" "ecs" {
  vpc_id = data.terraform_remote_state.shared.outputs.vpc_id
  ingress {
    protocol = "tcp"
    from_port = 80
    to_port = 80
    cidr_blocks = ["0.0.0.0/0"]
  }
  ingress {
    protocol = "-1"
    from_port = 0
    to_port = 0
    security_groups = [aws_security_group.alb.id]
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
  port = 80
  protocol = "HTTP"
  default_action {
    type = "redirect"
    redirect {
      host = "#{host}"
      path = "/#{path}"
      port = 443
      protocol = "HTTPS"
      query = "#{query}"
      status_code = "HTTP_301"
    }
  }
}

resource "aws_lb_listener" "https" {
  load_balancer_arn = aws_lb.alb.arn
  port = 443
  protocol = "HTTPS"
  certificate_arn = data.terraform_remote_state.dns.outputs.certificate_arn
  default_action {
    type = "forward"
    target_group_arn = aws_alb_target_group.app.arn
  }
}

resource "aws_alb_target_group" "app" {
  vpc_id = data.terraform_remote_state.shared.outputs.vpc_id
  port = 8080 // TODO
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
