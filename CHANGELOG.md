# Changelog

All notable changes to this project will be documented in this file.

## [0.2.0]

### Added
- Group features (Create, delete, modify)
- Group invitation features (Create, revoke, accept, decline)
- Group member features (View, remove, leave)
- Unit tests for group creation, invitation and member features
- `GlobalExceptionHandler` for graceful error handling

### Changes
- Updated `application.properties` to follow Spring Boot best practices
- Hardened CORS policy
- Updated `SecurityConfig` to require JWT for all endpoints (previously `/api/auth` did not require JWT)

### Removed
- Removed `/me` endpoint from `AuthController`

## [0.1.0]

### Added
- CORS & Security Configs
- `AuthController` for syncing user information into database
- `UserController`, `User` entity and `UserRepository` for handling all user operations
- `JwtUtil` function for extracting JWT information