# 📁 Image Storage Guide

Complete guide to where and how images are stored in the DOAS system.

---

## 📍 Storage Locations

### 1. **Physical Host Path** (Main Storage)

```
/var/lib/docker/volumes/auction-system_uploads_data/_data/
```

This is where Docker actually stores all uploaded images on your host system.

**Access:**
```bash
# List files (requires sudo)
sudo ls -lh /var/lib/docker/volumes/auction-system_uploads_data/_data/

# View a specific image
sudo cat /var/lib/docker/volumes/auction-system_uploads_data/_data/filename.jpg
```

---

### 2. **Inside Docker Containers**

All containers mount the same volume at different paths:

#### Nginx Container:
```
Path: /app/uploads/
Purpose: Serves images via HTTP
Access: http://localhost/uploads/filename.jpg
```

#### Backend Servers (1, 2, 3):
```
Path: /app/uploads/
Purpose: Write uploaded files
All servers share the SAME volume
```

**Access from container:**
```bash
# List files in Nginx
docker exec auction-system-nginx-1 ls -lh /app/uploads/

# List files in Backend Server 1
docker exec auction-system-auction-server-1-1 ls -lh /app/uploads/

# View a file
docker exec auction-system-nginx-1 cat /app/uploads/filename.jpg
```

---

### 3. **Docker Volume**

```
Volume Name: auction-system_uploads_data
Type: Docker named volume
Driver: local
```

**Inspect volume:**
```bash
# Get detailed info
docker volume inspect auction-system_uploads_data

# Get just the mountpoint
docker volume inspect auction-system_uploads_data --format '{{.Mountpoint}}'
```

---

## 🗂️ Directory Structure

```
Host System:
└── /var/lib/docker/volumes/auction-system_uploads_data/_data/
    ├── 7ae941b1-b4fb-4408-be76-7382520673bb.jpg
    ├── afcc8fbc-aac0-44e7-ab59-fb7c92e06a37.jpg
    ├── b3308a4c-5d3f-4d25-9806-167e0da421a6.jpeg
    └── fde2e815-b895-4602-9e2f-66391cae24d2.jpg

Inside Containers:
├── Nginx:           /app/uploads/  → Serves via HTTP
├── Backend Server 1: /app/uploads/  → Writes files
├── Backend Server 2: /app/uploads/  → Writes files
└── Backend Server 3: /app/uploads/  → Writes files

All point to the SAME physical location!
```

---

## 🌐 Access Methods

### 1. Via Web Browser (Public)

```
http://localhost/uploads/filename.jpg
http://localhost/uploads/b3308a4c-5d3f-4d25-9806-167e0da421a6.jpeg
```

This is how your frontend displays images:
```jsx
<img src="/uploads/uuid.jpg" alt="Auction item" />
```

---

### 2. Via Direct File System (Root Access Required)

```bash
# List all images
sudo ls -lh /var/lib/docker/volumes/auction-system_uploads_data/_data/

# Copy an image to current directory
sudo cp /var/lib/docker/volumes/auction-system_uploads_data/_data/image.jpg ./

# View image
sudo eog /var/lib/docker/volumes/auction-system_uploads_data/_data/image.jpg
```

---

### 3. Via Docker Commands (No Root Required)

```bash
# List files
docker run --rm -v auction-system_uploads_data:/data alpine ls -lh /data

# Copy file out
docker run --rm -v auction-system_uploads_data:/data \
  -v $(pwd):/backup alpine cp /data/image.jpg /backup/

# View file info
docker run --rm -v auction-system_uploads_data:/data alpine stat /data/image.jpg
```

---

### 4. Via Container Exec

```bash
# List from Nginx container
docker exec auction-system-nginx-1 ls /app/uploads/

# Copy from container to host
docker cp auction-system-nginx-1:/app/uploads/image.jpg ./local-image.jpg
```

---

## 💾 Backup & Restore

### Backup All Images

```bash
# Create compressed backup
docker run --rm -v auction-system_uploads_data:/data \
  -v $(pwd):/backup alpine \
  tar czf /backup/uploads-backup-$(date +%Y%m%d).tar.gz -C /data .

# Verify backup
tar tzf uploads-backup-*.tar.gz
```

### Restore Images

```bash
# Restore from backup
docker run --rm -v auction-system_uploads_data:/data \
  -v $(pwd):/backup alpine \
  tar xzf /backup/uploads-backup-20251021.tar.gz -C /data

# Restart services to apply
docker-compose restart
```

---

### Copy All Images to Local Directory

