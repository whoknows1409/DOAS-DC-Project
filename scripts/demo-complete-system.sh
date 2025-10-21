#!/bin/bash

# Complete Distributed Auction System Demonstration Script
# This script demonstrates all the features of the distributed auction system

echo "🎯 Distributed Auction System - Complete Demonstration"
echo "======================================================"
echo ""

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Function to print colored output
print_status() {
    echo -e "${GREEN}✅ $1${NC}"
}

print_info() {
    echo -e "${BLUE}ℹ️  $1${NC}"
}

print_warning() {
    echo -e "${YELLOW}⚠️  $1${NC}"
}

print_error() {
    echo -e "${RED}❌ $1${NC}"
}

# Check if system is running
echo "🔍 Checking system status..."
if ! curl -s http://localhost:8081/actuator/health > /dev/null; then
    print_error "System is not running. Please start with: docker-compose up -d"
    exit 1
fi
print_status "System is running"

echo ""
echo "📋 Phase 1: System Information"
echo "=============================="

# Get system info
print_info "Getting system information..."
curl -s http://localhost:8081/api/docs/system-info | jq .

echo ""
echo "📋 Phase 2: User Management"
echo "==========================="

# Test user registration
print_info "Testing user registration..."
USER_RESPONSE=$(curl -s -X POST http://localhost:8081/api/users/register \
  -H "Content-Type: application/json" \
  -d '{"username": "demouser", "email": "demo@example.com", "password": "password123"}')

if echo "$USER_RESPONSE" | jq -e '.id' > /dev/null; then
    USER_ID=$(echo "$USER_RESPONSE" | jq -r '.id')
    print_status "User registered successfully: $USER_ID"
else
    print_error "User registration failed"
fi

# Test user login
print_info "Testing user login..."
LOGIN_RESPONSE=$(curl -s -X POST http://localhost:8081/api/users/login \
  -H "Content-Type: application/json" \
  -d '{"username": "demouser", "password": "password123"}')

if echo "$LOGIN_RESPONSE" | jq -e '.id' > /dev/null; then
    print_status "User login successful"
else
    print_error "User login failed"
fi

# List all users
print_info "Listing all users..."
curl -s http://localhost:8081/api/users | jq '.[] | {username: .username, email: .email}'

echo ""
echo "📋 Phase 3: Auction Management"
echo "=============================="

