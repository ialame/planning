# Authentik OAuth2/OIDC Integration

Complete integration guide for Pokemon Card Planning System with Authentik.

## Architecture Overview

```
┌─────────────────────────────────────────────────────────────────────┐
│                         User Browser                                │
│  ┌─────────────┐                              ┌─────────────────┐  │
│  │  Vue.js     │──── OAuth2 Redirect ────────▶│   Authentik     │  │
│  │  Frontend   │◀─── JWT Access Token ────────│   (PostgreSQL)  │  │
│  └──────┬──────┘                              └────────┬────────┘  │
│         │                                              │           │
│         │ Bearer Token                                 │           │
│         ▼                                              │           │
│  ┌─────────────┐                                       │           │
│  │ Spring Boot │◀──── Validate JWT ───────────────────┘           │
│  │  Backend    │                                                   │
│  │  (MariaDB)  │                                                   │
│  └─────────────┘                                                   │
└─────────────────────────────────────────────────────────────────────┘
```

## Prerequisites

- Authentik server installed (with PostgreSQL + Redis)
- Spring Boot backend configured
- Vue.js frontend setup

---

## Part 1: Authentik Server Setup

### 1.1 Docker Compose for Authentik

Create `authentik/docker-compose.yml`:

```yaml
version: "3.8"

services:
  postgresql:
    image: postgres:16-alpine
    restart: unless-stopped
    volumes:
      - ./data/postgres:/var/lib/postgresql/data
    environment:
      POSTGRES_PASSWORD: ${PG_PASS}
      POSTGRES_USER: ${PG_USER:-authentik}
      POSTGRES_DB: ${PG_DB:-authentik}
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -d $${POSTGRES_DB} -U $${POSTGRES_USER}"]
      interval: 30s
      timeout: 5s
      retries: 5

  redis:
    image: redis:alpine
    restart: unless-stopped
    volumes:
      - ./data/redis:/data
    healthcheck:
      test: ["CMD-SHELL", "redis-cli ping | grep PONG"]
      interval: 30s
      timeout: 3s
      retries: 5

  server:
    image: ghcr.io/goauthentik/server:2024.10
    restart: unless-stopped
    command: server
    environment:
      AUTHENTIK_SECRET_KEY: ${AUTHENTIK_SECRET_KEY}
      AUTHENTIK_REDIS__HOST: redis
      AUTHENTIK_POSTGRESQL__HOST: postgresql
      AUTHENTIK_POSTGRESQL__USER: ${PG_USER:-authentik}
      AUTHENTIK_POSTGRESQL__NAME: ${PG_DB:-authentik}
      AUTHENTIK_POSTGRESQL__PASSWORD: ${PG_PASS}
    volumes:
      - ./media:/media
      - ./templates:/templates
    ports:
      - "9000:9000"
      - "9443:9443"
    depends_on:
      - postgresql
      - redis

  worker:
    image: ghcr.io/goauthentik/server:2024.10
    restart: unless-stopped
    command: worker
    environment:
      AUTHENTIK_SECRET_KEY: ${AUTHENTIK_SECRET_KEY}
      AUTHENTIK_REDIS__HOST: redis
      AUTHENTIK_POSTGRESQL__HOST: postgresql
      AUTHENTIK_POSTGRESQL__USER: ${PG_USER:-authentik}
      AUTHENTIK_POSTGRESQL__NAME: ${PG_DB:-authentik}
      AUTHENTIK_POSTGRESQL__PASSWORD: ${PG_PASS}
    volumes:
      - ./media:/media
      - ./templates:/templates
    depends_on:
      - postgresql
      - redis

volumes:
  database:
  redis:
```

### 1.2 Environment Variables

Create `authentik/.env`:

```bash
# Generate with: openssl rand -base64 50 | head -c 50
AUTHENTIK_SECRET_KEY=your-50-character-secret-key-here

# PostgreSQL
PG_PASS=your-strong-database-password
PG_USER=authentik
PG_DB=authentik

# Optional: Email
AUTHENTIK_EMAIL__HOST=smtp.example.com
AUTHENTIK_EMAIL__PORT=587
AUTHENTIK_EMAIL__USERNAME=authentik@pcagrade.com
AUTHENTIK_EMAIL__PASSWORD=email-password
AUTHENTIK_EMAIL__USE_TLS=true
AUTHENTIK_EMAIL__FROM=authentik@pcagrade.com
```

