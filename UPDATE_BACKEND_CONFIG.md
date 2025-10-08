# 🔧 Update Backend Configuration for "dev-planning"

After running the migration script, you need to update your backend configuration to point to the new database.

---

## Option 1: Local Development (application-local.properties)

**File:** `src/main/resources/application-local.properties`

```properties
# Database Configuration - NEW DATABASE
spring.datasource.url=jdbc:mariadb://localhost:3306/dev-planning?useSSL=false&allowPublicKeyRetrieval=true
spring.datasource.username=YOUR_USERNAME
spring.datasource.password=YOUR_PASSWORD
spring.datasource.driver-class-name=org.mariadb.jdbc.Driver

# Connection Pool
spring.datasource.hikari.maximum-pool-size=10
spring.datasource.hikari.minimum-idle=2
spring.datasource.hikari.connection-timeout=30000

# JPA/Hibernate Configuration
spring.jpa.hibernate.ddl-auto=validate
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.MariaDBDialect
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.format_sql=true

# Liquibase Configuration
spring.liquibase.enabled=false
spring.liquibase.change-log=classpath:db/changelog/db.changelog-master.yml
spring.liquibase.contexts=development
spring.liquibase.default-schema=dev-planning

# CORS Configuration
spring.web.cors.allowed-origins=http://localhost:3000,http://localhost:5173,http://127.0.0.1:3000
spring.web.cors.allowed-methods=GET,POST,PUT,DELETE,OPTIONS
spring.web.cors.allowed-headers=*

# Logging
logging.level.root=INFO
logging.level.com.pcagrade=DEBUG
logging.level.org.springframework.web=DEBUG
logging.level.org.hibernate.SQL=DEBUG

# Planning Configuration
planning.card.processing.time=3
planning.employee.break.time=15
planning.workday.start=08:00
planning.workday.end=17:00

# Order table compatibility
order.table.readonly=true
order.status.integer.mapping=true
order.adapter.service.enabled=true
```

---

## Option 2: Docker (application-docker.properties)

**File:** `src/main/resources/application-docker.properties`

```properties
# Application
spring.application.name=pokemon-card-planning
server.port=8080

# Database Configuration - NEW DATABASE
spring.datasource.url=jdbc:mariadb://database:3306/dev-planning?useSSL=false&allowPublicKeyRetrieval=true&createDatabaseIfNotExist=false
spring.datasource.username=ia
spring.datasource.password=foufafou
spring.datasource.driver-class-name=org.mariadb.jdbc.Driver

# Connection Pool
spring.datasource.hikari.maximum-pool-size=20
spring.datasource.hikari.minimum-idle=5
spring.datasource.hikari.connection-timeout=30000
spring.datasource.hikari.idle-timeout=600000
spring.datasource.hikari.max-lifetime=1800000

# JPA/Hibernate Configuration
spring.jpa.hibernate.ddl-auto=validate
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.MariaDBDialect
spring.jpa.show-sql=false
spring.jpa.properties.hibernate.format_sql=false

# Liquibase Configuration
spring.liquibase.enabled=false
spring.liquibase.change-log=classpath:db/changelog/db.changelog-master.yml
spring.liquibase.contexts=docker
spring.liquibase.drop-first=false
spring.liquibase.default-schema=dev-planning

# CORS Configuration
spring.web.cors.allowed-origins=http://frontend:3000,http://localhost:3000,http://127.0.0.1:3000
spring.web.cors.allowed-methods=GET,POST,PUT,DELETE,OPTIONS
spring.web.cors.allowed-headers=*

# Logging
logging.level.root=INFO
logging.level.com.pcagrade=INFO
logging.level.org.springframework.web=WARN
logging.level.org.hibernate.SQL=WARN

# Planning Configuration
planning.card.processing.time=3
planning.employee.break.time=15
planning.workday.start=08:00
planning.workday.end=17:00

# Management endpoints
management.endpoints.web.exposure.include=health,info,metrics
management.endpoint.health.show-details=always

# Order table compatibility
order.table.readonly=true
order.status.integer.mapping=true
order.adapter.service.enabled=true
```

---

## Option 3: Environment Variables (.env)

**File:** `.env`

```bash
# Database Configuration
DB_HOST=localhost
DB_PORT=3306
DB_NAME=dev-planning
DB_USER=YOUR_USERNAME
DB_PASSWORD=YOUR_PASSWORD

# Root password for database initialization
DB_ROOT_PASSWORD=YOUR_ROOT_PASSWORD

# Application
SERVER_PORT=8080
SPRING_PROFILES_ACTIVE=local
```

