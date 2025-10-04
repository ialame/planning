# 🔧 Configuration Guide - Pokemon Card Planning

This guide explains how to set up your development environment and deploy the application securely.

## 📋 Table of Contents

- [Initial Setup](#initial-setup)
- [Development Configuration](#development-configuration)
- [Docker Configuration](#docker-configuration)
- [Security Best Practices](#security-best-practices)

---

## 🚀 Initial Setup

### 1. Clone the Repository

```bash
git clone https://github.com/YOUR_USERNAME/pokemon-planning.git
cd pokemon-planning
```

### 2. Copy Template Files

```bash
# Backend configuration
cp src/main/resources/application-local.properties.example src/main/resources/application-local.properties

# Environment variables
cp .env.example .env

# Frontend environment (if needed)
cp src/main/frontend/.env.example src/main/frontend/.env

```

### 3. Update Configuration Files

Edit the copied files and replace placeholder values with your actual credentials:

- `application-local.properties` - Database credentials for local development
- `.env` - Docker and general environment variables

---

## 💻 Development Configuration

### Backend (Spring Boot)

1. **Edit `application-local.properties`**:
   ```properties
   spring.datasource.url=jdbc:mariadb://localhost:3306/dev
   spring.datasource.username=YOUR_USERNAME
   spring.datasource.password=YOUR_PASSWORD
   ```

2. **Set Active Profile**:
    - By default, Spring Boot will use `application-local.properties` if it exists
    - Or set in IDE: VM options → `-Dspring.profiles.active=local`

3. **Run the Application**:
   ```bash
   ./mvnw spring-boot:run
   ```

### Frontend (Vue.js)

1. **Navigate to frontend directory**:
   ```bash
   cd src/main/frontend
   ```

2. **Install dependencies**:
   ```bash
   npm install
   ```

3. **Configure API URL** (automatic):
    - Development: Uses `http://localhost:8080`
    - Production: Auto-detects backend URL

4. **Run development server**:
   ```bash
   npm run dev
   ```

---

## 🐳 Docker Configuration

### 1. Set Environment Variables

Edit `.env` file:
```bash
DB_ROOT_PASSWORD=secure_password
DB_USER=ia
DB_PASSWORD=your_password
```

### 2. Build Images

```bash
# Backend
docker build -t YOUR_USERNAME/pokemon-planning-backend:latest .

# Frontend
cd src/main/frontend
docker buildx build --platform linux/amd64 \
  -t YOUR_USERNAME/pokemon-planning-frontend:latest \
  --push .
```

### 3. Run with Docker Compose (optional)

```bash
docker-compose up -d
```

---

## 🔐 Security Best Practices

### ⚠️ NEVER Commit These Files:

- `application-local.properties`
- `.env`
- Any file with real passwords or API keys
- Database dumps (`.sql` files)
- Deployment scripts with credentials

### ✅ Always:

1. **Use template files** (`.example` extension)
2. **Use environment variables** for sensitive data
3. 
4. **Rotate passwords** regularly
5. **Use strong passwords** (16+ characters, mixed case, numbers, symbols)
6. **Review `.gitignore`** before committing
7. **Use `.git/info/exclude`** for personal ignores

### 🔍 Before Pushing to GitHub:

```bash
# Check for sensitive data
grep -r "password" . --exclude-dir={.git,target,node_modules,dist}

# Check what will be committed
git status
git diff --cached

# Verify .gitignore is working
git check-ignore -v application-local.properties
git check-ignore -v .env
```

---

## 📚 Additional Resources

- [Spring Boot Configuration](https://docs.spring.io/spring-boot/docs/current/reference/html/application-properties.html)
- [Docker Best Practices](https://docs.docker.com/develop/dev-best-practices/)
- [Vue.js Environment Variables](https://vitejs.dev/guide/env-and-mode.html)

---

## 🆘 Troubleshooting

### Database Connection Issues

- Verify credentials in `application-local.properties`
- Check if MariaDB is running: `mysql -u USERNAME -p`
- Check port: `netstat -an | grep 3306`

### Frontend API Connection Issues

- Check browser console for API URL
- Verify backend is accessible: `curl http://BACKEND_IP:8080/api/health`
- Check CORS configuration

---

## 📞 Support

For issues or questions:
1. Check this configuration guide
2. Review application logs
3. Create an issue in the repository