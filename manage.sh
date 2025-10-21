#!/bin/bash

# DOAS (Distributed Online Auction System) - Management Script
# Single script to manage the entire application

set -e

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
MAGENTA='\033[0;35m'
NC='\033[0m' # No Color

print_header() {
    echo ""
    echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
    echo -e "${CYAN}$1${NC}"
    echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
    echo ""
}

print_success() {
    echo -e "${GREEN}✅ $1${NC}"
}

print_error() {
    echo -e "${RED}❌ $1${NC}"
}

print_warning() {
    echo -e "${YELLOW}⚠️  $1${NC}"
}

print_info() {
    echo -e "${CYAN}ℹ️  $1${NC}"
}

print_step() {
    echo -e "${BLUE}▶ $1${NC}"
}

# Check if Docker is running
check_docker() {
    if ! docker info > /dev/null 2>&1; then
        print_error "Docker is not running!"
        echo "Please start Docker and try again."
        exit 1
    fi
}

# Check if running
check_running() {
    docker-compose ps | grep -q "Up" 2>/dev/null
}

# Start the system
start_system() {
    print_header "🚀 Starting DOAS - Distributed Online Auction System"
    
    check_docker
    
    if check_running; then
        print_warning "System is already running!"
        return 0
    fi
    
    print_step "Starting all services..."
    docker-compose up -d
    
    print_step "Waiting for services to initialize (30 seconds)..."
    sleep 30
    
    print_success "System started successfully!"
    echo ""
    show_access_urls
}

# Stop the system
stop_system() {
    print_header "🛑 Stopping DOAS"
    
    if ! check_running; then
        print_warning "System is not running"
        return 0
    fi
    
    print_step "Stopping all services..."
    docker-compose down
    
    print_success "System stopped successfully!"
}

# Restart the system
restart_system() {
    print_header "🔄 Restarting DOAS"
    
    stop_system
    sleep 5
    start_system
}

# Show status
show_status() {
    print_header "📊 System Status"
    
    docker-compose ps
    
    echo ""
    show_access_urls
    
    # Check health
    echo ""
    print_step "Health Check:"
    if curl -s http://localhost/api/health > /dev/null 2>&1; then
        print_success "Backend is healthy"
    else
        print_error "Backend is not responding"
    fi
    
    if curl -s http://localhost > /dev/null 2>&1; then
        print_success "Frontend is accessible"
    else
        print_error "Frontend is not accessible"
    fi
}

# Show access URLs
show_access_urls() {
    print_info "Access URLs:"
    echo "  Frontend:           http://localhost"
    echo "  API Docs:           http://localhost/api"
    echo "  Health Check:       http://localhost/api/health"
    echo ""
}

# Show logs
show_logs() {
    local service=$1
    
    if [ -z "$service" ]; then
        print_header "📜 Following all logs (Ctrl+C to exit)"
        docker-compose logs -f
    else
        print_header "📜 Following logs for: $service"
        docker-compose logs -f "$service"
    fi
}

# Build the system
build_system() {
    print_header "🔨 Building DOAS"
    
    check_docker
    
    # Build backend
    print_step "Building backend JAR..."
    cd backend
    mvn clean install -DskipTests
    cd ..
    print_success "Backend built successfully"
    
    # Build frontend
    print_step "Building React frontend..."
    cd frontend
    if [ ! -d "node_modules" ]; then
        print_step "Installing frontend dependencies..."
        npm install --legacy-peer-deps
    fi
    npm run build
    cd ..
    print_success "Frontend built successfully"
    
    # Build Docker images
    print_step "Building Docker images..."
    docker-compose build
    print_success "Docker images built successfully"
    
    print_success "Build completed!"
}

# Clean system (remove containers and volumes)
clean_system() {
    print_header "🧹 Cleaning DOAS"
    
    print_warning "This will remove all containers, volumes, and data!"
    read -p "Are you sure? (y/N): " confirm
    
    if [[ ! $confirm =~ ^[Yy]$ ]]; then
        print_info "Clean cancelled"
        return 0
    fi
    
    print_step "Stopping and removing containers..."
    docker-compose down -v
    
    print_step "Removing build artifacts..."
    rm -rf backend/target
    rm -rf frontend/build
    rm -rf frontend/node_modules
    
    print_success "Clean completed!"
}

# Reset database
reset_database() {
    print_header "🔄 Resetting Database"
    
    print_warning "This will delete all data (users, auctions, bids)!"
    read -p "Are you sure? (y/N): " confirm
    
    if [[ ! $confirm =~ ^[Yy]$ ]]; then
        print_info "Reset cancelled"
        return 0
    fi
    
    print_step "Restarting database..."
    docker-compose restart postgres
    sleep 10
    
    print_success "Database reset completed!"
}

