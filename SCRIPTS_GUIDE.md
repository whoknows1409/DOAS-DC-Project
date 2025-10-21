# 🛠️ DOAS Scripts Guide

This guide provides an overview of all available scripts for managing the DOAS application.

---

## 📋 Table of Contents

- [Application Management](#-application-management)
- [Database Management](#-database-management)
- [System Utilities](#-system-utilities)
- [Quick Reference](#-quick-reference)

---

## 🚀 Application Management

### Start Application

```bash
./scripts/start.sh
```

**Features:**
- Starts all services in the correct order
- Waits for database to be ready
- Health checks on backend servers
- Displays access URLs and status

**Options:**
```bash
./scripts/start.sh start    # Start the system (default)
./scripts/start.sh stop     # Stop the system
./scripts/start.sh restart  # Restart the system
./scripts/start.sh status   # Show system status
./scripts/start.sh logs     # Show container logs
./scripts/start.sh health   # Perform health check
./scripts/start.sh clean    # Stop and remove all data
./scripts/start.sh help     # Show help message
```

**Services started:**
1. PostgreSQL database
2. Redis cache
3. Backend Server 1 (port 8081)
4. Backend Server 2 (port 8082)
5. Backend Server 3 (port 8083)
6. Nginx load balancer (port 80)

---

### Stop Application

```bash
./scripts/stop.sh
```

**Features:**
- Stops all running containers gracefully
- Shows current status
- Provides information about running containers
- Suggests next steps

**What it does:**
- Executes `docker-compose down`
- Stops all DOAS containers
- Preserves data volumes
- Shows summary of stopped services

**Note:** This does NOT remove data. To remove data as well:
```bash
docker-compose down -v
```

---

### Restart Application

```bash
./scripts/restart.sh
```

**Features:**
- Stops the application
- Waits 5 seconds for graceful shutdown
- Starts the application fresh
- Shows final status

**Use cases:**
- After configuration changes
- After code updates
- When services become unresponsive
- To apply environment variable changes

---

### Check Status

```bash
./scripts/status.sh
```

**Features:**
- Shows all container statuses
- Displays access URLs
- Performs health checks on all services
- Shows summary statistics
- Provides quick command references

**Health checks:**
- ✓ Backend Server 1, 2, 3
- ✓ Frontend/Nginx
- ✓ PostgreSQL
- ✓ Redis

**Output includes:**
- Container running status
- Health check results
- Access URLs for all services
- Quick commands for common tasks

---

## 🗄️ Database Management

### Reset Database

```bash
./scripts/reset-database.sh
```

**Features:**
- Clears all data from database
- Keeps database structure
- Preserves users table
- Shows before/after statistics

**What it deletes:**
- All auctions
- All bids
- All associated data

**What it preserves:**
- Database schema
- Users
- Table structure

---

### Clear Auctions Only

```bash
./scripts/clear-auctions.sh
```

**Features:**
- Deletes only auctions and bids
- Preserves users
- Shows statistics before/after
- Quick cleanup for testing

**Use cases:**
- Testing new features
- Demo preparation
- Cleaning up test data

---

### Complete System Reset

```bash
./scripts/complete-reset.sh
```

**⚠️ WARNING: This is destructive!**

**Features:**
- Stops all containers
- Removes all volumes
- Deletes all data
- Removes Docker images
- Fresh clean slate

**What it deletes:**
- All containers
- All volumes
- All database data
- All Redis data
- All cached images

**Use when:**
- Starting completely fresh
- Troubleshooting persistent issues
- Before major version changes

---

## 🔧 System Utilities

### Build Docker Images

```bash
./scripts/docker-build.sh
```

**Features:**
- Builds backend JAR file
- Builds frontend production build
- Creates Docker images
- Shows build progress

**When to use:**
- After code changes
- Before deployment
- To update dependencies

**What it does:**
1. Builds backend with Maven
2. Builds frontend with npm
3. Creates Docker images
4. Tags images appropriately

---

### Validate System

```bash
./scripts/validate-system.sh
```

**Features:**
- Comprehensive system validation
- Tests all endpoints
- Checks all services
- Verifies configuration
- Shows detailed results

**Validation checks:**
- Docker and Docker Compose installation
- Container health
- Network connectivity
- API endpoints
- Database connections
- Redis connections
- RMI communication
- Load balancing
- File permissions

---

### Demo Complete System

```bash
./scripts/demo-complete-system.sh
```

**Features:**
- Automated demonstration script
- Shows all distributed features
- Tests election algorithms
- Demonstrates 2PC
- Shows load balancing

**Includes:**
- System startup
- RMI verification
- Clock synchronization demo
- Bully election simulation
- Load balancing test
- Performance metrics

---

### Verify GitHub Ready

```bash
./scripts/verify-github-ready.sh
```

**Features:**
- Checks for sensitive data
- Verifies file structure
- Validates .gitignore
- Checks documentation
- Shows repository statistics

**Use before:**
- Pushing to GitHub
- Creating releases
- Sharing the project

---

## ⚡ Quick Reference

### Daily Development

```bash
# Start working
./scripts/start.sh

# Check if everything is running
./scripts/status.sh

# View logs
docker-compose logs -f

# Stop when done
./scripts/stop.sh
```

---

### Testing & Demo

```bash
# Start fresh
./scripts/complete-reset.sh
./scripts/start.sh

# Run demonstration
./scripts/demo-complete-system.sh

# Validate everything works
./scripts/validate-system.sh
```

---

### Database Operations

```bash
# Clear test data
./scripts/clear-auctions.sh

# Reset database
./scripts/reset-database.sh

# Access database directly
docker exec -it auction-system-postgres-1 psql -U auctionuser -d auctiondb
```

---

### Troubleshooting

```bash
# Check status
./scripts/status.sh

# View logs
docker-compose logs -f [service-name]

# Restart everything
./scripts/restart.sh

# Complete reset (if nothing works)
./scripts/complete-reset.sh
./scripts/docker-build.sh
./scripts/start.sh
```

---

## 🎯 Common Use Cases

### 1. **Starting Development Session**

```bash
# Quick start
./scripts/start.sh

# Or with verbose output
./scripts/start.sh start
```

### 2. **After Making Code Changes**

```bash
# Rebuild and restart
./scripts/docker-build.sh
./scripts/restart.sh
```

### 3. **Preparing for Demo**

```bash
# Clean slate
./scripts/complete-reset.sh

# Build fresh
./scripts/docker-build.sh

# Start system
./scripts/start.sh

# Validate
./scripts/validate-system.sh

# Run demo
./scripts/demo-complete-system.sh
```

### 4. **Before Committing to Git**

```bash
# Verify codebase is clean
./scripts/verify-github-ready.sh

# Stop application
./scripts/stop.sh

# Commit changes
git add .
git commit -m "your message"
```

### 5. **Troubleshooting Issues**

```bash
# Step 1: Check status
./scripts/status.sh

# Step 2: View logs
docker-compose logs -f

# Step 3: Try restart
./scripts/restart.sh

# Step 4: If still broken, complete reset
./scripts/complete-reset.sh
./scripts/start.sh
```

---

## 📊 Script Permissions

All scripts should be executable. If you get "permission denied":

```bash
# Make all scripts executable
chmod +x scripts/*.sh

# Or individual script
chmod +x scripts/start.sh
```

---

## 🔍 Environment Variables

Some scripts use environment variables. See `.env.example` for configuration options:

```bash
# Copy example environment
cp .env.example .env

# Edit as needed
nano .env
```

---

## 📝 Notes

1. **Always use scripts from project root:**
   ```bash
   cd /path/to/auction-system
   ./scripts/start.sh
   ```

2. **Docker must be running:**
   - Ensure Docker Desktop is started (on Mac/Windows)
   - Docker daemon is running (on Linux)

3. **Ports must be available:**
   - 80 (Nginx)
   - 5432 (PostgreSQL)
   - 6379 (Redis)
   - 8081, 8082, 8083 (Backend servers)
   - 1101, 1102, 1103 (RMI ports)

4. **Script execution order matters:**
   - Stop before complete reset
   - Build before start (after code changes)
   - Validate after start

---

## 🆘 Getting Help

Each script has a help option:

```bash
./scripts/start.sh help
./scripts/start.sh --help
./scripts/start.sh -h
```

For more information, see:
- [README.md](README.md) - Complete documentation
- [DEMO_GUIDE.md](DEMO_GUIDE.md) - Demonstration guide
- [DATABASE_COMMANDS.md](DATABASE_COMMANDS.md) - Database operations

---

**Happy coding!** 🚀

