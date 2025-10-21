#!/bin/bash

# Build script for Distributed Auction System

set -e

echo "🏗️  Building Distributed Auction System..."

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
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

# Check prerequisites
check_prerequisites() {
    print_status "Checking prerequisites..."
    
    if ! command -v docker &> /dev/null; then
        print_error "Docker is not installed. Please install Docker first."
        exit 1
    fi
    
    if ! command -v docker-compose &> /dev/null; then
        print_error "Docker Compose is not installed. Please install Docker Compose first."
        exit 1
    fi
    
    if ! command -v java &> /dev/null; then
        print_error "Java 17+ is not installed. Please install Java 17+ first."
        exit 1
    fi
    
    if ! command -v node &> /dev/null; then
        print_error "Node.js is not installed. Please install Node.js 16+ first."
        exit 1
    fi
    
    print_status "All prerequisites are installed ✅"
}

# Build backend
build_backend() {
    print_status "Building Spring Boot backend..."
    
    cd backend
    
    # Check if Maven wrapper exists
    if [ ! -f "./mvnw" ]; then
        print_warning "Maven wrapper not found, using system Maven..."
        if ! command -v mvn &> /dev/null; then
            print_error "Maven is not installed. Please install Maven or use the Maven wrapper."
            exit 1
        fi
        mvn clean package -DskipTests
    else
        ./mvnw clean package -DskipTests
    fi
    
    # Check if JAR was created
    if [ ! -f "target/distributed-auction-system-1.0.0.jar" ]; then
        print_error "Backend JAR file was not created successfully."
        exit 1
    fi
    
    cd ..
    print_status "Backend built successfully ✅"
}

# Build frontend
build_frontend() {
    print_status "Building React frontend..."
    
    cd frontend
    
    # Install dependencies
    if [ ! -d "node_modules" ]; then
        print_status "Installing frontend dependencies..."
        npm install
    fi
    
    # Build for production
    npm run build
    
    # Check if build was successful
    if [ ! -d "build" ]; then
        print_error "Frontend build directory was not created."
        exit 1
    fi
    
    cd ..
    print_status "Frontend built successfully ✅"
}

# Build Docker images
build_docker_images() {
    print_status "Building Docker images..."
    
    # Build backend image
    docker build -t auction-backend:latest ./backend
    
    # Build frontend image (if needed)
    # docker build -t auction-frontend:latest ./frontend
    
    print_status "Docker images built successfully ✅"
}

# Create necessary directories
create_directories() {
    print_status "Creating necessary directories..."
    
    mkdir -p logs
    mkdir -p data/postgres
    mkdir -p data/redis
    
    print_status "Directories created ✅"
}

# Validate configuration
validate_config() {
    print_status "Validating configuration..."
    
    # Check Docker Compose file
    if [ ! -f "docker-compose.yml" ]; then
        print_error "docker-compose.yml not found."
        exit 1
    fi
    
    # Check Nginx configuration
    if [ ! -f "nginx/nginx.conf" ]; then
        print_error "Nginx configuration not found."
        exit 1
    fi
    
    # Check database initialization script
    if [ ! -f "database/init.sql" ]; then
        print_error "Database initialization script not found."
        exit 1
    fi
    
    print_status "Configuration validated ✅"
}

# Main build process
main() {
    print_status "Starting build process..."
    
    check_prerequisites
    validate_config
    create_directories
    build_backend
    build_frontend
    build_docker_images
    
    print_status "🎉 Build completed successfully!"
    print_status "You can now start the system with: docker-compose up -d"
    print_status "Access the application at: http://localhost"
    print_status "Access the admin dashboard at: http://localhost/admin"
}

# Handle script arguments
case "${1:-}" in
    "backend-only")
        check_prerequisites
        validate_config
        build_backend
        print_status "Backend build completed!"
        ;;
    "frontend-only")
        check_prerequisites
        build_frontend
        print_status "Frontend build completed!"
        ;;
    "docker-only")
        check_prerequisites
        validate_config
        build_docker_images
        print_status "Docker images build completed!"
        ;;
    "clean")
        print_status "Cleaning up..."
        rm -rf backend/target
        rm -rf frontend/build
        rm -rf frontend/node_modules
        docker system prune -f
        print_status "Cleanup completed!"
        ;;
    "help"|"-h"|"--help")
        echo "Usage: $0 [backend-only|frontend-only|docker-only|clean|help]"
        echo ""
        echo "Options:"
        echo "  backend-only   - Build only the Spring Boot backend"
        echo "  frontend-only  - Build only the React frontend"
        echo "  docker-only    - Build only the Docker images"
        echo "  clean          - Clean build artifacts and Docker cache"
        echo "  help           - Show this help message"
        echo ""
        echo "Default: Build all components (backend, frontend, Docker images)"
        ;;
    "")
        main
        ;;
    *)
        print_error "Unknown option: $1"
        echo "Use '$0 help' for usage information."
        exit 1
        ;;
esac