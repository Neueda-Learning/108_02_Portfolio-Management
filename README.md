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

## Project Structure

- `src/main/java` - Spring Boot application code
- `src/main/resources` - configuration and SQL schema
- `frontend/` - React client app

## Prerequisites

- Java 17+
- Maven 3.8+
- Node.js 18+
- MySQL 8+

## Configuration

Update database settings in:

- `src/main/resources/application.properties`

Default backend port is `8080`.

## Run the Backend

```powershell
Set-Location "c:\Users\Administrator\Downloads\demo-main\108_02_Portfolio-Management-main\108_02_Portfolio-Management-main\Portfolio-Management"
mvn spring-boot:run
```

## Run the Frontend

```powershell
Set-Location "c:\Users\Administrator\Downloads\demo-main\108_02_Portfolio-Management-main\108_02_Portfolio-Management-main\Portfolio-Management\frontend"
npm install
npm run dev
```

Frontend dev server runs on Vite default port (usually `5173`).

## Build

Backend:

```powershell
Set-Location "c:\Users\Administrator\Downloads\demo-main\108_02_Portfolio-Management-main\108_02_Portfolio-Management-main\Portfolio-Management"
mvn clean package
```

Frontend:

```powershell
Set-Location "c:\Users\Administrator\Downloads\demo-main\108_02_Portfolio-Management-main\108_02_Portfolio-Management-main\Portfolio-Management\frontend"
npm run build
```

## API Docs

Swagger UI is available when backend is running:

- `http://localhost:8080/swagger-ui/index.html`

## Notes

- Database schema is initialized from `src/main/resources/schema.sql` at startup.
- Additional project docs are included in files like `QUICKSTART.md`, `HOW-TO-USE.md`, and `API_DOCUMENTATION.md`.

