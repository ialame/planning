# Pokemon Card Planning System - Docker Setup

Quick guide to run the application locally with Docker.

## Prerequisites

- **Docker** & **Docker Compose** installed
- **Symfony API** running on `localhost:8000` (PCA Grade backend)
- Git access to the repository

## Quick Start

### 1. Clone the Repository

```bash
git clone git@github.com:your-org/planning.git
cd planning
```

### 2. Configure Environment

The repository includes a `docker-compose.yml` pre-configured for local development.

**Important:** Ensure the Symfony API is running with the GPT API key configured:

```bash
# In Symfony .env (should already be set)
GPT_API_KEY=planning-sync-key-2025
```

### 3. Start the Application

```bash
docker-compose up -d
```

First run will:
- Build the Spring Boot backend
- Build the Vue.js frontend
- Create and initialize the MariaDB database
- Run Liquibase migrations automatically

### 4. Access the Application

| Service | URL | Description |
|---------|-----|-------------|
| **Frontend** | http://localhost:8012 | Vue.js application |
| **Backend API** | http://localhost:8010 | Spring Boot REST API |
| **Database** | localhost:8011 | MariaDB (user: `pokemon_user` / pass: `pokemon_password`) |

### 5. Verify Everything Works

```bash
# Check all containers are running
docker-compose ps

# Test backend health
curl http://localhost:8010/actuator/health

# Test Symfony connection
curl http://localhost:8010/api/sync/health
```

## Synchronize Data

Once running, open http://localhost:8012 and go to **Data Sync** to import orders from Symfony.

Or via CLI:
```bash
curl -X POST http://localhost:8010/api/sync/orders
```

## Useful Commands

```bash
# Start
docker-compose up -d

# Stop
docker-compose down

# View logs
docker-compose logs -f backend
docker-compose logs -f frontend

# Rebuild after code changes
docker-compose build --no-cache
docker-compose up -d

# Rebuild only frontend
docker-compose build --no-cache frontend
docker-compose up -d frontend

# Rebuild only backend
docker-compose build --no-cache backend
docker-compose up -d backend

# Reset database (delete all data)
docker-compose down -v
docker-compose up -d
```

## Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                      Host Machine                           │
│                                                             │
│  ┌─────────────────┐                                        │
│  │   Symfony API   │ ◄── GPT_API_KEY=planning-sync-key-2025 │
│  │  localhost:8000 │                                        │
│  └────────▲────────┘                                        │
│           │ host.docker.internal:8000                       │
├───────────┼─────────────────────────────────────────────────┤
│           │           Docker Network                        │
│           │                                                 │
│  ┌────────┴────────┐    ┌──────────────┐   ┌────────────┐  │
│  │     Backend     │◄───│   Frontend   │   │  Database  │  │
│  │  localhost:8010 │    │ localhost:8012│   │ localhost: │  │
│  │   (port 8080)   │    │  (port 3000) │   │   8011     │  │
│  └────────┬────────┘    └──────────────┘   └──────┬─────┘  │
│           │                                       │         │
│           └───────────────────────────────────────┘         │
│                         db:3306                             │
└─────────────────────────────────────────────────────────────┘
```

## Configuration Files

| File | Location | Purpose |
|------|----------|---------|
| `docker-compose.yml` | Root | Docker services configuration |
| `application-docker.properties` | `src/main/resources/` | Spring Boot Docker profile |
| `Dockerfile` | Root | Backend build |
| `Dockerfile` | `src/main/frontend/` | Frontend build |

## Troubleshooting

### Frontend shows "Auth disabled" message
This is normal in Docker mode. Authentication is bypassed for local development.

### Symfony API connection failed
1. Ensure Symfony is running: `curl http://localhost:8000/v1/json/gpt/orders?limit=1 -H "Authorization: Bearer planning-sync-key-2025"`
2. Check the API key matches in both Symfony and docker-compose.yml

### Database connection issues
```bash
# Check database is healthy
docker-compose ps

# View database logs
docker-compose logs db

# Reset database
docker-compose down -v
docker-compose up -d
```

### Port already in use
```bash
# Find what's using the port
lsof -i :8010
lsof -i :8011
lsof -i :8012

# Kill the process or change ports in docker-compose.yml
```

## Development Without Docker

For frontend development with hot-reload:

```bash
cd src/main/frontend

# Create local env file
cat > .env.local << 'EOF'
VITE_API_BASE_URL=http://localhost:8010
VITE_AUTH_DISABLED=true
EOF

# Run dev server
npm install
npm run dev
```

Then access http://localhost:5173

---

**Note:** This Docker setup disables OAuth2 authentication for easier local development. For production deployment, use the Kubernetes configuration with proper Authentik integration.