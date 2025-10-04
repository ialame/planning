# 🚀 GitHub Preparation Checklist

Follow these steps to securely prepare your repository for GitHub.

## ✅ Pre-Push Checklist

### Step 1: Copy Template Files

```bash
# Make setup script executable
chmod +x setup-dev-env.sh

# Backend configuration template
cp src/main/resources/application-local.properties.example src/main/resources/application-local.properties

# Environment variables template
cp .env.example .env

# Kubernetes deployment template
cp kubernetes-deployment.yaml.example kubernetes-deployment.yaml

# Frontend environment
cd src/main/frontend
cp .env.example .env
cd ../..
```

### Step 2: Remove Sensitive Files from Git History

If you've already committed sensitive files, clean them:

```bash
# Check what's currently tracked
git ls-files | grep -E '(application-local|\.env$|deployment\.yaml)'

# If sensitive files are tracked, remove them from git (but keep locally)
git rm --cached src/main/resources/application-local.properties 2>/dev/null || true
git rm --cached .env 2>/dev/null || true
git rm --cached kubernetes-deployment.yaml 2>/dev/null || true
git rm --cached pokemon-planning-deployment.yaml 2>/dev/null || true
git rm --cached src/main/frontend/.env 2>/dev/null || true
git rm --cached src/main/frontend/.env.production 2>/dev/null || true

# Commit the removal
git commit -m "chore: remove sensitive configuration files from tracking"
```

### Step 3: Search for Hardcoded Credentials

```bash
# Search for potential passwords (exclude .git, target, node_modules)
echo "🔍 Searching for 'password' references..."
grep -r "password" . \
  --exclude-dir={.git,target,node_modules,dist,.idea} \
  --exclude="*.example" \
  --exclude="*.md" \
  --exclude=".gitignore"

echo ""
echo "🔍 Searching for 'foufafou' (example password)..."
grep -r "foufafou" . \
  --exclude-dir={.git,target,node_modules,dist,.idea} \
  --exclude="*.example" \
  --exclude="*.md" \
  --exclude=".gitignore"

echo ""
echo "🔍 Searching for potential credentials..."
grep -rE "(password|secret|api[_-]?key|token)" . \
  --exclude-dir={.git,target,node_modules,dist,.idea} \
  --exclude="*.example" \
  --exclude="*.md" \
  --exclude=".gitignore" \
  | grep -v "PASSWORD_HERE" \
  | grep -v "YOUR_" \
  | head -20
```

### Step 4: Update Configuration Files

**Replace hardcoded credentials in these files:**

1. `src/main/resources/application.properties` → Use placeholders
2. `src/main/resources/application-docker.properties` → Use environment variables
3. `src/main/resources/application-kubernetes.properties` → Use environment variables

**Example updates:**

```properties
# Before (NEVER commit this):
spring.datasource.password=foufafou

# After (safe to commit):
spring.datasource.password=${DB_PASSWORD:default_dev_password}
```

### Step 5: Update Deployment Scripts

Remove hardcoded IPs and credentials from:
- `deploy-frontend.sh` → Create `.example` version
- Any other deployment scripts

### Step 6: Verify .gitignore is Working

```bash
# Test that sensitive files are ignored
echo "Testing .gitignore..."

git check-ignore -v src/main/resources/application-local.properties
git check-ignore -v .env
git check-ignore -v kubernetes-deployment.yaml
git check-ignore -v src/main/frontend/.env.production

# Should show these files are ignored
```

### Step 7: Review What Will Be Committed

```bash
# See all files that will be committed
git status

# Review changes
git diff

# See all tracked files
git ls-files

# Make sure NO sensitive files are listed!
```

### Step 8: Clean Up Local Files

```bash
# Remove any SQL dumps or backups
find . -name "*.sql" -type f ! -path "./.git/*" -delete

# Remove any local deployment files
rm -f pokemon-planning-deployment.yaml
rm -f deploy-frontend.sh  # Keep the .example version
rm -f setup-env.sh  # If it contains secrets

# Remove any logs with potential sensitive data
find . -name "*.log" -type f ! -path "./.git/*" -delete
```

### Step 9: Update README.md

Add security notice to README:

