# 🚀 API Synchronization Setup Guide
## Symfony ↔ Spring Boot Integration

Complete guide for setting up data synchronization between your Symfony and Spring Boot applications using REST API.

---

## 📐 Architecture Overview

```
┌──────────────────────────┐         ┌───────────────────────────┐
│  Symfony Application     │         │  Spring Boot Application  │
│  (Backend/CRM)           │         │  (Planning System)        │
│                          │         │                           │
│  Port: 8000              │  HTTP   │  Port: 8080               │
│  └─ DataExportAPI        │ ◄─────► │  └─ DataSyncController    │
│     └─ /api/export/*     │  REST   │     └─ /api/sync/*        │
│                          │         │                           │
│  Database: dev           │         │  Database: dev-planning   │
│  ├─ order                │         │  ├─ order (synced)        │
│  ├─ card                 │         │  ├─ card (synced)         │
│  └─ card_*               │         │  ├─ j_employee            │
│                          │         │  └─ j_planning            │
└──────────────────────────┘         └───────────────────────────┘
```

---

## 🎯 Part 1: Symfony API Setup

### Step 1: Install the Controller

Copy `DataExportController.php` to your Symfony project:

```bash
cp DataExportController.php your-symfony-project/src/Controller/Api/
```

### Step 2: Configure Routes (Optional)

If using annotations, routes are auto-configured. Otherwise add to `config/routes.yaml`:

```yaml
api_export:
    resource: '../src/Controller/Api/DataExportController.php'
    type: annotation
    prefix: /api/export
```

### Step 3: Test Symfony API

```bash
# Start Symfony server
symfony server:start

# Or with PHP built-in server
php -S localhost:8000 -t public/

# Test health endpoint
curl http://localhost:8000/api/export/health

# Test orders endpoint
curl http://localhost:8000/api/export/orders?limit=10
```

Expected response:
```json
{
  "success": true,
  "data": [
    {
      "id": "0191234567890abc",
      "num_commande": "CMD-001",
      ...
    }
  ],
  "pagination": {
    "total": 1500,
    "limit": 10,
    "offset": 0,
    "hasMore": true
  }
}
```

### Step 4: Configure CORS (if needed)

If Symfony and Spring Boot are on different ports/domains:

```bash
composer require nelmio/cors-bundle
```

In `config/packages/nelmio_cors.yaml`:

```yaml
nelmio_cors:
    defaults:
        origin_regex: true
        allow_origin: ['http://localhost:8080']
        allow_methods: ['GET', 'OPTIONS', 'POST', 'PUT', 'PATCH', 'DELETE']
        allow_headers: ['Content-Type', 'Authorization']
        max_age: 3600
    paths:
        '^/api/': ~
```

---

## 🎯 Part 2: Spring Boot Setup

### Step 1: Add API Client Service

Copy `SymfonyApiClient.java` to your Spring Boot project:

```bash
cp SymfonyApiClient.java src/main/java/com/pcagrade/order/service/
```

### Step 2: Add Updated Sync Controller

Copy `DataSyncController_API_Version.java` and rename it:

```bash
cp DataSyncController_API_Version.java src/main/java/com/pcagrade/order/controller/DataSyncController.java
```

### Step 3: Configure Symfony API URL

Add to `application-local.properties`:

```properties
# Symfony API Configuration
symfony.api.base-url=http://localhost:8000
symfony.api.timeout=30
```

For Docker:
```properties
# application-docker.properties
symfony.api.base-url=http://symfony-app:8000
symfony.api.timeout=30
```

### Step 4: Restart Spring Boot

```bash
./mvnw spring-boot:run
```

### Step 5: Test Spring Boot API

```bash
# Check Symfony API connectivity
curl http://localhost:8080/api/sync/symfony-health

# Get Symfony statistics
curl http://localhost:8080/api/sync/symfony-stats

# Check sync status
curl http://localhost:8080/api/sync/status

# Sync all data
curl -X POST http://localhost:8080/api/sync/all
```

---

## 🔧 Configuration Files

### Symfony: .env.local

```env
# Database
DATABASE_URL="mysql://ia:foufafou@127.0.0.1:3306/dev?serverVersion=11.2.2-MariaDB"

# CORS (if needed)
CORS_ALLOW_ORIGIN='^http://localhost:8080$'
```

### Spring Boot: application-local.properties

```properties
# Main database (dev-planning)
spring.datasource.url=jdbc:mariadb://localhost:3306/dev-planning?useSSL=false
spring.datasource.username=ia
spring.datasource.password=foufafou

# Symfony API
symfony.api.base-url=http://localhost:8000
symfony.api.timeout=30

# Liquibase
spring.liquibase.enabled=false
spring.jpa.hibernate.ddl-auto=validate
```

---

## 📡 API Endpoints Reference

### Symfony API (Port 8000)

