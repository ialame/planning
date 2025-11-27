# OAuth2/OIDC Integration with Authentik

This folder contains the updated frontend files for integrating OAuth2/OIDC authentication with Authentik.

## Architecture Overview

```
┌─────────────────┐       ┌─────────────────┐       ┌─────────────────┐
│   Vue.js App    │──────▶│    Authentik    │       │  Spring Boot    │
│   (Frontend)    │◀──────│   (OAuth2 IdP)  │       │   (Backend)     │
└────────┬────────┘       └─────────────────┘       └────────┬────────┘
         │                                                    │
         │           Bearer Token (JWT)                       │
         └───────────────────────────────────────────────────▶│
                                                              │
                    ┌─────────────────┐                       │
                    │  Symfony API    │◀──────────────────────┘
                    │  (API Key Auth) │      (Server-to-Server)
                    └─────────────────┘
```

## Files Included

### Configuration
- `src/config/oidc.config.ts` - OIDC settings for Authentik

### Services
- `src/services/authService.ts` - OAuth2 authentication service
- `src/services/api.ts` - HTTP client with token handling

### Composables
- `src/composables/useAuth.ts` - Reactive Vue composable for auth

### Router
- `src/router/index.ts` - Routes with authentication guards

### Views
- `src/views/auth/OAuthCallback.vue` - OAuth redirect handler
- `src/views/auth/SilentRenew.vue` - Token refresh handler
- `src/views/auth/Unauthorized.vue` - Access denied page
- `src/App.vue` - Updated main app component

### Entry Point
- `src/main.ts` - Application bootstrap

### Environment
- `.env.example` - Environment variables template

## Installation Steps

### 1. Install Dependencies

```bash
cd src/main/frontend
npm install oidc-client-ts
```

### 2. Copy Files

Copy the files from this folder to your project, replacing existing files:

```bash
# From project root
cp -r frontend-oauth2/src/* src/main/frontend/src/
cp frontend-oauth2/.env.example src/main/frontend/
```

### 3. Configure Environment

Create `.env` file from the example:

```bash
cd src/main/frontend
cp .env.example .env
```

Update the values:

```env
# API Base URL
VITE_API_BASE_URL=http://localhost:8080

# Application URL
VITE_APP_URL=http://localhost:5173

# Authentik Configuration
VITE_AUTHENTIK_AUTHORITY=https://auth.pcagrade.com/application/o/pokemon-planning/
VITE_AUTHENTIK_CLIENT_ID=your-client-id-from-authentik
```

### 4. Update TypeScript Config (if needed)

Ensure `tsconfig.json` includes:

```json
{
  "compilerOptions": {
    "paths": {
      "@/*": ["./src/*"]
    }
  }
}
```

### 5. Remove Old Auth Files

Delete these files (no longer needed):

- `src/components/LoginModal.vue`
- Any JWT-related auth code

## Authentik Setup

### Create Application in Authentik

1. Go to **Admin Interface** → **Applications** → **Create**
2. Fill in:
   - **Name**: `Pokemon Card Planning`
   - **Slug**: `pokemon-planning`
   - **Provider**: Create new OAuth2/OpenID Provider

### Create OAuth2 Provider

1. **Name**: `pokemon-planning-provider`
2. **Authorization flow**: `default-provider-authorization-implicit-consent`
3. **Client type**: `Public` (for SPA)
4. **Client ID**: Auto-generated (copy this!)
5. **Redirect URIs**:
   ```
   http://localhost:5173/callback
   https://planning.pcagrade.com/callback
   ```
6. **Scopes**: `openid profile email offline_access`

### Create Groups for Roles

Create groups in Authentik that map to application roles:

| Authentik Group | Application Role |
|-----------------|------------------|
| `noteurs`       | `ROLE_NOTEUR`    |
| `certificateurs`| `ROLE_CERTIFICATEUR` |
| `scanneurs`     | `ROLE_SCANNEUR`  |
| `admins`        | `ROLE_ADMIN`     |
| `managers`      | `ROLE_MANAGER`   |

## Usage Examples

### Check Authentication

```vue
<script setup>
import { useAuth } from '@/composables/useAuth'

const { isAuthenticated, user, hasRole } = useAuth()
</script>

<template>
  <div v-if="isAuthenticated">
    Welcome, {{ user.name }}!
  </div>
</template>
```

### Protected API Calls

```typescript
import api from '@/services/api'

// Token is automatically included
const orders = await api.get('/api/orders')
const newOrder = await api.post('/api/orders', { ... })
```

### Role-Based UI

```vue
<template>
  <button v-if="hasRole('ROLE_ADMIN')">
    Admin Action
  </button>
</template>
```

### Manual Login/Logout

```vue
<script setup>
import { useAuth } from '@/composables/useAuth'

const { login, logout } = useAuth()

// Redirect to Authentik login
await login()

// Redirect to Authentik logout
await logout()
</script>
```

## Testing

### Local Development

1. Start Authentik (if running locally via Docker)
2. Start Spring Boot backend
3. Start Vue.js frontend
4. Access `http://localhost:5173`
5. You'll be redirected to Authentik login
6. After login, you'll be redirected back to the app

### Common Issues

**CORS Errors**
- Ensure redirect URIs are correctly configured in Authentik
- Check that `VITE_APP_URL` matches the actual frontend URL

**Token Expired**
- Silent renew should handle this automatically
- If issues persist, check the `silent_redirect_uri` in Authentik

**Role Mapping**
- Verify groups are correctly assigned in Authentik
- Check the `claimMappings` in `oidc.config.ts`

## Security Notes

- Never commit `.env` files with real credentials
- Use HTTPS in production
- Configure proper CORS in Spring Boot
- Use short token lifetimes with automatic refresh
