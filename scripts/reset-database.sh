#!/bin/bash

# DOAS Database Reset Script
# This script clears all data from the database while keeping the structure

set -e

echo "╔═══════════════════════════════════════════════════════════════╗"
echo "║                                                               ║"
echo "║         🗄️  DOAS DATABASE RESET SCRIPT                       ║"
echo "║                                                               ║"
echo "╚═══════════════════════════════════════════════════════════════╝"
echo ""

# Check if PostgreSQL container is running
if ! docker ps | grep -q auction-system-postgres-1; then
    echo "❌ PostgreSQL container is not running!"
    echo "   Please start the services first: docker-compose up -d"
    exit 1
fi

echo "⚠️  WARNING: This will delete ALL data from the database!"
echo ""
echo "Tables that will be cleared:"
echo "  • bids"
echo "  • auctions"
echo "  • users"
echo ""
read -p "Are you sure you want to continue? (yes/no): " confirm

if [ "$confirm" != "yes" ]; then
    echo "❌ Aborted."
    exit 0
fi

echo ""
echo "🔄 Clearing database tables..."

# Truncate all tables
docker exec auction-system-postgres-1 psql -U auctionuser -d auctiondb -c "TRUNCATE TABLE bids CASCADE;" > /dev/null 2>&1
echo "✅ Cleared bids table"

docker exec auction-system-postgres-1 psql -U auctionuser -d auctiondb -c "TRUNCATE TABLE auctions CASCADE;" > /dev/null 2>&1
echo "✅ Cleared auctions table"

docker exec auction-system-postgres-1 psql -U auctionuser -d auctiondb -c "TRUNCATE TABLE users CASCADE;" > /dev/null 2>&1
echo "✅ Cleared users table"

echo ""
echo "🎉 Database reset complete!"
echo ""
echo "📊 Current row counts:"
docker exec auction-system-postgres-1 psql -U auctionuser -d auctiondb -c "SELECT 'users' as table_name, COUNT(*) as count FROM users UNION ALL SELECT 'auctions', COUNT(*) FROM auctions UNION ALL SELECT 'bids', COUNT(*) FROM bids;" 

echo ""
echo "💡 Tip: Restart the backend servers to regenerate sample data:"
echo "   docker-compose restart auction-server-1 auction-server-2 auction-server-3"


