#!/bin/bash

# ============================================
# DOAS - Stop Application Script
# ============================================

echo "╔═══════════════════════════════════════════════════════════════╗"
echo "║                                                               ║"
echo "║          🛑 Stopping DOAS Application                        ║"
echo "║                                                               ║"
echo "╚═══════════════════════════════════════════════════════════════╝"
echo ""

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Get the directory where the script is located
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
PROJECT_ROOT="$( cd "$SCRIPT_DIR/.." && pwd )"

cd "$PROJECT_ROOT"

echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "📍 Project Directory: $PROJECT_ROOT"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""

# Check if docker-compose is installed
if ! command -v docker-compose &> /dev/null; then
    echo -e "${RED}✗ docker-compose is not installed!${NC}"
    echo "Please install Docker Compose first."
    exit 1
fi

# Check if containers are running
RUNNING_CONTAINERS=$(docker-compose ps -q 2>/dev/null)

if [ -z "$RUNNING_CONTAINERS" ]; then
    echo -e "${YELLOW}⚠ No running containers found${NC}"
    echo "Application is already stopped or was not started with docker-compose."
    echo ""
    
    # Check for any manually started containers
    MANUAL_CONTAINERS=$(docker ps --filter "name=auction-system" -q)
    if [ ! -z "$MANUAL_CONTAINERS" ]; then
        echo -e "${BLUE}ℹ Found manually started containers${NC}"
        echo "Stopping them now..."
        docker stop $MANUAL_CONTAINERS
        echo -e "${GREEN}✓ Stopped manually started containers${NC}"
    fi
else
    echo -e "${BLUE}Stopping all services...${NC}"
    echo ""
    
    # Stop and remove containers
    docker-compose down
    
    if [ $? -eq 0 ]; then
        echo ""
        echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
        echo -e "${GREEN}✓ All services stopped successfully!${NC}"
        echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
    else
        echo ""
        echo -e "${RED}✗ Error stopping services${NC}"
        exit 1
    fi
fi

echo ""
echo "📊 Current Status:"
RUNNING=$(docker ps --filter "name=auction-system" --format "{{.Names}}" | wc -l)
echo "   Running containers: $RUNNING"

if [ $RUNNING -eq 0 ]; then
    echo -e "   Status: ${GREEN}All stopped${NC}"
else
    echo -e "   Status: ${YELLOW}Some containers still running${NC}"
    echo ""
    echo "Running containers:"
    docker ps --filter "name=auction-system" --format "table {{.Names}}\t{{.Status}}"
fi

echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "💡 To start the application again, run:"
echo "   ./scripts/start.sh"
echo ""
echo "💡 To stop and remove all data (volumes), run:"
echo "   docker-compose down -v"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""

