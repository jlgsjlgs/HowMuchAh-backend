# Changelog

All notable changes to this project will be documented in this file.

## [1.1.0] - 18-01-2026

### Added
- Invitation link feature, allowing for automatic whitelisting and group invitation

### Changes
- Updated database schema backup

## [1.0.0] - 26-12-2025

### Added
- Dockerfile for deployment on Render
- Database schema file

### Changes
- Updated README

## [0.7.0] - 26-12-2025

### Added
- WebSocketService via Spring WebSocket + STOMP 

### Changes
- Group invitations now send a message via WebSocket to the respective client

## [0.6.0] - 25-12-2025

### Added
- Settlement feature (Implemented using Greedy variation of debt settlement algorithm)
- Unit tests for settlement feature

### Changes
- Removed unused user-related endpoint

## [0.5.0] - 24-12-2025

### Changes
- Deprecated expense update feature
- Removed expense update unit tests

## [0.4.1] - 20-12-2025

### Changes
- Upgraded from Spring Boot 3.5.6 to 3.5.9
- Added `@Builder.Default` annotation to Entity classes that contained default values for some fields

## [0.4.0] - 15-12-2025

### Added
- Expense tracking features (CRUD)
- Unit tests for expense tracking (CRUD) features
- Settlement entity and repository classes

### Changes
- Updated `GlobalExceptionHandler` to catch database errors

## [0.3.0] - 14-12-2025

### Added
- Rate limiting

### Changes
- Updated unit tests for controllers to ignore rate limiting

## [0.2.0] - 13-12-2025

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

## [0.1.0] - 17-11-2025

### Added
- CORS & Security Configs
- `AuthController` for syncing user information into database
- `UserController`, `User` entity and `UserRepository` for handling all user operations
- `JwtUtil` function for extracting JWT information

## [0.0.0] - 22-10-2025

### Added
- Initial commit files