Then update `application.properties` to use these:

```properties
spring.datasource.url=jdbc:mariadb://${DB_HOST:localhost}:${DB_PORT:3306}/${DB_NAME:dev-planning}?useSSL=false&allowPublicKeyRetrieval=true
spring.datasource.username=${DB_USER:ia}
spring.datasource.password=${DB_PASSWORD:foufafou}
```

---

## Docker Compose Update

**File:** `docker-compose.yml`

```yaml
version: '3.8'

services:
  database:
    image: mariadb:11.2
    container_name: pokemon-planning-db
    environment:
      MYSQL_ROOT_PASSWORD: ${DB_ROOT_PASSWORD}
      MYSQL_DATABASE: dev-planning  # ← CHANGED
      MYSQL_USER: ${DB_USER}
      MYSQL_PASSWORD: ${DB_PASSWORD}
    ports:
      - "3306:3306"
    volumes:
      - db_data:/var/lib/mysql
    healthcheck:
      test: ["CMD", "healthcheck.sh", "--connect", "--innodb_initialized"]
      interval: 10s
      timeout: 5s
      retries: 5

  backend:
    image: your-username/pokemon-planning-backend:latest
    container_name: pokemon-planning-backend
    environment:
      SPRING_PROFILES_ACTIVE: docker
      SPRING_DATASOURCE_URL: jdbc:mariadb://database:3306/dev-planning?useSSL=false&allowPublicKeyRetrieval=true
      SPRING_DATASOURCE_USERNAME: ${DB_USER}
      SPRING_DATASOURCE_PASSWORD: ${DB_PASSWORD}
    ports:
      - "8080:8080"
    depends_on:
      database:
        condition: service_healthy

  frontend:
    image: your-username/pokemon-planning-frontend:latest
    container_name: pokemon-planning-frontend
    ports:
      - "3000:3000"
    depends_on:
      - backend

volumes:
  db_data:
```

---

## Step-by-Step Migration Process

### 1️⃣ Run Migration Script

```bash
mysql -u YOUR_USERNAME -p < migrate_to_dev_planning.sql
```

Or connect and run:
```bash
mysql -u YOUR_USERNAME -p
source migrate_to_dev_planning.sql
```

### 2️⃣ Update Backend Configuration

Choose one of the options above (local, docker, or environment variables)

### 3️⃣ Disable Liquibase (Important!)

Since you're copying existing tables, disable Liquibase:

```properties
spring.liquibase.enabled=false
```

Or set:
```properties
spring.jpa.hibernate.ddl-auto=validate
```

### 4️⃣ Restart Backend

**Local:**
```bash
./mvnw spring-boot:run
```

**Docker:**
```bash
docker-compose down
docker-compose up -d
```

### 5️⃣ Verify Connection

```bash
curl http://localhost:8080/api/employees
```

Should return list of employees (or empty array if none exist yet)

---

## Verification Checklist

After migration, verify:

- ✅ Backend starts without errors
- ✅ Database connection successful
- ✅ Orders are accessible: `curl http://localhost:8080/api/orders`
- ✅ Employees are accessible: `curl http://localhost:8080/api/employees`
- ✅ Planning generation works
- ✅ Frontend connects to backend

---

## Troubleshooting

### Issue: "Unknown database 'dev-planning'"

**Solution:** Run the migration script first to create the database

### Issue: "Access denied for user"

**Solution:** Check username/password in configuration match database credentials

### Issue: "Table doesn't exist"

**Solution:**
1. Check migration script completed successfully
2. Verify you're connecting to the correct database
3. Run: `SHOW TABLES;` in dev-planning database

### Issue: Backend won't start after change

**Solution:**
1. Check application logs
2. Verify datasource URL format
3. Ensure database is running: `mysql -u YOUR_USERNAME -p dev-planning`

---

## Rollback Plan

If you need to switch back to "dev":

1. **Update configuration:**
   ```properties
   spring.datasource.url=jdbc:mariadb://localhost:3306/dev?useSSL=false...
   ```

2. **Restart backend**

The original "dev" database is unchanged and can be used anytime.

---

## Important Notes

⚠️ **Liquibase:** Set `spring.liquibase.enabled=false` since tables are already created

⚠️ **Data Sync:** Changes in dev-planning won't affect original "dev" database

⚠️ **Backups:** Consider backing up both databases:
```bash
mysqldump -u root -p dev > dev_backup.sql
mysqldump -u root -p dev-planning > dev-planning_backup.sql
```

✅ **Independence:** Both databases can coexist and be used separately