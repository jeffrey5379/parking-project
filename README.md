# Parking Lot Project

A full-stack parking lot management system: a Java 21 / Spring Boot 3 backend and a React frontend.

```
parking-project/
  backend/    Spring Boot REST API + JPA + SQS
  frontend/   React + Vite dashboard
  terraform/  AWS infrastructure (ECS Fargate, RDS, SQS, CloudFront)
```

## What it does

- **27 spots**: 9 Small, 9 Medium, 9 Large.
- Park a Motorcycle (needs Small+), Car (needs Medium+), or Truck (needs Large).
- The backend always picks the smallest compatible free spot first.
- **Relocation**: if no compatible spot is free but a larger spot holds a smaller vehicle, that vehicle is automatically relocated to free the spot. The destination spot shows a pulsing "Moving…" state for 5–10 s while the SQS-driven async move completes.
- **Park Concurrently**: park multiple vehicles at once using one virtual thread per vehicle; each thread runs through the same optimistic-locking retry path.
- A "Reset lot" button frees every spot at once.
- The UI polls the backend every 5 seconds and refreshes immediately after every action.

## Local quick start

**Prerequisites:** JDK 21, Maven 3.8+, Node.js 18+, Docker.

**1. Start ElasticMQ (SQS emulator)**

```bash
docker-compose up -d
```

**2. Start the backend**

```bash
cd backend
mvn spring-boot:run
```

Runs on `http://localhost:8080`. H2 console at `http://localhost:8080/h2-console`.

**3. Start the frontend**

```bash
cd frontend
npm install
npm run dev
```

Runs on `http://localhost:5173`.

## AWS deployment

Infrastructure is managed with Terraform. See [`terraform/`](terraform/) for details.

High-level architecture:
- **ECS Fargate** runs the Spring Boot container (pulled from ECR).
- **RDS PostgreSQL** stores parking spot state.
- **Amazon SQS** handles async relocation messages.
- **CloudFront** serves the React app from S3 and proxies `/api/*` to the ALB — no CORS required.

```bash
cd terraform
terraform init
terraform apply

# push backend image
aws ecr get-login-password | docker login --username AWS --password-stdin $(terraform output -raw ecr_repository_url)
cd ../backend && docker build -t $(cd ../terraform && terraform output -raw ecr_repository_url):latest .
docker push $(cd ../terraform && terraform output -raw ecr_repository_url):latest
aws ecs update-service --cluster parking-prod-cluster --service parking-prod-backend --force-new-deployment

# deploy frontend
cd ../frontend && npm run build
aws s3 sync dist/ s3://$(cd ../terraform && terraform output -raw frontend_bucket)/ --delete
aws cloudfront create-invalidation \
  --distribution-id $(cd ../terraform && terraform output -raw cloudfront_distribution_id) \
  --paths "/*"

terraform output app_url
```