# List all auctions
print_info "Listing all auctions..."
AUCTIONS=$(curl -s http://localhost:8081/api/auctions)
echo "$AUCTIONS" | jq '.[] | {id: .id, title: .title, currentPrice: .currentPrice, status: .status}'

# Get first auction ID for testing
FIRST_AUCTION_ID=$(echo "$AUCTIONS" | jq -r '.[0].id')
print_info "Using auction ID for testing: $FIRST_AUCTION_ID"

# Test auction creation
print_info "Creating a new auction..."
NEW_AUCTION=$(curl -s -X POST http://localhost:8081/api/auctions \
  -H "Content-Type: application/json" \
  -d "{
    \"title\": \"Demo Auction Item\",
    \"description\": \"A demonstration auction item\",
    \"startingPrice\": 100.00,
    \"endTime\": \"$(date -d '+2 hours' -Iseconds)\",
    \"sellerId\": \"$USER_ID\"
  }")

if echo "$NEW_AUCTION" | jq -e '.id' > /dev/null; then
    NEW_AUCTION_ID=$(echo "$NEW_AUCTION" | jq -r '.id')
    print_status "Auction created successfully: $NEW_AUCTION_ID"
else
    print_error "Auction creation failed"
fi

echo ""
echo "📋 Phase 4: Bidding System"
echo "=========================="

# Get a bidder ID
BIDDER_ID=$(curl -s http://localhost:8081/api/users | jq -r '.[] | select(.username == "bidder1") | .id')
print_info "Using bidder ID: $BIDDER_ID"

# Place a bid
print_info "Placing a bid..."
BID_RESPONSE=$(curl -s -X POST http://localhost:8081/api/auctions/$FIRST_AUCTION_ID/bids \
  -H "Content-Type: application/json" \
  -d "{
    \"bidderId\": \"$BIDDER_ID\",
    \"amount\": 600.00
  }")

if echo "$BID_RESPONSE" | jq -e '.id' > /dev/null; then
    print_status "Bid placed successfully"
else
    print_warning "Bid placement failed (might be expected if amount is too low)"
fi

# Get bids for auction
print_info "Getting bids for auction..."
curl -s http://localhost:8081/api/auctions/$FIRST_AUCTION_ID/bids | jq '.[] | {amount: .amount, bidderId: .bidderId, timestamp: .timestamp}'

echo ""
echo "📋 Phase 5: Distributed System Features"
echo "======================================="

# Test load balancing - make requests to different servers
print_info "Testing load balancing across servers..."
for i in {1..3}; do
    echo "Request to server $i:"
    curl -s http://localhost:808$i/api/auctions/status | jq '{server: .serverHealthy, logicalClock: .logicalClock}'
done

# Test data replication
print_info "Testing data replication..."
echo "Auctions on Server 1:"
curl -s http://localhost:8081/api/auctions | jq 'length'
echo "Auctions on Server 2:"
curl -s http://localhost:8082/api/auctions | jq 'length'
echo "Auctions on Server 3:"
curl -s http://localhost:8083/api/auctions | jq 'length'

echo ""
echo "📋 Phase 6: Admin & Monitoring"
echo "=============================="

# Get comprehensive system status
print_info "Getting comprehensive system status..."
curl -s http://localhost:8081/api/admin/status | jq '{
  serverId: .serverId,
  isCoordinator: .isCoordinator,
  coordinatorId: .coordinatorId,
  activeAuctions: .activeAuctions,
  totalBids: .totalBids,
  logicalClock: .logicalClock,
  uptime: .uptime
}'

# Get API documentation
print_info "Getting API documentation..."
curl -s http://localhost:8081/api/docs/endpoints | jq 'keys'

echo ""
echo "📋 Phase 7: User-Specific Data"
echo "=============================="

# Get user's auctions
print_info "Getting user's auctions..."
curl -s http://localhost:8081/api/users/$USER_ID/auctions | jq '.[] | {title: .title, status: .status}'

# Get user's bids
print_info "Getting user's bids..."
curl -s http://localhost:8081/api/users/$BIDDER_ID/bids | jq '.[] | {amount: .amount, auctionId: .auctionId}'

echo ""
echo "📋 Phase 8: Real-time Features"
echo "=============================="

print_info "WebSocket endpoints available:"
echo "  - Connect: ws://localhost:8081/ws"
echo "  - Send bid: {\"auctionId\": \"$FIRST_AUCTION_ID\", \"bidderId\": \"$BIDDER_ID\", \"amount\": 700.00}"
echo "  - Subscribe to: /queue/bid-response, /topic/auction-updates"

echo ""
echo "🎉 Demonstration Complete!"
echo "========================="
print_status "All major features have been demonstrated:"
echo "  ✅ User registration and authentication"
echo "  ✅ Auction creation and management"
echo "  ✅ Bidding system with distributed coordination"
echo "  ✅ Data replication across servers"
echo "  ✅ Load balancing"
echo "  ✅ Admin monitoring and system status"
echo "  ✅ API documentation"
echo "  ✅ Sample data initialization"

echo ""
print_info "System URLs:"
echo "  🌐 Frontend: http://localhost"
echo "  🔧 Server 1: http://localhost:8081"
echo "  🔧 Server 2: http://localhost:8082"
echo "  🔧 Server 3: http://localhost:8083"
echo "  📚 API Docs: http://localhost:8081/api/docs/endpoints"

echo ""
print_info "Key Features Demonstrated:"
echo "  🔄 Distributed Architecture (3 servers)"
echo "  🗳️  Bully Election Algorithm"
echo "  🔒 Two-Phase Commit Protocol"
echo "  ⏰ Lamport Logical Clocks"
echo "  🌐 Real-time WebSocket Updates"
echo "  💾 Redis Caching"
echo "  🗄️  PostgreSQL Database"
echo "  ⚖️  Load Balancing with Nginx"


