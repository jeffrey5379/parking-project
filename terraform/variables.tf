variable "aws_region" {
  description = "AWS region"
  type        = string
  default     = "us-east-1"
}

variable "project" {
  description = "Short prefix applied to every resource name"
  type        = string
  default     = "parking"
}

variable "environment" {
  description = "Deployment environment (prod, staging …)"
  type        = string
  default     = "prod"
}

variable "db_name" {
  description = "PostgreSQL database name"
  type        = string
  default     = "parking"
}

variable "db_username" {
  description = "PostgreSQL admin username"
  type        = string
  default     = "parking"
}

variable "backend_cpu" {
  description = "ECS task CPU units (256 = 0.25 vCPU)"
  type        = number
  default     = 256
}

variable "backend_memory" {
  description = "ECS task memory in MiB"
  type        = number
  default     = 512
}

variable "backend_desired_count" {
  description = "Number of ECS backend tasks to keep running"
  type        = number
  default     = 1
}
