#!/bin/bash

# ============================================
# DOAS - Application Status Script
# ============================================

echo "╔═══════════════════════════════════════════════════════════════╗"
echo "║                                                               ║"
echo "║          📊 DOAS Application Status                          ║"
echo "║                                                               ║"
echo "╚═══════════════════════════════════════════════════════════════╝"
echo ""

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

# Get the directory where the script is located
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
PROJECT_ROOT="$( cd "$SCRIPT_DIR/.." && pwd )"

cd "$PROJECT_ROOT"

# Check if docker-compose is installed
if ! command -v docker-compose &> /dev/null; then
    echo -e "${RED}✗ docker-compose is not installed!${NC}"
    exit 1
fi

echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo -e "${CYAN}🐳 Docker Containers Status${NC}"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""

docker-compose ps

echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo -e "${CYAN}🌐 Access URLs${NC}"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""
echo -e "${GREEN}Frontend:${NC}"
echo "  🌍 Main Application:    http://localhost"
echo "  👨‍💼 Admin Dashboard:     http://localhost/admin"
echo "  📚 API:                 http://localhost/api"
echo ""
echo -e "${GREEN}Backend Servers:${NC}"
echo "  🖥️  Server 1 (RMI 1101):  http://localhost:8081"
echo "  🖥️  Server 2 (RMI 1102):  http://localhost:8082"
echo "  🖥️  Server 3 (RMI 1103):  http://localhost:8083"
echo ""
echo -e "${GREEN}Infrastructure:${NC}"
echo "  🗄️  PostgreSQL:          localhost:5432"
echo "  📦 Redis:               localhost:6379"
echo ""

# Check health status
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo -e "${CYAN}🏥 Health Check${NC}"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""

HEALTHY=0
UNHEALTHY=0

# Check backend servers
for server in 1 2 3; do
    port=$((8080 + server))
    if curl -s -f http://localhost:$port/api/auctions/status > /dev/null 2>&1; then
        echo -e "  ${GREEN}✓${NC} Server $server (port $port) - Healthy"
        HEALTHY=$((HEALTHY + 1))
    else
        echo -e "  ${RED}✗${NC} Server $server (port $port) - Not responding"
        UNHEALTHY=$((UNHEALTHY + 1))
    fi
done

# Check frontend/Nginx
if curl -s -f http://localhost > /dev/null 2>&1; then
    echo -e "  ${GREEN}✓${NC} Frontend/Nginx - Healthy"
    HEALTHY=$((HEALTHY + 1))
else
    echo -e "  ${RED}✗${NC} Frontend/Nginx - Not responding"
    UNHEALTHY=$((UNHEALTHY + 1))
fi

# Check database
if docker-compose exec -T postgres pg_isready -U auctionuser -d auctiondb > /dev/null 2>&1; then
    echo -e "  ${GREEN}✓${NC} PostgreSQL - Healthy"
    HEALTHY=$((HEALTHY + 1))
else
    echo -e "  ${RED}✗${NC} PostgreSQL - Not responding"
    UNHEALTHY=$((UNHEALTHY + 1))
fi

# Check Redis
if docker-compose exec -T redis redis-cli ping > /dev/null 2>&1; then
    echo -e "  ${GREEN}✓${NC} Redis - Healthy"
    HEALTHY=$((HEALTHY + 1))
else
    echo -e "  ${RED}✗${NC} Redis - Not responding"
    UNHEALTHY=$((UNHEALTHY + 1))
fi

echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo -e "${CYAN}📈 Summary${NC}"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""
echo -e "  ${GREEN}Healthy:${NC}   $HEALTHY"
echo -e "  ${RED}Unhealthy:${NC} $UNHEALTHY"
echo ""

if [ $UNHEALTHY -eq 0 ]; then
    echo -e "  ${GREEN}✓ All services are running properly!${NC}"
else
    echo -e "  ${YELLOW}⚠ Some services are not responding${NC}"
    echo ""
    echo "  Try restarting the application:"
    echo "    ./scripts/restart.sh"
fi

echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo -e "${CYAN}💡 Quick Commands${NC}"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""
echo "  View logs:      docker-compose logs -f"
echo "  Restart:        ./scripts/restart.sh"
echo "  Stop:           ./scripts/stop.sh"
echo "  Database:       docker exec -it auction-system-postgres-1 psql -U auctionuser -d auctiondb"
echo "  Redis:          docker exec -it auction-system-redis-1 redis-cli"
echo ""

