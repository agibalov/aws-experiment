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
      containerPort = 8080
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