```markdown
## 🔐 Security & Configuration

**IMPORTANT:** This repository does not contain sensitive credentials.

For local development:
1. Copy template files: `./setup-dev-env.sh`
2. Edit configuration files with your credentials
3. See [CONFIG.md](CONFIG.md) for detailed instructions

Never commit:
- `application-local.properties`
- `.env` files
- `*-deployment.yaml` (actual deployment files)
- Any files with real passwords or API keys
```

### Step 10: Create First Secure Commit

```bash
# Add all safe files
git add .

# Review what's staged
git status
git diff --cached --name-only

# Commit with clear message
git commit -m "feat: initial commit with secure configuration templates

- Add template files for local configuration
- Secure .gitignore for sensitive files
- Add setup scripts and documentation
- Remove hardcoded credentials from tracked files"

# Push to GitHub
git push origin main
```

## 🔒 Post-Push Verification

After pushing to GitHub:

1. **Visit your repository on GitHub**
2. **Check that sensitive files are NOT visible:**
    - `application-local.properties` ❌ Should NOT be there
    - `.env` ❌ Should NOT be there
    - `kubernetes-deployment.yaml` ❌ Should NOT be there
    - `application-local.properties.example` ✅ Should be there
    - `.env.example` ✅ Should be there
    - `kubernetes-deployment.yaml.example` ✅ Should be there

3. **Search your repository on GitHub** for sensitive strings:
    - Use GitHub's search: Press `/` and search for "foufafou"
    - Search for "password="
    - If found in non-example files, IMMEDIATELY follow "Emergency Cleanup" below

## 🚨 Emergency: If Sensitive Data Was Pushed

If you accidentally pushed sensitive data:

### Option 1: Remove from Last Commit (if just pushed)

```bash
# Remove file and amend commit
git rm --cached src/main/resources/application-local.properties
git commit --amend -m "feat: initial secure commit"
git push --force origin main
```

### Option 2: Use BFG Repo Cleaner (for older commits)

```bash
# Install BFG
brew install bfg  # macOS
# or download from: https://rtyley.github.io/bfg-repo-cleaner/

# Clone a fresh copy
git clone --mirror https://github.com/YOUR_USERNAME/pokemon-planning.git

# Remove passwords from all commits
bfg --replace-text passwords.txt pokemon-planning.git

# Push cleaned history
cd pokemon-planning.git
git reflog expire --expire=now --all
git gc --prune=now --aggressive
git push --force
```

### Option 3: Rotate All Credentials

If sensitive data was exposed:

1. **Change all passwords immediately**
2. **Revoke any API keys**
3. **Update all deployed instances**
4. **Consider repository private until cleaned**

## 📚 Maintenance

### Before Every Commit:

```bash
# Always check what you're committing
git status
git diff

# Run a quick security check
./check-secrets.sh  # Create this script

# Only commit if all checks pass
```

### Create a Pre-Commit Hook:

```bash
# Create .git/hooks/pre-commit
cat > .git/hooks/pre-commit << 'EOF'
#!/bin/bash
if git diff --cached --name-only | grep -E '(application-local\.properties|^\.env$|deployment\.yaml)'; then
    echo "❌ ERROR: Attempting to commit sensitive files!"
    echo "These files should not be committed:"
    git diff --cached --name-only | grep -E '(application-local\.properties|^\.env$|deployment\.yaml)'
    exit 1
fi
EOF

chmod +x .git/hooks/pre-commit
```

## ✅ Final Checklist

Before pushing to GitHub, verify:

- [ ] All sensitive files are in `.gitignore`
- [ ] All configuration uses templates (`.example` files)
- [ ] No hardcoded passwords in tracked files
- [ ] All deployment scripts use environment variables
- [ ] README includes security notice
- [ ] `CONFIG.md` has setup instructions
- [ ] Search found no sensitive strings
- [ ] `git status` shows only safe files
- [ ] Pre-commit hook is installed (optional but recommended)

## 🎯 Success!

If all checks pass, your repository is ready for GitHub! 🎉

Remember:
- Keep template files updated
- Document any new configuration requirements
- Review `.gitignore` when adding new config files
- Never commit real credentials, even temporarily