| Endpoint | Method | Description | Parameters |
|----------|--------|-------------|------------|
| `/api/export/health` | GET | Health check | - |
| `/api/export/stats` | GET | Get statistics | - |
| `/api/export/orders` | GET | Get orders | `limit`, `offset`, `status`, `since` |
| `/api/export/cards` | GET | Get cards | `limit`, `offset` |
| `/api/export/card-translations` | GET | Get translations | `limit`, `offset` |
| `/api/export/card-certifications` | GET | Get certifications | `limit`, `offset` |
| `/api/export/card-certification-orders` | GET | Get cert orders | `limit`, `offset` |

### Spring Boot API (Port 8080)

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/api/sync/symfony-health` | GET | Check Symfony API |
| `/api/sync/symfony-stats` | GET | Get Symfony stats |
| `/api/sync/status` | GET | Compare databases |
| `/api/sync/all` | POST | Sync all tables |
| `/api/sync/orders` | POST | Sync orders only |
| `/api/sync/cards` | POST | Sync cards only |

---

## 🧪 Testing the Integration

### Test 1: Symfony API Accessibility

```bash
# From Spring Boot server
curl http://localhost:8000/api/export/health

# Expected:
# {"success":true,"status":"healthy","database":"connected"}
```

### Test 2: Spring Boot → Symfony Connection

```bash
curl http://localhost:8080/api/sync/symfony-health

# Expected:
# {"success":true,"status":"healthy","message":"✅ Symfony API is accessible"}
```

### Test 3: Get Statistics

```bash
curl http://localhost:8080/api/sync/symfony-stats

# Expected:
# {
#   "success": true,
#   "data": {
#     "orders": {"total": 1500, "active": 1200},
#     "cards": {"total": 50000}
#   }
# }
```

### Test 4: Check Sync Status

```bash
curl http://localhost:8080/api/sync/status

# Expected:
# {
#   "success": true,
#   "tableComparison": [
#     {
#       "table": "order",
#       "symfonyCount": 1500,
#       "devPlanningCount": 1500,
#       "inSync": true
#     }
#   ]
# }
```

### Test 5: Perform Sync

```bash
curl -X POST http://localhost:8080/api/sync/all

# Expected:
# {
#   "success": true,
#   "message": "✅ Full synchronization completed",
#   "syncedTables": [
#     "Orders: 0 deleted, 1500 inserted",
#     "Cards: 0 deleted, 50000 inserted",
#     ...
#   ]
# }
```

---

## 🔄 Usage Workflows

### Workflow 1: Initial Setup

```bash
# 1. Start Symfony
cd symfony-project
symfony server:start

# 2. Start Spring Boot
cd spring-boot-project
./mvnw spring-boot:run

# 3. Verify connectivity
curl http://localhost:8080/api/sync/symfony-health

# 4. Perform initial sync
curl -X POST http://localhost:8080/api/sync/all

# 5. Verify sync
curl http://localhost:8080/api/sync/status
```

### Workflow 2: Daily Sync

```bash
# Option A: Manual
curl -X POST http://localhost:8080/api/sync/all

# Option B: Cron job (add to crontab)
0 2 * * * curl -X POST http://localhost:8080/api/sync/all

# Option C: Spring Scheduler (see below)
```

### Workflow 3: Development Cycle

```bash
# 1. Modify data in Symfony
# 2. Sync to planning system
curl -X POST http://localhost:8080/api/sync/orders

# 3. Generate planning
curl -X POST http://localhost:8080/api/planning/generate \
  -H "Content-Type: application/json" \
  -d '{"planningDate": "2025-06-01", "cleanFirst": true}'

# 4. View planning in frontend
open http://localhost:3000/planning
```

---

## ⚙️ Advanced Configuration

### Scheduled Automatic Sync

Add to your Spring Boot project:

```java
package com.pcagrade.order.scheduler;

import com.pcagrade.order.controller.DataSyncController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@EnableScheduling
public class DataSyncScheduler {

    private static final Logger log = LoggerFactory.getLogger(DataSyncScheduler.class);

    @Autowired
    private DataSyncController syncController;

    // Run every 6 hours
    @Scheduled(cron = "0 0 */6 * * *")
    public void scheduledSync() {
        log.info("🔄 Starting scheduled synchronization");
        try {
            syncController.syncAll();
            log.info("✅ Scheduled sync completed");
        } catch (Exception e) {
            log.error("❌ Scheduled sync failed", e);
        }
    }
}
```

### Batch Size Configuration

For large datasets, configure batch processing:

```properties
# application.properties
sync.batch.size=1000
sync.parallel.enabled=true
sync.thread.pool.size=4
```

### Retry Logic

Add retry configuration:

```properties
# application.properties
sync.retry.max-attempts=3
sync.retry.backoff.delay=2000
```

---

## 🐛 Troubleshooting

### Problem: "Cannot reach Symfony API"

**Causes:**
- Symfony server not running
- Wrong URL configuration
- Firewall blocking connection

**Solutions:**
```bash
# Check Symfony is running
ps aux | grep symfony

