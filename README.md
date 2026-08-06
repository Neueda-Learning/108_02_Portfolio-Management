# Portfolio Management

A full-stack portfolio management app built with Spring Boot (backend) and React + Vite (frontend).

## Features

- Multi-user portfolio tracking
- Buy/sell assets (stocks, ETFs, crypto, bonds, etc.)
- Wallet balance and transaction history
- Portfolio allocation and performance views
- Market stats page with historical graphs (`1W`, `1M`, `1Y`)

## Tech Stack

- **Backend:** Java 17, Spring Boot, Maven, MySQL
- **Frontend:** React, Vite, Tailwind CSS

## Docker / Jenkins Deployment (Linux)

This repository includes production deployment files for backend, frontend, and MySQL using Docker and Jenkins:

- `Jenkinsfile`
- `Dockerfile.backend`
- `frontend/Dockerfile`
- `frontend/nginx.conf`
- `docker-compose.yml`
- `docker-compose.prod.yml`
- `.env.example`
- `.dockerignore`

Database defaults are configured to use:

- MySQL root user: `root`
- Password: `n3u3da!`

## Project Structure

- `src/main/java` - Spring Boot application code
- `src/main/resources` - configuration and SQL schema
- `frontend/` - React client app

## Prerequisites

- Java 17+
- Maven 3.8+
- Node.js 18+
- MySQL 8+
- Docker Engine
- Docker Compose (`docker compose` or `docker-compose`)
- Jenkins (running on Linux server with Docker access)

## Configuration

Update database settings in:

- `src/main/resources/application.properties`

Default backend port is `8080`.

For Docker/Jenkins deployments, the app reads these environment variables:

- `DB_HOST`
- `DB_PORT`
- `DB_NAME`
- `DB_USER`
- `DB_PASSWORD`
- `SERVER_PORT`

## Run the Backend

```powershell
Set-Location "C:\Users\Administrator\108_02_Portfolio-Management"
.\mvnw.cmd spring-boot:run
```

## Run the Frontend

```powershell
Set-Location "C:\Users\Administrator\108_02_Portfolio-Management\frontend"
npm install
npm run dev
```

Frontend dev server runs on Vite default port (usually `5173`).

## Build

Backend:

```powershell
Set-Location "C:\Users\Administrator\108_02_Portfolio-Management"
.\mvnw.cmd -Dmaven.test.skip=true clean package
```

Frontend:

```powershell
Set-Location "C:\Users\Administrator\108_02_Portfolio-Management\frontend"
npm run build
```

## Docker Compose

Local/dev (build locally):

```powershell
Set-Location "C:\Users\Administrator\108_02_Portfolio-Management"
docker compose up --build
```

Production stack (pull prebuilt images):

```bash
cp .env.example .env
docker compose -f docker-compose.prod.yml pull
docker compose -f docker-compose.prod.yml up -d
```

## Jenkins CI/CD Pipeline

`Jenkinsfile` performs:

1. Checkout
2. Compose CLI detection (`docker compose` or `docker-compose`)
3. Backend build (`mvnw`)
4. Frontend build (Node container)
5. Docker image build + push
6. Deploy on `main` using `docker-compose.prod.yml`

Required Jenkins credential:

- `dockerhub-creds` (type: Username with password)

Important Jenkins parameters (defaulted in `Jenkinsfile`):

- `REGISTRY` (default `docker.io`)
- `REGISTRY_NAMESPACE` (default `admin`)
- `DOCKERHUB_CREDENTIALS_ID` (default `dockerhub-creds`)

## API Docs

Swagger UI is available when backend is running:

- `http://localhost:8080/swagger-ui/index.html`

## User Stories and Task Tracking

The complete user stories, task details, acceptance criteria, effort tracking, and development progress are available in the Excel document below:

[User Stories Excel File](https://drive.google.com/file/d/1yvIxXZolYCR7A0-qyF2Y-1kuDaQj9FlZ/view?usp=drive_link)

## Notes

- Database schema is initialized from `src/main/resources/schema.sql` at startup.
- Additional project docs are included in files like `QUICKSTART.md`, `HOW-TO-USE.md`, and `API_DOCUMENTATION.md`.

