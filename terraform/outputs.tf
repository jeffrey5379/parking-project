output "cloudfront_distribution_id" {
  description = "CloudFront distribution ID — needed for cache invalidation after deploy"
  value       = aws_cloudfront_distribution.main.id
}

output "app_url" {
  description = "Public URL of the application (frontend + API)"
  value       = "https://${aws_cloudfront_distribution.main.domain_name}"
}

output "ecr_repository_url" {
  description = "ECR repository — push your Docker image here"
  value       = aws_ecr_repository.backend.repository_url
}

output "frontend_bucket" {
  description = "S3 bucket — upload your React build here"
  value       = aws_s3_bucket.frontend.bucket
}

output "alb_dns" {
  description = "ALB DNS (for debugging; CloudFront is the public entry point)"
  value       = aws_lb.main.dns_name
}

output "sqs_queue_name" {
  description = "SQS queue name (passed as SQS_QUEUE_NAME env var)"
  value       = aws_sqs_queue.relocation.name
}

output "db_host" {
  description = "RDS endpoint host"
  value       = aws_db_instance.main.address
}

output "db_secret_arn" {
  description = "Secrets Manager ARN containing DB username and password"
  value       = aws_secretsmanager_secret.db.arn
}
