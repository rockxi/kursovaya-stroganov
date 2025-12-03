# WARP.md

This file provides guidance to WARP (warp.dev) when working with code in this repository.

## Project Overview

This is an online education platform ("Информационно-справочная система онлайн школы") built as a coursework project. The system uses a microservices architecture with separate backend (REST API) and frontend (web UI) services, backed by PostgreSQL database, all containerized with Docker.

## Architecture

### Three-Tier Architecture
- **Frontend** (`frontend/`): Spring Boot MVC application with Thymeleaf templates serving web UI on port 8081
- **Backend** (`backend/`): Spring Boot REST API with Spring Data JPA for database operations on port 8080
- **Database**: PostgreSQL 15 running in Docker container

### Communication Flow
Frontend → Backend REST API (via RestTemplate) → PostgreSQL Database

The frontend makes HTTP requests to backend API endpoints (e.g., `/api/auth/register`, `/api/auth/login`, `/api/courses`) to retrieve and manipulate data.

### Key Components

**Backend** (`com.education.backend`):
- `AuthController`: Handles user registration and authentication via REST endpoints
- `CourseController`: CRUD operations for courses with search/filtering
- `UserService`: Business logic for user management with BCrypt password hashing
- `SecurityConfig`: Spring Security configuration with CSRF disabled for REST API, HTTP Basic auth
- JPA Entities: `User`, `Course` with Lombok annotations
- Repositories: `UserRepository`, `CourseRepository` using Spring Data JPA

**Frontend** (`com.education.frontend`):
- `WebController`: Serves Thymeleaf templates and proxies requests to backend
- Uses HttpSession for user state management (username, email, role)
- Thymeleaf templates in `src/main/resources/templates/`: index.html (courses list), login.html, register.html, about.html

### Database Schema
- `users` table: id, username (unique), password (BCrypt hashed), email (unique), role (USER/ADMIN)
- `courses` table: id, title, description, category, author_id

### Authentication & Authorization
- Registration creates new users with default USER role
- Passwords are hashed using BCrypt (Spring Security)
- Login authenticates via backend API and stores user info in frontend session
- No JWT tokens - session-based authentication on frontend
- ADMIN role must be manually assigned via direct database update

## Development Commands

### Building & Running

Start all services with Docker Compose:
```bash
docker-compose up -d --build
```

Stop all services:
```bash
docker-compose down
```

View logs:
```bash
docker-compose logs -f [backend|frontend|postgres]
```

### Building Individual Modules

Build backend only:
```bash
cd backend
mvn clean package
```

Build frontend only:
```bash
cd frontend
mvn clean package
```

Build entire project (from root):
```bash
mvn clean package
```

### Running Services Locally (without Docker)

Prerequisites: PostgreSQL running on localhost:5432

Backend:
```bash
cd backend
mvn spring-boot:run
```

Frontend:
```bash
cd frontend
mvn spring-boot:run
```

### Testing

Run authentication tests:
```bash
./test_auth.sh
```

Manual API testing examples in `AUTH_TESTING.md`

### Database Operations

Connect to database:
```bash
docker exec -it education_db psql -U postgres -d education_platform
```

View all users:
```bash
docker exec education_db psql -U postgres -d education_platform -c "SELECT id, username, email, role FROM users;"
```

Promote user to ADMIN:
```bash
docker exec education_db psql -U postgres -d education_platform -c "UPDATE users SET role = 'ADMIN' WHERE username = 'admin';"
```

Delete user:
```bash
docker exec education_db psql -U postgres -d education_platform -c "DELETE FROM users WHERE username = 'testuser';"
```

### API Testing

Register user:
```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username":"newuser","password":"password123","email":"newuser@example.com"}'
```

Login:
```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"newuser","password":"password123"}'
```

Get courses:
```bash
curl http://localhost:8080/api/courses
```

Search courses:
```bash
curl "http://localhost:8080/api/courses?search=Java"
```

## Configuration

### Backend Configuration
- Database connection: `backend/src/main/resources/application.properties`
  - Default local: `jdbc:postgresql://localhost:5432/education_db`
  - Docker override via environment variables in `docker-compose.yaml`
- JPA: Auto DDL update enabled, SQL logging enabled
- Server port: 8080

### Frontend Configuration
- Backend API URL: `frontend/src/main/resources/application.properties`
  - Default local: `http://localhost:8080/api`
  - Docker override: `http://backend:8080/api`
- Server port: 8081
- Thymeleaf: Cache disabled for development

### Docker Environment Variables
Set in `docker-compose.yaml`:
- `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD`
- `BACKEND_API_URL` for frontend service

## Access Points

- Web UI: http://localhost:8081
- Backend API: http://localhost:8080/api
- Database: localhost:5432 (port not exposed by default in docker-compose)

## Important Notes

### Security Considerations
- CSRF protection is disabled for REST API endpoints
- CORS is enabled with `origins = "*"` in AuthController
- All `/api/auth/**` and `/api/courses/**` endpoints are publicly accessible
- Spring Security's HTTP Basic authentication is configured but not enforced on public endpoints

### Password Hashing
- All passwords are hashed with BCrypt before storage
- BCrypt generates unique salts per hash, so identical passwords have different hashes
- Test users must be created via API registration, not direct SQL inserts with plain text passwords

### Session Management
- Frontend maintains HttpSession with user data (username, email, role)
- No backend session tracking or JWT tokens
- Session invalidated on logout

### Multi-module Maven Structure
- Parent POM at root coordinates both modules
- Both backend and frontend have independent Spring Boot applications
- Java 17 required for all modules
- Eclipse project files (.classpath, .project, .settings) committed to repository

### Database Initialization
- PostgreSQL container runs `init.sql` on first startup
- Creates tables and inserts sample course data
- Sample courses reference non-existent author_ids (1, 2, 3)
