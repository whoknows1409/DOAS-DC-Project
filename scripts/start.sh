#!/bin/bash

# Start script for Distributed Auction System

set -e

echo "🚀 Starting Distributed Auction System..."

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Function to print colored output
print_status() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

print_header() {
    echo -e "${BLUE}$1${NC}"
}

# Check if system is already running
check_running() {
    if docker-compose ps | grep -q "Up"; then
        print_warning "System is already running!"
        echo "Current status:"
        docker-compose ps
        return 0
    fi
    return 1
}

# Start the system
start_system() {
    print_header "🐳 Starting Docker containers..."
    
    # Start database and Redis first
    print_status "Starting PostgreSQL and Redis..."
    docker-compose up -d postgres redis
    
    # Wait for database to be ready
    print_status "Waiting for database to be ready..."
    sleep 10
    
    # Check database connection
    for i in {1..30}; do
        if docker-compose exec -T postgres pg_isready -U auctionuser -d auctiondb; then
            print_status "Database is ready!"
            break
        fi
        if [ $i -eq 30 ]; then
            print_error "Database failed to start properly"
            exit 1
        fi
        sleep 2
    done
    
    # Start backend servers
    print_status "Starting backend servers..."
    docker-compose up -d auction-server-1 auction-server-2 auction-server-3
    
    # Wait for backend servers to be ready
    print_status "Waiting for backend servers to be ready..."
    sleep 15
    
    # Check backend health
    for server in 1 2 3; do
        port=$((8080 + server))
        for i in {1..30}; do
            if curl -f http://localhost:$port/api/auctions/status &>/dev/null; then
                print_status "Server $server is ready!"
                break
            fi
            if [ $i -eq 30 ]; then
                print_warning "Server $server might not be fully ready yet"
            fi
            sleep 2
        done
    done
    
    # Start Nginx and frontend
    print_status "Starting Nginx load balancer and frontend..."
    docker-compose up -d nginx
    
    # Wait a bit for Nginx to start
    sleep 5
    
    print_status "🎉 System started successfully!"
}

# Show system status
show_status() {
    print_header "📊 System Status"
    docker-compose ps
    
    echo ""
    print_header "🌐 Access URLs"
    echo "Frontend:           http://localhost"
    echo "Admin Dashboard:    http://localhost/admin"
    echo "API Documentation:  http://localhost/api"
    echo ""
    print_header "🔧 Backend Servers"
    echo "Server 1:           http://localhost:8081"
    echo "Server 2:           http://localhost:8082"
    echo "Server 3:           http://localhost:8083"
    echo ""
    print_header "🗄️  Database Access"
    echo "PostgreSQL:         localhost:5432"
    echo "Redis:              localhost:6379"
}

# Show logs
show_logs() {
    print_header "📋 Showing logs..."
    docker-compose logs -f
}

# Stop the system
stop_system() {
    print_header "🛑 Stopping Distributed Auction System..."
    docker-compose down
    print_status "System stopped successfully!"
}

# Stop and remove all data
clean_system() {
    print_header "🧹 Cleaning up Distributed Auction System..."
    docker-compose down -v --remove-orphans
    docker system prune -f
    print_status "System cleaned successfully!"
}

# Restart the system
restart_system() {
    print_header "🔄 Restarting Distributed Auction System..."
    stop_system
    sleep 5
    start_system
    show_status
}

# Health check
health_check() {
    print_header "🏥 Performing health check..."
    
    # Check Docker containers
    print_status "Checking Docker containers..."
    if ! docker-compose ps | grep -q "Up"; then
        print_error "Some containers are not running!"
        docker-compose ps
        return 1
    fi
    
    # Check backend servers
    print_status "Checking backend servers..."
    for server in 1 2 3; do
        port=$((8080 + server))
        if curl -f http://localhost:$port/api/auctions/status &>/dev/null; then
            print_status "✅ Server $server (port $port) is healthy"
        else
            print_error "❌ Server $server (port $port) is not responding"
        fi
    done
    
    # Check Nginx
    print_status "Checking Nginx load balancer..."
    if curl -f http://localhost/health &>/dev/null; then
        print_status "✅ Nginx is healthy"
    else
        print_error "❌ Nginx is not responding"
    fi
    
    # Check database
    print_status "Checking database connection..."
    if docker-compose exec -T postgres pg_isready -U auctionuser -d auctiondb &>/dev/null; then
        print_status "✅ PostgreSQL is healthy"
    else
        print_error "❌ PostgreSQL is not responding"
    fi
    
    # Check Redis
    print_status "Checking Redis connection..."
    if docker-compose exec -T redis redis-cli ping &>/dev/null; then
        print_status "✅ Redis is healthy"
    else
        print_error "❌ Redis is not responding"
    fi
    
    print_status "Health check completed!"
}

# Main script logic
case "${1:-start}" in
    "start")
        if ! check_running; then
            start_system
            show_status
        fi
        ;;
    "stop")
        stop_system
        ;;
    "restart")
        restart_system
        ;;
    "status")
        show_status
        ;;
    "logs")
        show_logs
        ;;
    "clean")
        clean_system
        ;;
    "health")
        health_check
        ;;
    "help"|"-h"|"--help")
        echo "Usage: $0 [start|stop|restart|status|logs|clean|health|help]"
        echo ""
        echo "Commands:"
        echo "  start    - Start the distributed auction system (default)"
        echo "  stop     - Stop all containers"
        echo "  restart  - Restart the system"
        echo "  status   - Show system status and access URLs"
        echo "  logs     - Show and follow container logs"
        echo "  clean    - Stop and remove all containers and data"
        echo "  health   - Perform health check on all components"
        echo "  help     - Show this help message"
        echo ""
        echo "Examples:"
        echo "  $0                # Start the system"
        echo "  $0 start          # Start the system"
        echo "  $0 status         # Show status"
        echo "  $0 logs           # View logs"
        echo "  $0 health         # Health check"
        ;;
    *)
        print_error "Unknown command: $1"
        echo "Use '$0 help' for usage information."
        exit 1
        ;;
esac