```bash
# Create local copy
docker cp auction-system-nginx-1:/app/uploads/ ./images-backup/

# Or using volume mount
docker run --rm -v auction-system_uploads_data:/data \
  -v $(pwd):/backup alpine \
  cp -r /data/. /backup/uploads-copy/
```

---

## 🔍 Finding Specific Images

### By Filename

```bash
# Search for specific file
docker run --rm -v auction-system_uploads_data:/data alpine \
  find /data -name "*.jpg"

# Find recently uploaded (last 24 hours)
docker run --rm -v auction-system_uploads_data:/data alpine \
  find /data -type f -mtime -1
```

### By Size

```bash
# Find large files (>1MB)
docker run --rm -v auction-system_uploads_data:/data alpine \
  find /data -type f -size +1M

# Sort by size
docker run --rm -v auction-system_uploads_data:/data alpine \
  ls -lhS /data
```

---

## 📊 Storage Statistics

### Get Volume Size

```bash
# Total volume size
docker run --rm -v auction-system_uploads_data:/data alpine \
  du -sh /data

# Detailed breakdown
docker run --rm -v auction-system_uploads_data:/data alpine \
  du -h /data/*
```

### Count Files

```bash
# Total files
docker run --rm -v auction-system_uploads_data:/data alpine \
  sh -c "ls -1 /data | wc -l"

# By extension
docker run --rm -v auction-system_uploads_data:/data alpine \
  sh -c "ls -1 /data/*.jpg | wc -l"
```

---

## 🔐 Permissions

### Current Permissions

```bash
# Check permissions
docker run --rm -v auction-system_uploads_data:/data alpine \
  ls -la /data
```

Default: `root:root` with `644` permissions

### Fix Permissions (If Needed)

```bash
# Make all files readable
docker run --rm -v auction-system_uploads_data:/data alpine \
  chmod -R 644 /data/*

# Fix ownership (if needed)
docker run --rm -v auction-system_uploads_data:/data alpine \
  chown -R root:root /data
```

---

## 🗑️ Cleanup

### Delete Specific Images

```bash
# Delete single file
docker run --rm -v auction-system_uploads_data:/data alpine \
  rm /data/filename.jpg

# Delete old files (older than 30 days)
docker run --rm -v auction-system_uploads_data:/data alpine \
  find /data -type f -mtime +30 -delete
```

### Clear All Images

```bash
# WARNING: This deletes ALL uploaded images!
docker run --rm -v auction-system_uploads_data:/data alpine \
  sh -c "rm -rf /data/*"

# Or remove entire volume
docker-compose down
docker volume rm auction-system_uploads_data
docker-compose up -d
```

---

## ⚠️ Important Notes

### Persistence

✅ **Images persist through:**
- Container restarts
- `docker-compose restart`
- `docker-compose down` + `docker-compose up`
- System reboots

❌ **Images are LOST if:**
- You run `docker-compose down -v` (removes volumes)
- You manually delete the volume: `docker volume rm auction-system_uploads_data`
- You delete the physical directory (requires root)

### Storage Location

- **DO NOT** manually edit files in `/var/lib/docker/volumes/` - use Docker commands
- The volume is managed by Docker and may change location
- Always use the volume name, not the physical path

### Shared Volume

- All backend servers write to the same volume
- Nginx reads from the same volume
- This ensures consistency across all containers
- No file replication needed

---

## 🚀 Quick Commands

```bash
# List all images
docker exec auction-system-nginx-1 ls -lh /app/uploads/

# Count images
docker exec auction-system-nginx-1 sh -c "ls -1 /app/uploads/ | wc -l"

# Backup images
docker run --rm -v auction-system_uploads_data:/data \
  -v $(pwd):/backup alpine \
  tar czf /backup/uploads-backup.tar.gz -C /data .

# Copy to local
docker cp auction-system-nginx-1:/app/uploads/ ./uploads-copy/

# Get volume path
docker volume inspect auction-system_uploads_data --format '{{.Mountpoint}}'

# Access via web
curl http://localhost/uploads/filename.jpg
```

---

## 📖 Related Documentation

- See `docker-compose.yml` for volume configuration
- See `nginx/nginx.conf` for serving configuration
- See `backend/src/.../FileController.java` for upload logic
- See `TROUBLESHOOTING.md` for common issues

---

**Last Updated:** October 22, 2025  
**Volume Name:** `auction-system_uploads_data`  
**Physical Path:** `/var/lib/docker/volumes/auction-system_uploads_data/_data`