# Health check
health_check() {
    print_header "🏥 Health Check"
    
    local all_healthy=true
    
    # Check Nginx
    print_step "Checking Nginx..."
    if curl -s http://localhost > /dev/null 2>&1; then
        print_success "Nginx: OK"
    else
        print_error "Nginx: FAILED"
        all_healthy=false
    fi
    
    # Check Backend
    print_step "Checking Backend API..."
    if curl -s http://localhost/api/health > /dev/null 2>&1; then
        print_success "Backend: OK"
    else
        print_error "Backend: FAILED"
        all_healthy=false
    fi
    
    # Check Database
    print_step "Checking PostgreSQL..."
    if docker exec auction-system-postgres-1 pg_isready -U auction_user > /dev/null 2>&1; then
        print_success "PostgreSQL: OK"
    else
        print_error "PostgreSQL: FAILED"
        all_healthy=false
    fi
    
    # Check Redis
    print_step "Checking Redis..."
    if docker exec auction-system-redis-1 redis-cli ping > /dev/null 2>&1; then
        print_success "Redis: OK"
    else
        print_error "Redis: FAILED"
        all_healthy=false
    fi
    
    echo ""
    if [ "$all_healthy" = true ]; then
        print_success "All services are healthy! ✨"
    else
        print_error "Some services are unhealthy. Check logs with: $0 logs"
    fi
}

# GitHub upload
github_upload() {
    print_header "🚀 GitHub Upload"
    
    # Check if git is installed
    if ! command -v git &> /dev/null; then
        print_error "Git is not installed!"
        echo "Install it with: sudo apt install git"
        exit 1
    fi
    
    # Check if already initialized
    if [ -d ".git" ]; then
        print_warning "Git repository already initialized"
        git status
        return 0
    fi
    
    # Get GitHub username
    read -p "Enter your GitHub username: " GITHUB_USERNAME
    if [ -z "$GITHUB_USERNAME" ]; then
        print_error "Username cannot be empty"
        return 1
    fi
    
    # Get repository name
    print_info "Suggested repository name: doas-auction-system"
    read -p "Enter repository name (or press Enter for default): " REPO_NAME
    REPO_NAME=${REPO_NAME:-doas-auction-system}
    
    echo ""
    print_header "📋 Summary"
    echo "GitHub Username: $GITHUB_USERNAME"
    echo "Repository Name: $REPO_NAME"
    echo "Repository URL:  https://github.com/${GITHUB_USERNAME}/${REPO_NAME}"
    echo ""
    
    print_warning "Make sure you have created the repository on GitHub first!"
    print_info "Go to: https://github.com/new"
    echo ""
    
    read -p "Have you created the repository on GitHub? (y/N): " CREATED
    if [[ ! $CREATED =~ ^[Yy]$ ]]; then
        print_info "Please create the repository first, then run this command again"
        return 0
    fi
    
    # Initialize and push
    print_step "Initializing git repository..."
    git init
    
    print_step "Adding all files..."
    git add .
    
    print_step "Creating initial commit..."
    git commit -m "Initial commit: DOAS - Distributed Online Auction System

Features:
- Java RMI for client-server communication
- Multithreading with ExecutorService
- Lamport Logical Clocks for synchronization
- Bully and Ring election algorithms
- Two-Phase Commit protocol
- Distributed locking with Redis
- Data replication across 3 backend servers
- Nginx load balancing (least connections)
- Real-time bidding with WebSocket
- React frontend with Ant Design
- Docker containerization
- PostgreSQL database with persistent volumes"
    
    print_step "Adding GitHub remote..."
    git remote add origin "https://github.com/${GITHUB_USERNAME}/${REPO_NAME}.git"
    
    print_step "Pushing to GitHub..."
    echo ""
    print_warning "You may need to enter your GitHub credentials:"
    print_info "Username: Your GitHub username"
    print_info "Password: Your Personal Access Token (NOT your password)"
    echo ""
    
    git branch -M main
    git push -u origin main
    
    print_success "Successfully pushed to GitHub!"
    echo ""
    print_info "Repository URL: https://github.com/${GITHUB_USERNAME}/${REPO_NAME}"
}

# Show help
show_help() {
    cat << EOF

╔═══════════════════════════════════════════════════════════════════════════════╗
║                                                                               ║
║           🏛️  DOAS - Distributed Online Auction System                       ║
║                                                                               ║
╚═══════════════════════════════════════════════════════════════════════════════╝

Usage: $0 [command]

MAIN COMMANDS:
  start             Start the entire system
  stop              Stop all services
  restart           Restart all services
  status            Show system status
  logs [service]    Show logs (all or specific service)
  
BUILD & SETUP:
  build             Build backend, frontend, and Docker images
  clean             Remove all containers, volumes, and build artifacts
  
MAINTENANCE:
  health            Run health checks on all services
  reset-db          Reset database (delete all data)
  
GITHUB:
  github            Interactive GitHub upload
  
HELP:
  help              Show this help message

EXAMPLES:
  $0 start          # Start the system
  $0 logs nginx     # Show nginx logs
  $0 health         # Check all services
  $0 build          # Build everything

SERVICES:
  - nginx           Load balancer and reverse proxy
  - auction-server-1, auction-server-2, auction-server-3
  - postgres        PostgreSQL database
  - redis           Redis cache

ACCESS URLS:
  Frontend:         http://localhost
  API Docs:         http://localhost/api
  Health Check:     http://localhost/api/health

For more information, see README.md

EOF
}

# Main command handler
case "${1:-help}" in
    "start")
        start_system
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
        show_logs "$2"
        ;;
    "build")
        build_system
        ;;
    "clean")
        clean_system
        ;;
    "reset-db")
        reset_database
        ;;
    "health")
        health_check
        ;;
    "github")
        github_upload
        ;;
    "help"|"-h"|"--help")
        show_help
        ;;
    *)
        print_error "Unknown command: $1"
        echo "Use '$0 help' for usage information."
        exit 1
        ;;
esac