# Test Symfony directly
curl http://localhost:8000/api/export/health

# Check Spring Boot config
grep symfony application-local.properties

# Test from Spring Boot container (if Docker)
docker exec -it planning-backend curl http://symfony-app:8000/api/export/health
```

### Problem: "Connection timeout"

**Solutions:**
```properties
# Increase timeout
symfony.api.timeout=60
```

### Problem: "Binary ID conversion error"

**Cause:** IDs not properly converted from binary to hex

**Solution:** Verify Symfony controller uses `bin2hex()`:
```php
$order['id'] = bin2hex($order['id']);
```

### Problem: "Sync takes too long"

**Solutions:**
1. Increase batch size in Symfony:
   ```php
   $limit = $request->query->getInt('limit', 5000);
   ```

2. Enable parallel processing in Spring Boot

3. Sync only modified records:
   ```bash
   curl -X POST "http://localhost:8080/api/sync/incremental?since=2025-10-01"
   ```

### Problem: "Foreign key constraint fails"

**Solution:** Ensure sync order is correct:
1. Orders (parent)
2. Cards (parent)
3. Card translations
4. Card certifications
5. Card certification orders (child)

---

## 📊 Monitoring

### Log Sync Activity

Add to `application.properties`:
```properties
logging.level.com.pcagrade.order.controller.DataSyncController=DEBUG
logging.level.com.pcagrade.order.service.SymfonyApiClient=DEBUG
```

### Monitor Sync Status

Create a monitoring script:
```bash
#!/bin/bash
# sync-monitor.sh

STATUS=$(curl -s http://localhost:8080/api/sync/status)

if echo "$STATUS" | jq -e '.tableComparison[] | select(.inSync==false)' > /dev/null; then
    echo "⚠️ ALERT: Databases out of sync!"
    # Send notification (email, Slack, etc.)
else
    echo "✅ All tables in sync"
fi
```

Run periodically:
```bash
*/15 * * * * /path/to/sync-monitor.sh
```

---

## 🔒 Security Considerations

### 1. API Authentication (Recommended)

**Symfony:**
```php
// Add JWT or API key authentication
#[Route('/api/export/orders', name: 'api_export_orders')]
#[IsGranted('ROLE_API_USER')]
public function getOrders(Request $request): JsonResponse
```

**Spring Boot:**
```java
// Add API key header
HttpRequest request = HttpRequest.newBuilder()
    .uri(URI.create(url))
    .header("X-API-Key", apiKey)
    .GET()
    .build();
```

### 2. HTTPS in Production

```properties
# Use HTTPS
symfony.api.base-url=https://symfony-app.example.com
```

### 3. Rate Limiting

Add to Symfony to prevent abuse:
```php
use Symfony\Component\RateLimiter\RateLimiterFactory;

public function getOrders(Request $request, RateLimiterFactory $apiLimiter)
{
    $limiter = $apiLimiter->create($request->getClientIp());
    if (!$limiter->consume(1)->isAccepted()) {
        return new JsonResponse(['error' => 'Too many requests'], 429);
    }
    // ...
}
```

---

## ✅ Checklist

Before going to production:

- [ ] Symfony API endpoints tested and working
- [ ] Spring Boot can connect to Symfony API
- [ ] Sync works for all tables
- [ ] Sync status shows all tables in sync
- [ ] Scheduled sync configured (if needed)
- [ ] Monitoring and alerts set up
- [ ] Error handling tested
- [ ] Security measures implemented
- [ ] Documentation updated
- [ ] Backup strategy in place

---

## 🎉 Quick Start Summary

```bash
# 1. Copy files
cp DataExportController.php symfony-project/src/Controller/Api/
cp SymfonyApiClient.java spring-boot/src/main/java/.../service/
cp DataSyncController_API_Version.java spring-boot/src/main/java/.../controller/

# 2. Configure
echo "symfony.api.base-url=http://localhost:8000" >> application-local.properties

# 3. Start servers
symfony server:start         # Terminal 1
./mvnw spring-boot:run       # Terminal 2

# 4. Test
curl http://localhost:8080/api/sync/symfony-health
curl -X POST http://localhost:8080/api/sync/all
curl http://localhost:8080/api/sync/status

# 5. Done! 🚀
```

---

## 📞 Support

- Check logs: `tail -f logs/spring-boot-application.log`
- Test Symfony: `curl http://localhost:8000/api/export/health`
- Test Spring: `curl http://localhost:8080/api/sync/symfony-health`
- Verify config: `grep symfony application-local.properties`

**You're ready to sync!** 🎊