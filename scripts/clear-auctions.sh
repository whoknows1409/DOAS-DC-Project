#!/bin/bash

# DOAS Clear Auctions Script
# This script clears only auctions and bids, keeping users intact

set -e

echo "╔═══════════════════════════════════════════════════════════════╗"
echo "║                                                               ║"
echo "║         🛒 CLEAR AUCTIONS & BIDS                             ║"
echo "║                                                               ║"
echo "╚═══════════════════════════════════════════════════════════════╝"
echo ""

# Check if PostgreSQL container is running
if ! docker ps | grep -q auction-system-postgres-1; then
    echo "❌ PostgreSQL container is not running!"
    echo "   Please start the services first: docker-compose up -d"
    exit 1
fi

echo "⚠️  This will delete:"
echo ""
echo "  • All bids"
echo "  • All auctions"
echo ""
echo "✅ This will keep:"
echo ""
echo "  • All users"
echo ""
read -p "Continue? (yes/no): " confirm

if [ "$confirm" != "yes" ]; then
    echo "❌ Aborted."
    exit 0
fi

echo ""
echo "🔄 Clearing auctions and bids..."

# Truncate tables
docker exec auction-system-postgres-1 psql -U auctionuser -d auctiondb -c "TRUNCATE TABLE bids CASCADE;" > /dev/null 2>&1
echo "✅ Cleared bids table"

docker exec auction-system-postgres-1 psql -U auctionuser -d auctiondb -c "TRUNCATE TABLE auctions CASCADE;" > /dev/null 2>&1
echo "✅ Cleared auctions table"

echo ""
echo "🎉 Auctions and bids cleared!"
echo ""
echo "📊 Current row counts:"
docker exec auction-system-postgres-1 psql -U auctionuser -d auctiondb -c "SELECT 'users' as table_name, COUNT(*) as count FROM users UNION ALL SELECT 'auctions', COUNT(*) FROM auctions UNION ALL SELECT 'bids', COUNT(*) FROM bids;"

echo ""
echo "💡 You can now create new auctions!"


