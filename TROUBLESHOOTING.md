# 🔧 DOAS Troubleshooting Guide

Common issues and their solutions for the DOAS application.

---

## 📋 Table of Contents

- [Frontend Issues](#frontend-issues)
- [Backend Issues](#backend-issues)
- [Database Issues](#database-issues)
- [Docker Issues](#docker-issues)
- [Network Issues](#network-issues)

---

## 🌐 Frontend Issues

### Issue: 403 Forbidden Error

**Symptoms:**
- Browser shows "403 Forbidden" when accessing http://localhost
- Nginx returns 403 error
- Frontend not loading

**Root Cause:**
Frontend build directory is empty or missing.

**Solution:**
```bash
# Navigate to project root
cd /path/to/auction-system

# Build the frontend
cd frontend
npm install --legacy-peer-deps
npm run build
cd ..

# Restart Nginx
docker-compose restart nginx

# Verify
curl http://localhost
```

**Quick Fix:**
```bash
./scripts/docker-build.sh frontend-only
docker-compose restart nginx
```

---

### Issue: React App Not Loading (Blank Page)

**Symptoms:**
- Page loads but shows blank white screen
- Console shows errors about missing chunks

**Solution:**
```bash
# Clear browser cache
# Or try incognito mode

# Rebuild frontend
cd frontend
rm -rf build node_modules
npm install --legacy-peer-deps
npm run build
cd ..

# Restart Nginx
docker-compose restart nginx
```

---

### Issue: CSS Not Loading / Styling Issues

**Symptoms:**
- Page loads but has no styling
- Console shows 404 for CSS files

**Solution:**
```bash
# Check if static files exist
ls -la frontend/build/static/

# Rebuild if empty
cd frontend
npm run build
cd ..

docker-compose restart nginx
```

---

### Issue: Images Not Visible After Upload

**Symptoms:**
- Images upload successfully but don't display
- Broken image icons in browser
- 404 errors when accessing image URLs

**Solution:**
```bash
# The system now uses persistent Docker volumes
# Images are automatically saved and served correctly

# Verify images are accessible
curl http://localhost/uploads/filename.jpg

# Check volume
docker volume inspect auction-system_uploads_data

# If images still not visible, restart services
docker-compose restart nginx
```

**How it works:**
- Images are stored in Docker volume `uploads_data`
- Volume is mounted to all backend servers at `/app/uploads`
- Nginx serves images directly from `/uploads/` path
- Images persist across container restarts

---

## 🖥️ Backend Issues

### Issue: Server Not Responding

**Symptoms:**
- Backend servers return connection refused
- API calls fail

**Solution:**
```bash
# Check if servers are running
docker-compose ps

# Check logs
docker-compose logs auction-server-1

# Restart specific server
docker-compose restart auction-server-1

# Or restart all
./scripts/restart.sh
```

---

### Issue: RMI Connection Failures

**Symptoms:**
- Logs show "Connection refused" for RMI
- Election algorithms failing
- Servers can't communicate

**Solution:**
```bash
# Check RMI ports are exposed
docker-compose ps

# Verify network
docker network inspect auction-system_auction-network

# Restart all servers
./scripts/restart.sh

# Check logs for RMI binding
docker-compose logs | grep RMI
```

---

### Issue: Database Connection Errors

**Symptoms:**
- Backend logs show "Connection refused" to PostgreSQL
- API returns 500 errors

**Solution:**
```bash
# Check if database is running
docker-compose ps postgres

# Check database logs
docker-compose logs postgres

# Restart database
docker-compose restart postgres

# Wait 10 seconds, then restart backend
sleep 10
docker-compose restart auction-server-1 auction-server-2 auction-server-3
```

---

## 🗄️ Database Issues

### Issue: Database Won't Start

**Symptoms:**
- PostgreSQL container exits immediately
- Logs show initialization errors

**Solution:**
```bash
# Stop all containers
docker-compose down

# Remove database volume
docker volume rm auction-system_postgres_data

# Start fresh
docker-compose up -d

# Database will reinitialize from init.sql
```

---

### Issue: Tables Not Found

**Symptoms:**
- API returns errors about missing tables
- Fresh database not initialized

**Solution:**
```bash
# Check if init.sql was run
docker exec -it auction-system-postgres-1 \
  psql -U auctionuser -d auctiondb -c "\dt"

# If empty, manually initialize
docker exec -it auction-system-postgres-1 \
  psql -U auctionuser -d auctiondb -f /docker-entrypoint-initdb.d/init.sql

# Or reset database
./scripts/reset-database.sh
```

---

## 🐳 Docker Issues

### Issue: Port Already in Use

**Symptoms:**
- Error: "port is already allocated"
- Container fails to start

**Solution:**
```bash
# Check what's using the port
sudo lsof -i :80
sudo lsof -i :5432

# Stop conflicting service
sudo systemctl stop apache2  # or nginx, etc.

# Or change port in docker-compose.yml
# ports:
#   - "8080:80"  # Use 8080 instead of 80
```

---

### Issue: Out of Disk Space

**Symptoms:**
- Container fails to build
- Error: "no space left on device"

**Solution:**
```bash
# Clean up Docker
docker system prune -a
docker volume prune

# Remove unused images
docker image prune -a

# Check disk space
df -h
```

---

### Issue: Container Keeps Restarting

**Symptoms:**
- Container status shows "Restarting"
- Container exits immediately

**Solution:**
```bash
# Check logs for the specific container
docker-compose logs auction-server-1

# Common causes:
# - Port conflict
# - Missing environment variables
# - Configuration errors

# Fix and restart
docker-compose restart auction-server-1
```

---

## 🌐 Network Issues

### Issue: Containers Can't Communicate

**Symptoms:**
- Backend can't reach database
- Servers can't communicate via RMI

**Solution:**
```bash
# Check network
docker network inspect auction-system_auction-network

# Recreate network
docker-compose down
docker-compose up -d

# Verify all containers on same network
docker network inspect auction-system_auction-network | \
  grep -A 3 "Containers"
```

---

### Issue: Load Balancing Not Working

**Symptoms:**
- All requests go to same server
- Nginx not distributing load

**Solution:**
```bash
# Check Nginx configuration
docker exec auction-system-nginx-1 cat /etc/nginx/nginx.conf

# Verify backend servers are healthy
curl http://localhost:8081/api/auctions/status
curl http://localhost:8082/api/auctions/status
curl http://localhost:8083/api/auctions/status

# Restart Nginx
docker-compose restart nginx

# Test load distribution
for i in {1..10}; do 
  curl -s http://localhost/api/auctions/status | jq .serverId
done
```

---

## 🔄 General Troubleshooting Steps

### Step 1: Check System Status
```bash
./scripts/status.sh
```

### Step 2: View Logs
```bash
# All services
docker-compose logs -f

# Specific service
docker-compose logs -f auction-server-1

# Last 50 lines
docker-compose logs --tail 50 auction-server-1
```

### Step 3: Restart Services
```bash
# Restart specific service
docker-compose restart auction-server-1

# Restart all
./scripts/restart.sh

# Nuclear option - complete reset
./scripts/complete-reset.sh
./scripts/start.sh
```

### Step 4: Verify Configuration
```bash
# Check environment variables
docker-compose config

# Validate docker-compose.yml
docker-compose config --quiet
```

### Step 5: Check Resources
```bash
# CPU and memory usage
docker stats

# Disk space
df -h

# Docker disk usage
docker system df
```

---

## 🆘 Emergency Recovery

If nothing works, use the nuclear option:

```bash
# Stop everything
docker-compose down -v

# Clean Docker system
docker system prune -a
docker volume prune

# Rebuild from scratch
./scripts/docker-build.sh

# Start fresh
./scripts/start.sh

# Verify
./scripts/status.sh
```

---

## 📞 Getting Help

If you're still having issues:

1. **Check logs** - Most issues show up in logs
   ```bash
   docker-compose logs -f
   ```

2. **Verify configuration** - Check environment variables
   ```bash
   docker-compose config
   ```

3. **Test components individually**
   ```bash
   # Database
   docker exec -it auction-system-postgres-1 psql -U auctionuser -d auctiondb
   
   # Redis
   docker exec -it auction-system-redis-1 redis-cli PING
   
   # Backend
   curl http://localhost:8081/api/auctions/status
   ```

4. **Open an issue** with:
   - Full error message
   - Relevant logs
   - Steps to reproduce
   - System information (OS, Docker version)

---

## 🎯 Prevention Tips

1. **Always build before starting**
   ```bash
   ./scripts/docker-build.sh
   ./scripts/start.sh
   ```

2. **Check status regularly**
   ```bash
   ./scripts/status.sh
   ```

3. **Monitor logs**
   ```bash
   docker-compose logs -f
   ```

4. **Keep Docker clean**
   ```bash
   docker system prune -f  # Weekly
   ```

5. **Backup data before major changes**
   ```bash
   # Export database
   docker exec auction-system-postgres-1 pg_dump -U auctionuser auctiondb > backup.sql
   ```

---

**Last Updated:** October 22, 2025  
**Version:** 1.0.0

