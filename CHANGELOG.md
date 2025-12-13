# Changelog

All notable changes to this project will be documented in this file.

## [0.2.0]

### Added
- Group creation and member invitation features
- Unit tests for group creation & invitation features

### Changes
- Updated `application.properties` to follow best practices

### Removed
- Redundant `AuthController` endpoint

## [0.1.0]

### Added
- CORS & Security Configs
- `AuthController` for syncing user information into database
- `UserController`, `User` entity and `UserRepository` for handling all user operations
- `JwtUtil` function for extracting JWT information