### 1.3 Start Authentik

```bash
cd authentik
docker-compose up -d
```

### 1.4 Initial Setup

1. Navigate to: `https://your-server:9443/if/flow/initial-setup/`
2. Create admin account (`akadmin`)
3. Configure your domain

---

## Part 2: Authentik Application Configuration

### 2.1 Create Application

1. Go to **Applications** → **Create**
2. Fill in:
   - **Name**: `Pokemon Planning`
   - **Slug**: `pokemon-planning`
   - **Provider**: Create new (see below)

### 2.2 Create OAuth2/OIDC Provider

1. **Name**: `pokemon-planning-provider`
2. **Authorization flow**: `default-provider-authorization-implicit-consent`
3. **Client type**: `Confidential`
4. **Redirect URIs**:
   ```
   http://localhost:5173/callback
   https://planning.pcagrade.com/callback
   ```
5. **Scopes**: Select `openid`, `profile`, `email`

### 2.3 Note Your Credentials

After creation, note:
- **Client ID**: `abc123...`
- **Client Secret**: `xyz789...`
- **OpenID Configuration URL**: `https://auth.pcagrade.com/application/o/pokemon-planning/.well-known/openid-configuration`

### 2.4 Create Groups for Roles

Create groups in **Directory** → **Groups**:
- `ADMIN`
- `MANAGER`
- `NOTEUR`
- `CERTIFICATEUR`
- `SCANNEUR`
- `PREPARATEUR`

Assign users to appropriate groups.

---

## Part 3: Spring Boot Backend Integration

### 3.1 Add Dependencies

`pom.xml`:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-oauth2-resource-server</artifactId>
</dependency>
```

### 3.2 Application Properties

`application.properties`:

```properties
# Authentik OAuth2 Resource Server
spring.security.oauth2.resourceserver.jwt.issuer-uri=${AUTHENTIK_ISSUER_URI:https://auth.pcagrade.com/application/o/pokemon-planning/}
spring.security.oauth2.resourceserver.jwt.jwk-set-uri=${AUTHENTIK_ISSUER_URI:https://auth.pcagrade.com/application/o/pokemon-planning/}jwks/
```

### 3.3 Security Configuration

`SecurityConfig.java`:

```java
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final ApiKeyAuthenticationFilter apiKeyAuthFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .authorizeHttpRequests(auth -> auth
                // Public endpoints
                .requestMatchers("/api/public/**").permitAll()
                .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                
                // Sync endpoints - API Key auth (Symfony)
                .requestMatchers("/api/sync/**").permitAll()
                
                // Role-based access
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                .requestMatchers("/api/employees/**").hasAnyRole("ADMIN", "MANAGER")
                .requestMatchers("/api/planning/generate").hasAnyRole("ADMIN", "MANAGER")
                
                // All other API endpoints require authentication
                .requestMatchers("/api/**").authenticated()
                
                .anyRequest().permitAll()
            )
            // OAuth2 Resource Server - validates Authentik JWTs
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt
                    .jwtAuthenticationConverter(jwtAuthenticationConverter())
                )
            )
            // Keep API Key filter for Symfony sync
            .addFilterBefore(apiKeyAuthFilter, UsernamePasswordAuthenticationFilter.class);
        
        return http.build();
    }

    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter grantedAuthoritiesConverter = 
            new JwtGrantedAuthoritiesConverter();
        // Authentik puts groups in 'groups' claim
        grantedAuthoritiesConverter.setAuthoritiesClaimName("groups");
        grantedAuthoritiesConverter.setAuthorityPrefix("ROLE_");

        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(grantedAuthoritiesConverter);
        return converter;
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of(
            "http://localhost:5173",
            "http://localhost:3000",
            "https://planning.pcagrade.com"
        ));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
