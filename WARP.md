# WARP.md

This file provides guidance to WARP (warp.dev) when working with code in this repository.

## Project Overview

This is an online education platform ("Информационно-справочная система онлайн школы") built as a coursework project by Строганов Т.А. (ИУ5-53Б). The system uses a microservices architecture with separate backend (REST API) and frontend (web UI) services, backed by PostgreSQL database, all containerized with Docker.

**Technologies**: Spring Boot 3.2.0, Thymeleaf, PostgreSQL 15, Docker, Lombok, Spring Security, Spring Data JPA

## Architecture

### Three-Tier Architecture
- **Frontend** (`frontend/`): Spring Boot MVC application with Thymeleaf templates serving web UI on port 8081
- **Backend** (`backend/`): Spring Boot REST API with Spring Data JPA for database operations on port 8080
- **Database**: PostgreSQL 15 running in Docker container

### Communication Flow
Frontend → Backend REST API (via RestTemplate) → PostgreSQL Database

The frontend makes HTTP requests to backend API endpoints to retrieve and manipulate data. Backend communicates with PostgreSQL database using Spring Data JPA.

### Key Components

**Backend** (`com.education.backend`):
- `AuthController`: Handles user registration and authentication via REST endpoints (`/api/auth/register`, `/api/auth/login`)
- `CourseController`: CRUD operations for courses with search/filtering (`/api/courses`)
- `UserController`: User management API for admins (`/api/admin/users/**`, `/admin/users/**`)
- `MainController`: Serves backend views and handles authentication flow (legacy/alternative endpoints)
- `UserService`: Business logic for user management with BCrypt password hashing
- `CustomUserDetailsService`: Spring Security integration for loading user details
- `SecurityConfig`: Spring Security configuration with CSRF disabled for REST API, HTTP Basic auth, role-based access control
- `CorsConfig`: Global CORS configuration allowing all origins
- JPA Entities: `User`, `Course` with Lombok annotations
- Repositories: `UserRepository`, `CourseRepository` using Spring Data JPA
- Helper classes: `AuthRequest`, `HashGenerator`, `GenerateAdminPassword.java` (root)

**Frontend** (`com.education.frontend`):
- `WebController`: Serves Thymeleaf templates and proxies all requests to backend REST API
- Uses HttpSession for user state management (username, email, role)
- Thymeleaf templates in `src/main/resources/templates/`:
  - `index.html`: Courses list with search functionality
  - `login.html`: User login form
  - `register.html`: User registration form
  - `about.html`: About author page
  - `create-course.html`: Course creation form (ADMIN only)
  - `user-management.html`: User list and management (ADMIN only)
  - `edit-user.html`: Edit user details (ADMIN only)

### Database Schema
- `users` table: id (BIGSERIAL), username (unique, varchar 255), password (varchar 255, BCrypt hashed), email (unique, varchar 255), role (varchar 50, default 'USER')
- `courses` table: id (BIGSERIAL), title (varchar 255), description (text), category (varchar 100), author_id (bigint)

### Authentication & Authorization
- Registration creates new users with default USER role via `/api/auth/register`
- Passwords are hashed using BCrypt (Spring Security PasswordEncoder)
- Login authenticates via backend API (`/api/auth/login`) and stores user info in frontend HttpSession
- Session-based authentication on frontend (no JWT tokens)
- ADMIN role can be assigned:
  - Via SQL: `UPDATE users SET role = 'ADMIN' WHERE username = 'admin';`
  - Via user management UI (by existing admin)
  - Default admin created by `init.sql`: username=`admin`, password=`admin123`
- Spring Security enforces role-based access:
  - `/api/auth/**` and `/api/courses/**` are public
  - `/api/admin/users/**` is public for API access (auth checked by backend logic)
  - `/admin/users/**` requires ADMIN role
  - `/courses/create` requires authentication + ADMIN role (checked in frontend)

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

