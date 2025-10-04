# 🔧 Configuration Guide - Pokemon Card Planning

This guide explains how to set up your development environment and deploy the application securely.

## 📋 Table of Contents

- [Initial Setup](#initial-setup)
- [Development Configuration](#development-configuration)
- [Docker Configuration](#docker-configuration)
- [Kubernetes Deployment](#kubernetes-deployment)
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

# Kubernetes deployment (for production)
cp kubernetes-deployment.yaml.example kubernetes-deployment.yaml
```

### 3. Update Configuration Files

Edit the copied files and replace placeholder values with your actual credentials:

- `application-local.properties` - Database credentials for local development
- `.env` - Docker and general environment variables
- `kubernetes-deployment.yaml` - Kubernetes secrets and Docker Hub username

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

## ☸️ Kubernetes Deployment

### Prerequisites

- Kubernetes cluster (DigitalOcean, AWS, GCP, etc.)
- `kubectl` configured
- Docker images pushed to Docker Hub

### 1. Configure Deployment File

Edit `kubernetes-deployment.yaml`:

```yaml
# Update these values
stringData:
  root-password: YOUR_ROOT_PASSWORD
  username: YOUR_DB_USERNAME
  password: YOUR_DB_PASSWORD

# Update Docker Hub username
image: YOUR_DOCKERHUB_USERNAME/pokemon-planning-backend:latest
```

### 2. Create Secrets

```bash
kubectl create secret generic database-secret \
  --from-literal=root-password='YOUR_PASSWORD' \
  --from-literal=username='YOUR_USERNAME' \
  --from-literal=password='YOUR_PASSWORD' \
  -n pokemon-planning
```

### 3. Deploy Application

```bash
kubectl apply -f kubernetes-deployment.yaml
```

### 4. Verify Deployment

```bash
# Check pods
kubectl get pods -n pokemon-planning

# Check services
kubectl get svc -n pokemon-planning

# Get external IPs
kubectl get svc -n pokemon-planning | grep LoadBalancer
```

### 5. Update Frontend with Backend IP

```bash
# Get backend IP
BACKEND_IP=$(kubectl get svc backend-loadbalancer -n pokemon-planning -o jsonpath='{.status.loadBalancer.ingress[0].ip}')

# Create frontend .env.production
cd src/main/frontend
echo "VITE_API_BASE_URL=http://${BACKEND_IP}:8080" > .env.production

# Rebuild and deploy frontend
npm run build
docker buildx build --platform linux/amd64 \
  -t YOUR_USERNAME/pokemon-planning-frontend:latest \
  --push .

kubectl set image deployment/frontend \
  vue-frontend=YOUR_USERNAME/pokemon-planning-frontend:latest \
  -n pokemon-planning
```

---

## 🔐 Security Best Practices

### ⚠️ NEVER Commit These Files:

- `application-local.properties`
- `.env`
- `kubernetes-deployment.yaml` (actual file, only commit `.example`)
- Any file with real passwords or API keys
- Database dumps (`.sql` files)
- Deployment scripts with credentials

### ✅ Always:

1. **Use template files** (`.example` extension)
2. **Use environment variables** for sensitive data
3. **Use Kubernetes Secrets** for production
4. **Rotate passwords** regularly
5. **Use strong passwords** (16+ characters, mixed case, numbers, symbols)
6. **Review `.gitignore`** before committing
7. **Use `.git/info/exclude`** for personal ignores

### 🔍 Before Pushing to GitHub:

```bash
# Check for sensitive data
grep -r "password" . --exclude-dir={.git,target,node_modules,dist}
grep -r "foufafou" . --exclude-dir={.git,target,node_modules,dist}

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
- [Kubernetes Secrets](https://kubernetes.io/docs/concepts/configuration/secret/)
- [Docker Best Practices](https://docs.docker.com/develop/dev-best-practices/)
- [Vue.js Environment Variables](https://vitejs.dev/guide/env-and-mode.html)

---

## 🆘 Troubleshooting

### Database Connection Issues

- Verify credentials in `application-local.properties`
- Check if MariaDB is running: `mysql -u USERNAME -p`
- Check port: `netstat -an | grep 3306`

### Kubernetes Deployment Issues

- Check pod logs: `kubectl logs -n pokemon-planning POD_NAME`
- Describe pod: `kubectl describe pod -n pokemon-planning POD_NAME`
- Check secrets: `kubectl get secrets -n pokemon-planning`

### Frontend API Connection Issues

- Check browser console for API URL
- Verify backend is accessible: `curl http://BACKEND_IP:8080/api/health`
- Check CORS configuration

---

## 📞 Support

For issues or questions:
1. Check this configuration guide
2. Review application logs
3. Check Kubernetes events
4. Create an issue in the repository