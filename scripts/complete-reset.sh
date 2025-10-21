#!/bin/bash

# DOAS Complete Reset Script
# This script performs a complete reset: database, Redis, and uploaded files

set -e

echo "╔═══════════════════════════════════════════════════════════════╗"
echo "║                                                               ║"
echo "║         🔥 DOAS COMPLETE RESET SCRIPT                        ║"
echo "║                                                               ║"
echo "╚═══════════════════════════════════════════════════════════════╝"
echo ""

echo "⚠️  WARNING: This will DELETE EVERYTHING:"
echo ""
echo "  • All database data (PostgreSQL volume)"
echo "  • All Redis cache data"
echo "  • All uploaded images and files"
echo ""
read -p "Are you absolutely sure? Type 'RESET' to confirm: " confirm

if [ "$confirm" != "RESET" ]; then
    echo "❌ Aborted."
    exit 0
fi

echo ""
echo "🛑 Stopping all services..."
docker-compose down

echo ""
echo "🗑️  Removing PostgreSQL volume..."
docker volume rm auction-system_postgres-data 2>/dev/null || echo "  (Volume already removed or doesn't exist)"

echo ""
echo "🗑️  Removing Redis volume..."
docker volume rm auction-system_redis-data 2>/dev/null || echo "  (Volume already removed or doesn't exist)"

echo ""
echo "🗑️  Clearing uploaded files..."
if [ -d "uploads" ]; then
    rm -rf uploads/*
    echo "✅ Cleared uploads directory"
else
    echo "  (Uploads directory doesn't exist)"
fi

echo ""
echo "🚀 Restarting services..."
docker-compose up -d

echo ""
echo "⏳ Waiting for services to initialize (30 seconds)..."
for i in {30..1}; do
    echo -ne "   $i seconds remaining...\r"
    sleep 1
done
echo "                              "

echo ""
echo "🎉 COMPLETE RESET SUCCESSFUL!"
echo ""
echo "✨ Your DOAS system is now fresh and clean!"
echo ""
echo "📝 Next steps:"
echo "  1. Visit http://localhost"
echo "  2. Register a new admin account"
echo "  3. Create auctions and start bidding!"
echo ""
echo "💡 The backend should have regenerated sample data automatically."