```

### 3.4 Get Current User in Controllers

```java
@RestController
@RequestMapping("/api/profile")
public class ProfileController {

    @GetMapping("/me")
    public Map<String, Object> getCurrentUser(@AuthenticationPrincipal Jwt jwt) {
        return Map.of(
            "sub", jwt.getSubject(),
            "email", jwt.getClaimAsString("email"),
            "name", jwt.getClaimAsString("name"),
            "groups", jwt.getClaimAsStringList("groups")
        );
    }
}
```

---

## Part 4: Vue.js Frontend Integration

### 4.1 Install Dependency

```bash
npm install oidc-client-ts
```

### 4.2 Files to Add/Update

Copy these files from this package:

```
src/
├── config/
│   └── oidc.config.ts          # OIDC configuration
├── services/
│   └── oidcAuthService.ts      # Auth service
├── composables/
│   └── useOidcAuth.ts          # Vue composable
├── views/
│   └── Callback.vue            # OAuth callback handler
├── router/
│   └── index.ts                # Updated router with guards
├── App.vue                     # Updated with auth UI
└── main.ts                     # Entry point
public/
└── silent-renew.html           # Silent token renewal
.env.example                    # Environment template
```

### 4.3 Environment Configuration

Create `.env.local`:

```env
VITE_API_BASE_URL=http://localhost:8080
VITE_AUTHENTIK_AUTHORITY=https://auth.pcagrade.com/application/o/pokemon-planning/
VITE_AUTHENTIK_CLIENT_ID=your-client-id
VITE_APP_URL=http://localhost:5173
```

### 4.4 Usage in Components

```vue
<script setup>
import { useOidcAuth } from '@/composables/useOidcAuth'

const { 
  user, 
  isAuthenticated, 
  isLoading,
  hasRole,
  authService 
} = useOidcAuth()

// Make authenticated API calls
async function loadData() {
  const data = await authService.get('/api/orders')
}

// Check roles
const canManage = computed(() => hasRole('MANAGER'))
</script>
```

---

## Part 5: Environment Variables Summary

### Authentik Server

| Variable | Description |
|----------|-------------|
| `AUTHENTIK_SECRET_KEY` | 50-char secret key |
| `PG_PASS` | PostgreSQL password |
| `PG_USER` | PostgreSQL username |
| `PG_DB` | Database name |

### Spring Boot Backend

| Variable | Description |
|----------|-------------|
| `AUTHENTIK_ISSUER_URI` | `https://auth.pcagrade.com/application/o/pokemon-planning/` |

### Vue.js Frontend

| Variable | Description |
|----------|-------------|
| `VITE_API_BASE_URL` | Backend URL |
| `VITE_AUTHENTIK_AUTHORITY` | Authentik issuer URL |
| `VITE_AUTHENTIK_CLIENT_ID` | OAuth2 Client ID |
| `VITE_APP_URL` | Frontend URL |

---

## Testing

### 1. Test Authentik

```bash
# Check OpenID configuration
curl https://auth.pcagrade.com/application/o/pokemon-planning/.well-known/openid-configuration
```

### 2. Test Backend JWT Validation

```bash
# Get token from Authentik (via frontend login)
# Then test API
curl -H "Authorization: Bearer <token>" http://localhost:8080/api/profile/me
```

### 3. Test Frontend Flow

1. Start frontend: `npm run dev`
2. Navigate to `http://localhost:5173`
3. Should redirect to Authentik login
4. After login, redirect back to app with user info

---

## Troubleshooting

### CORS Errors
- Ensure redirect URIs match exactly in Authentik
- Check CORS configuration in Spring Boot

### Token Validation Fails
- Verify `issuer-uri` matches Authentik configuration
- Check clock synchronization between servers

### Groups Not Mapped
- Ensure groups are included in scope
- Check Authentik provider settings for group claims

---

## Migration from JWT Auth

To migrate gradually:

1. Keep existing `/api/auth/**` endpoints temporarily
2. Add OAuth2 resource server configuration
3. Update frontend to use OIDC
4. Remove old JWT endpoints after testing
