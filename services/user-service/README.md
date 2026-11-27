# User Service

Spring Boot user and authentication service in the Airline microservices project.

## Responsibilities

- Account signup and email verification
- Signin, JWT access tokens, and rotating refresh tokens
- User profile and password management
- OTP-based account and password recovery
- Admin-only user management

## Local setup

1. Start PostgreSQL and create the user_db database.
2. Copy .env.example to .env, then provide database credentials and a unique JWT_SECRET (at least 32 bytes). Set MAIL_FROM, MAIL_USERNAME, and MAIL_PASSWORD for SMTP delivery.
3. The service loads `./.env` automatically (see `spring.config.import` in application.yaml), so run it with the service folder as the working directory, or export the values in your shell or IDE instead.
4. From the repository root, build common-lib and start the service with ./services/user-service/mvnw -f pom.xml -pl services/user-service -am spring-boot:run.

The service listens on port 8081 by default. Only set ADMIN_EMAIL and ADMIN_PASSWORD when intentionally provisioning the first system administrator. The service does not create a default admin account.

## API outline

All routes are under /api/v1.

- POST /auth/signup
- POST /auth/verify-email
- POST /auth/verify-email/resend
- POST /auth/signin
- POST /auth/refresh
- POST /auth/logout
- POST /auth/forgot-password
- POST /auth/reset-password
- GET /users/me (Bearer access token required)
- PUT /users/me/password (Bearer access token required)
- GET /users (system admin)
- PUT /users/{id}/role (system admin)

Shared DTOs and common error handling belong in common-lib only when they are truly used by more than one service. Keep persistence entities, repositories, and service-specific request models inside this service.