Restart specific service:
```bash
docker-compose restart backend
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

Prerequisites: PostgreSQL running on localhost:5432 with database `education_db`

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

Run authentication tests (requires running Docker containers):
```bash
./test_auth.sh
```

The test script:
1. Registers a new user (testuser3)
2. Tests duplicate registration (should fail)
3. Authenticates with correct password
4. Authenticates with wrong password (should fail)
5. Tests admin login

### Database Operations

Connect to database:
```bash
docker exec -it education_db psql -U postgres -d education_platform
```

View all users:
```bash
docker exec education_db psql -U postgres -d education_platform -c "SELECT id, username, email, role FROM users;"
```

View all courses:
```bash
docker exec education_db psql -U postgres -d education_platform -c "SELECT id, title, category, author_id FROM courses;"
```

Promote user to ADMIN:
```bash
docker exec education_db psql -U postgres -d education_platform -c "UPDATE users SET role = 'ADMIN' WHERE username = 'username_here';"
```

Delete user:
```bash
docker exec education_db psql -U postgres -d education_platform -c "DELETE FROM users WHERE username = 'testuser';"
```

Delete course:
```bash
docker exec education_db psql -U postgres -d education_platform -c "DELETE FROM courses WHERE id = 1;"
```

### API Testing

**Authentication**

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

**Courses**

Get all courses:
```bash
curl http://localhost:8080/api/courses
```

Search courses:
```bash
curl "http://localhost:8080/api/courses?search=Java"
```

Create course:
```bash
curl -X POST http://localhost:8080/api/courses \
  -H "Content-Type: application/json" \
  -d '{"title":"New Course","description":"Course description","category":"Programming","authorId":null}'
```

Delete course:
```bash
curl -X DELETE http://localhost:8080/api/courses/1
```

**User Management (Admin)**

Get all users:
```bash
curl http://localhost:8080/api/admin/users
```

Search users:
```bash
curl "http://localhost:8080/api/admin/users?query=admin"
```

Get user by ID:
```bash
curl http://localhost:8080/api/admin/users/1
```

Update user:
```bash
curl -X PUT http://localhost:8080/api/admin/users/1 \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","email":"admin@example.com","role":"ADMIN"}'
```

Delete user:
```bash
curl -X DELETE http://localhost:8080/api/admin/users/2
```

## Configuration

### Backend Configuration
- File: `backend/src/main/resources/application.properties`
- Database connection:
  - Default local: `jdbc:postgresql://localhost:5432/education_db`
  - Docker override via environment variables in `docker-compose.yaml`
  - Username: `postgres`
  - Password: `postgres` (local) or `password` (Docker)
- JPA: Auto DDL update enabled (`spring.jpa.hibernate.ddl-auto=update`), SQL logging enabled
- Server port: 8080
- Hibernate dialect: PostgreSQL

### Frontend Configuration
- File: `frontend/src/main/resources/application.properties`
- Backend API URL:
  - Default local: `http://localhost:8080/api`
  - Docker: `http://backend:8080/api` (via environment variable)
  - Configured via `@Value("${BACKEND_API_URL:http://localhost:8080/api}")`
- Server port: 8081
- Thymeleaf:
  - Cache disabled for development
  - Template location: `classpath:/templates/`
  - Template suffix: `.html`

### Docker Configuration
File: `docker-compose.yaml`

**Services:**
1. `postgres` (container: `education_db`):
   - Image: postgres:15-alpine
   - Database: `education_platform`
   - User/Password: `postgres`/`password`
   - Volume: `db_data` for persistence
   - Init script: `./init.sql` (creates tables and default admin)
   - Internal network only (port 5432 not exposed)

2. `backend` (container: `education_backend`):
   - Build: `./backend/Dockerfile`
   - Port: 8080 (internal only)
   - Env vars: Database connection overrides
   - Depends on: postgres

3. `frontend` (container: `education_frontend`):
   - Build: `./frontend/Dockerfile`
   - Port: 8081 (exposed to host)
   - Env var: `BACKEND_API_URL=http://backend:8080/api`
   - Depends on: backend

**Network:** `education_net` (bridge)

## Access Points

- **Web UI**: http://localhost:8081 (main entry point for users)
- **Backend API**: http://localhost:8080/api (accessible from frontend container only)
- **Database**: postgres:5432 (accessible from backend container only)

**Default Admin Account**:
- Username: `admin`
- Password: `admin123`
- Email: `admin@example.com`
- Role: `ADMIN`

## Important Notes

