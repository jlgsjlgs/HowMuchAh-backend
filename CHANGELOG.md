# Changelog

All notable changes to this project will be documented in this file.

## [unreleased]

### Added
- Added features for group creation and member invitation

### Removed
- Redundant `AuthController` endpoint

## [0.1.0]

### Added
- CORS & Security Configs
- `AuthController` for syncing user information into database
- `UserController`, `User` entity and `UserRepository` for handling all user operations
- `JwtUtil` function for extracting JWT information