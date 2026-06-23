resource "aws_sqs_queue" "relocation_dlq" {
  name                      = "${local.name}-relocation-dlq"
  message_retention_seconds = 1209600 # 14 days — time to investigate failures
  tags                      = { Name = "${local.name}-relocation-dlq" }
}

resource "aws_sqs_queue" "relocation" {
  name                       = "${local.name}-relocation"
  message_retention_seconds  = 86400 # 1 day
  visibility_timeout_seconds = 30

  redrive_policy = jsonencode({
    deadLetterTargetArn = aws_sqs_queue.relocation_dlq.arn
    maxReceiveCount     = 3
  })

  tags = { Name = "${local.name}-relocation" }
}