### Security Considerations
- **CSRF**: Disabled globally for REST API endpoints (`csrf.disable()` in SecurityConfig)
- **CORS**: Enabled with `allowedOrigins("*")` in CorsConfig - allows all origins
- **Public endpoints**: `/api/auth/**`, `/api/courses/**`, `/api/admin/users/**` are accessible without authentication
- **Protected endpoints**: `/admin/users/**`, `/courses/create`, `/logout` require authentication
- **Role-based access**: ADMIN role required for `/admin/users/**` and course creation
- **HTTP Basic Auth**: Configured but not enforced on public API endpoints
- **Password security**: BCrypt hashing with auto-generated salts

### Password Hashing
- All passwords are hashed with BCrypt before storage using Spring Security's `PasswordEncoder`
- BCrypt generates unique salts per hash, so identical passwords produce different hashes
- **IMPORTANT**: Test users must be created via API registration (`/api/auth/register`), NOT via direct SQL inserts with plain text passwords
- Default admin password is pre-hashed in `init.sql`: `$2b$12$vshIp3Z4Tn6BS7MKcp84Qeap6uidZOrO8lZ/pRDwalhQFab3FO5Iy` = `admin123`
- Helper utility: `GenerateAdminPassword.java` in root directory can generate BCrypt hashes

### Session Management
- **Frontend**: Maintains HttpSession with user data (username, email, role)
- **Backend**: No session tracking, stateless REST API
- **No JWT**: Uses session-based authentication instead of token-based
- **Session storage**: User info stored in HttpSession attributes on frontend after successful login
- **Logout**: Invalidates frontend session via `session.invalidate()`

### Multi-module Maven Structure
- **Parent POM** at root (`pom.xml`) coordinates both modules with common properties
- **Independent modules**: Backend and frontend are separate Spring Boot applications with their own POMs
- **Java version**: Java 17 required for all modules
- **Build**: Can build modules individually or entire project from root
- **Lombok**: Used in backend for entity classes (`@Data` annotation)
- **Eclipse files**: `.classpath`, `.project`, `.settings` committed to repository

### Database Initialization
- PostgreSQL container runs `init.sql` on first startup (via `/docker-entrypoint-initdb.d/`)
- Creates `users` and `courses` tables if they don't exist
- Inserts default admin user with pre-hashed password
- Inserts 5 sample courses (Java, Spring Boot, Docker, PostgreSQL, Frontend)
- Sample courses reference author_ids (1, 2, 3) that may not exist - this is intentional
- `ON CONFLICT DO NOTHING` prevents duplicate entries on container restarts

### API Endpoints Structure
- **Dual endpoint pattern**: Some endpoints exist in both backend and frontend controllers
  - `/api/admin/users/**` - Backend REST API (public access)
  - `/admin/users/**` - Backend REST API or Frontend proxy (requires ADMIN role)
- **Frontend pattern**: WebController proxies all requests to backend via RestTemplate
- **Backend pattern**: Controllers return JSON for API endpoints, Views for direct access
- **Error handling**: Frontend catches `HttpClientErrorException` and displays user-friendly messages

### Development Workflow
1. Start Docker containers: `docker-compose up -d --build`
2. Access web UI at http://localhost:8081
3. Login with default admin (admin/admin123) or register new user
4. Admin can create courses and manage users via web UI
5. API endpoints available at http://localhost:8080/api (from within containers)
6. View logs: `docker-compose logs -f backend frontend`
7. Database accessible via `docker exec -it education_db psql`

### Troubleshooting
- **Backend connection error**: Check if backend container is running and healthy
- **Database connection error**: Verify PostgreSQL is running and credentials match
- **Authentication fails**: Ensure password was hashed via BCrypt (use API registration)
- **ADMIN role not working**: Check user role in database, update via SQL if needed
- **Port conflicts**: Ensure ports 8080 and 8081 are available on host (only 8081 is exposed)
- **Container won't start**: Check logs with `docker-compose logs backend` or `docker-compose logs postgres`
- **Init SQL not running**: Delete volume `docker volume rm kursovaya-stroganov_db_data` and restart

### Frontend-Backend Communication
- Frontend uses Spring `RestTemplate` for HTTP requests
- Backend URL configured via `BACKEND_API_URL` environment variable
- JSON serialization/deserialization handled by Jackson (default in Spring Boot)
- Error responses from backend are caught and displayed in frontend templates
- Session attributes used to pass messages between redirects (success/error messages)

