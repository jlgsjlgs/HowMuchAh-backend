# HowMuchAh - Backend

A Spring Boot application for group expense tracking and settlement, with real-time updates via WebSockets.

<details>
  <summary>Table of Contents</summary>
  <ol>
    <li><a href="#about">About</a></li>
    <li><a href="#built-with">Built With</a></li>
    <li><a href="#getting-started">Getting Started</a></li>
    <li><a href="#deployment">Deployment</a></li>
  </ol>
</details>

## About

HowMuchAh is a Splitwise-inspired expense splitting application. The backend handles user authentication via Supabase, group management, expense tracking, and automated settlement calculations.

**Key Features:**
- JWT-based authentication with Supabase integration
- Group creation and member management
- Expense splitting with custom amounts or equal splits
- Automated settlement calculations using debt settlement greedy algorithm
- Real-time invitation notifications via WebSockets
- Rate limiting

## Built With

- **Java 25** (Amazon Corretto)
- **Spring Boot 3.5.9**
- **Spring Security** with OAuth2 Resource Server
- **Spring Data JPA**
- **PostgreSQL** (Supabase)
- **Spring WebSocket** for real-time updates
- **Bucket4j** for rate limiting
- **Lombok** for boilerplate reduction

## Getting Started

### Prerequisites

- Java 25 or higher
- Maven 3.x
- PostgreSQL database (or Supabase account)

### Installation

1. Clone the repository
   ```bash
   git clone git@github.com:jlgsjlgs/HowMuchAh-backend.git
   cd howmuchah-backend
   ```

2. Configure environment variables (.env or equivalent)

Variable | Purpose                     | How to obtain
:-- |:----------------------------| :--
DATABASE_URL | Connecting to your database | PostgreSQL connection string 
DATABASE_PASSWORD | Password for your database | PostgreSQL database password
SUPABASE_JWT_SECRET | For signing JWT | Supabase Auth
SUPABASE_URL | Pointing to your Supabase project | Supabase project dashboard
CORS_ALLOWED_ORIGINS | Allow your frontend to access server resources | Your frontend domain

3. Install dependencies and run

## Deployment

The application is deployed on **Railway** and configured to use PostgreSQL from Supabase.

**Railway deployment steps:**
1. Connect your GitHub repository to Railway
2. Set environment variables in Railway dashboard
3. Railway auto-deploys on push to main branch
4. Access via Railway-provided domain