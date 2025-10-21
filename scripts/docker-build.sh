#!/bin/bash

# Docker-based build script for environments without Java/Maven

set -e

echo "🐳 Building Distributed Auction System using Docker..."

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

print_status() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Build frontend
build_frontend() {
    print_status "Building React frontend..."
    
    cd frontend
    
    # Install dependencies if node_modules doesn't exist
    if [ ! -d "node_modules" ]; then
        print_status "Installing frontend dependencies..."
        npm install --legacy-peer-deps
    fi
    
    # Build production bundle
    print_status "Creating production build..."
    npm run build
    
    cd ..
    
    print_status "Frontend build completed!"
}

# Build backend using Docker
build_backend() {
    print_status "Building Spring Boot backend using Docker..."
    
    # Create a temporary build container
    cat > Dockerfile.build << 'EOF'
FROM maven:3.9.9-eclipse-temurin-17 AS builder


WORKDIR /app
COPY backend/pom.xml .
COPY backend/src ./src

# Build the application
RUN mvn clean package -DskipTests



# Create runtime image
FROM openjdk:17-jdk-slim

WORKDIR /app
COPY --from=builder /app/target/distributed-auction-system-1.0.0.jar app.jar

EXPOSE 8080 1100
CMD ["java", "-jar", "app.jar"]
EOF

    # Build the backend image
    docker build -f Dockerfile.build -t auction-backend:latest .
    
    # Clean up temporary Dockerfile
    rm Dockerfile.build
    
    print_status "Backend built successfully ✅"
}

# Build frontend using Docker
build_frontend() {
    print_status "Building React frontend using Docker..."
    
    # Create a temporary build container
    cat > Dockerfile.frontend << 'EOF'
FROM node:18-alpine AS builder

WORKDIR /app
COPY frontend/package*.json ./
RUN npm ci --only=production

COPY frontend/ ./
RUN npm run build

# Use nginx for serving
FROM nginx:alpine
COPY --from=builder /app/build /usr/share/nginx/html
COPY nginx/nginx.conf /etc/nginx/nginx.conf

EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
EOF

    # Build the frontend image
    docker build -f Dockerfile.frontend -t auction-frontend:latest .
    
    # Clean up temporary Dockerfile
    rm Dockerfile.frontend
    
    print_status "Frontend built successfully ✅"
}

# Update docker-compose to use the built images
update_docker_compose() {
    print_status "Updating docker-compose.yml..."
    
    # Create a backup
    cp docker-compose.yml docker-compose.yml.backup
    
    # Update the docker-compose file to use the built images
    sed -i 's|build:|image:|g' docker-compose.yml
    sed -i 's|context: ./backend|# context: ./backend|g' docker-compose.yml
    sed -i 's|dockerfile: Dockerfile|# dockerfile: Dockerfile|g' docker-compose.yml
    
    # Update backend services to use the built image
    sed -i 's|auction-server-1:|auction-backend:latest\n    container_name: auction-server-1|g' docker-compose.yml
    sed -i 's|auction-server-2:|auction-backend:latest\n    container_name: auction-server-2|g' docker-compose.yml
    sed -i 's|auction-server-3:|auction-backend:latest\n    container_name: auction-server-3|g' docker-compose.yml
    
    # Update nginx to use the built frontend image
    sed -i 's|nginx:alpine|auction-frontend:latest|g' docker-compose.yml
    
    print_status "Docker Compose updated ✅"
}

# Main build process
main() {
    print_status "Starting Docker-based build process..."
    
    # Check if Docker is available
    if ! command -v docker &> /dev/null; then
        print_error "Docker is not available. Please install Docker first."
        exit 1
    fi
    
    build_backend
    build_frontend
    update_docker_compose
    
    print_status "🎉 Docker-based build completed!"
    print_status "You can now start the system with: docker-compose up -d"
    print_status "To restore original docker-compose.yml: cp docker-compose.yml.backup docker-compose.yml"
}

# Handle script arguments
case "${1:-build}" in
    "build")
        main
        ;;
    "backend-only")
        build_backend
        ;;
    "frontend-only")
        build_frontend
        ;;
    "restore")
        if [ -f "docker-compose.yml.backup" ]; then
            cp docker-compose.yml.backup docker-compose.yml
            print_status "Original docker-compose.yml restored"
        else
            print_error "No backup file found"
        fi
        ;;
    "help"|"-h"|"--help")
        echo "Usage: $0 [build|backend-only|frontend-only|restore|help]"
        echo ""
        echo "Options:"
        echo "  build         - Build all components using Docker"
        echo "  backend-only  - Build only the backend using Docker"
        echo "  frontend-only - Build only the frontend using Docker"
        echo "  restore       - Restore original docker-compose.yml"
        echo "  help          - Show this help message"
        ;;
    *)
        print_error "Unknown option: $1"
        echo "Use '$0 help' for usage information."
        exit 1
        ;;
esac
