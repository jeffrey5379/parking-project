resource "random_password" "db" {
  length  = 24
  special = false # avoid chars that break JDBC URLs
}

resource "aws_secretsmanager_secret" "db" {
  name                    = "${local.name}/db-credentials"
  recovery_window_in_days = 0 # allow immediate deletion; increase for production data
}

resource "aws_secretsmanager_secret_version" "db" {
  secret_id = aws_secretsmanager_secret.db.id
  secret_string = jsonencode({
    username = var.db_username
    password = random_password.db.result
  })
}

resource "aws_db_subnet_group" "main" {
  name       = "${local.name}-db-subnet-group"
  subnet_ids = aws_subnet.private[*].id
  tags       = { Name = "${local.name}-db-subnet-group" }
}

resource "aws_db_instance" "main" {
  identifier             = "${local.name}-postgres"
  engine                 = "postgres"
  engine_version         = "16"
  instance_class         = "db.t3.micro"
  allocated_storage      = 20
  storage_type           = "gp2"
  db_name                = var.db_name
  username               = var.db_username
  password               = random_password.db.result
  db_subnet_group_name   = aws_db_subnet_group.main.name
  vpc_security_group_ids = [aws_security_group.rds.id]
  publicly_accessible    = false
  multi_az               = false  # set true for HA (doubles cost)
  skip_final_snapshot    = true   # set false and add final_snapshot_identifier for production data
  deletion_protection    = false

  tags = { Name = "${local.name}-postgres" }
}
