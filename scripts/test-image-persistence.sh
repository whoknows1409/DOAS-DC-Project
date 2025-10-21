#!/bin/bash

# Image Persistence Test Script for DOAS
# This script automates testing of image persistence across container restarts

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

print_header() {
    echo ""
    echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
    echo -e "${BLUE}$1${NC}"
    echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
    echo ""
}

print_success() {
    echo -e "${GREEN}✅ $1${NC}"
}

print_error() {
    echo -e "${RED}❌ $1${NC}"
}

print_info() {
    echo -e "${YELLOW}ℹ️  $1${NC}"
}

print_step() {
    echo -e "${BLUE}▶ $1${NC}"
}

# Test 1: Check if volume exists
test_volume_exists() {
    print_header "TEST 1: Checking if uploads volume exists"
    
    if docker volume ls | grep -q "auction-system_uploads_data"; then
        print_success "Volume 'auction-system_uploads_data' exists"
        
        # Show volume details
        print_info "Volume details:"
        docker volume inspect auction-system_uploads_data | grep -E "(Mountpoint|Name)" || true
        return 0
    else
        print_error "Volume 'auction-system_uploads_data' NOT found!"
        print_info "Run './scripts/start.sh' to create the volume"
        return 1
    fi
}

# Test 2: List current files
test_list_files() {
    print_header "TEST 2: Listing current uploaded files"
    
    if docker ps | grep -q "nginx"; then
        print_step "Files in uploads directory:"
        docker exec auction-system-nginx-1 ls -lh /app/uploads/ || {
            print_error "Failed to list files (containers might not be running)"
            return 1
        }
        
        # Count files
        FILE_COUNT=$(docker exec auction-system-nginx-1 ls -1 /app/uploads/ 2>/dev/null | grep -v "^$" | wc -l)
        print_success "Found $FILE_COUNT file(s) in uploads directory"
        return 0
    else
        print_error "Nginx container is not running"
        print_info "Run './scripts/start.sh' first"
        return 1
    fi
}

# Test 3: Verify files across all backend servers
test_shared_volume() {
    print_header "TEST 3: Verifying shared volume across backend servers"
    
    if ! docker ps | grep -q "auction-server"; then
        print_error "Backend servers are not running"
        return 1
    fi
    
    print_step "Checking if all servers see the same files..."
    
    FILES_S1=$(docker exec auction-system-auction-server-1-1 ls -1 /app/uploads/ 2>/dev/null | wc -l)
    FILES_S2=$(docker exec auction-system-auction-server-2-1 ls -1 /app/uploads/ 2>/dev/null | wc -l)
    FILES_S3=$(docker exec auction-system-auction-server-3-1 ls -1 /app/uploads/ 2>/dev/null | wc -l)
    
    echo "  Server 1: $FILES_S1 files"
    echo "  Server 2: $FILES_S2 files"
    echo "  Server 3: $FILES_S3 files"
    
    if [ "$FILES_S1" == "$FILES_S2" ] && [ "$FILES_S2" == "$FILES_S3" ]; then
        print_success "All servers see the same files - shared volume working correctly!"
        return 0
    else
        print_error "File count mismatch - shared volume might not be working!"
        return 1
    fi
}

# Test 4: Persistence test (restart containers)
test_persistence_restart() {
    print_header "TEST 4: Testing persistence after container restart"
    
    if ! docker ps | grep -q "nginx"; then
        print_error "Containers are not running"
        return 1
    fi
    
    # Count files before restart
    print_step "Counting files before restart..."
    FILES_BEFORE=$(docker exec auction-system-nginx-1 ls -1 /app/uploads/ 2>/dev/null | grep -v "^$" | wc -l)
    print_info "Files before: $FILES_BEFORE"
    
    # Restart containers
    print_step "Restarting containers..."
    docker-compose restart > /dev/null 2>&1
    
    print_step "Waiting for services to come back up (30 seconds)..."
    sleep 30
    
    # Count files after restart
    print_step "Counting files after restart..."
    FILES_AFTER=$(docker exec auction-system-nginx-1 ls -1 /app/uploads/ 2>/dev/null | grep -v "^$" | wc -l)
    print_info "Files after: $FILES_AFTER"
    
    if [ "$FILES_BEFORE" == "$FILES_AFTER" ]; then
        print_success "Persistence test PASSED! Files survived container restart"
        return 0
    else
        print_error "Persistence test FAILED! File count changed"
        return 1
    fi
}

# Test 5: Nuclear test (stop and start)
test_persistence_nuclear() {
    print_header "TEST 5: Nuclear test (docker-compose down + up)"
    
    if ! docker ps | grep -q "nginx"; then
        print_error "Containers are not running"
        return 1
    fi
    
    # Count files before
    print_step "Counting files before shutdown..."
    FILES_BEFORE=$(docker exec auction-system-nginx-1 ls -1 /app/uploads/ 2>/dev/null | grep -v "^$" | wc -l)
    print_info "Files before: $FILES_BEFORE"
    
    # Stop and remove containers
    print_step "Stopping and removing all containers..."
    docker-compose down > /dev/null 2>&1
    
    print_step "Verifying containers are removed..."
    sleep 2
    
    # Check if volume still exists
    if docker volume ls | grep -q "auction-system_uploads_data"; then
        print_success "Volume still exists after container removal"
    else
        print_error "Volume was deleted! This should not happen"
        return 1
    fi
    
    # Start containers again
    print_step "Starting containers again..."
    docker-compose up -d > /dev/null 2>&1
    
    print_step "Waiting for services to come back up (30 seconds)..."
    sleep 30
    
    # Count files after
    print_step "Counting files after restart..."
    FILES_AFTER=$(docker exec auction-system-nginx-1 ls -1 /app/uploads/ 2>/dev/null | grep -v "^$" | wc -l)
    print_info "Files after: $FILES_AFTER"
    
    if [ "$FILES_BEFORE" == "$FILES_AFTER" ]; then
        print_success "NUCLEAR TEST PASSED! Files survived complete container removal"
        return 0
    else
        print_error "Nuclear test FAILED! File count changed"
        return 1
    fi
}

# Test 6: Check web access
test_web_access() {
    print_header "TEST 6: Testing web access to uploaded files"
    
    if ! docker ps | grep -q "nginx"; then
        print_error "Nginx is not running"
        return 1
    fi
    
    # Get first file
    FIRST_FILE=$(docker exec auction-system-nginx-1 ls -1 /app/uploads/ 2>/dev/null | head -1)
    
    if [ -z "$FIRST_FILE" ]; then
        print_info "No files to test web access (upload an image first)"
        return 0
    fi
    
    print_step "Testing access to: http://localhost/uploads/$FIRST_FILE"
    
    HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" "http://localhost/uploads/$FIRST_FILE")
    
    if [ "$HTTP_CODE" == "200" ]; then
        print_success "Web access working! HTTP $HTTP_CODE"
        return 0
    else
        print_error "Web access failed! HTTP $HTTP_CODE"
        return 1
    fi
}

# Main test suite
run_all_tests() {
    print_header "🧪 DOAS - Image Persistence Test Suite"
    
    TESTS_PASSED=0
    TESTS_FAILED=0
    
    # Run tests
    test_volume_exists && ((TESTS_PASSED++)) || ((TESTS_FAILED++))
    test_list_files && ((TESTS_PASSED++)) || ((TESTS_FAILED++))
    test_shared_volume && ((TESTS_PASSED++)) || ((TESTS_FAILED++))
    test_web_access && ((TESTS_PASSED++)) || ((TESTS_FAILED++))
    
    # Summary
    print_header "📊 TEST SUMMARY"
    echo -e "${GREEN}Passed: $TESTS_PASSED${NC}"
    echo -e "${RED}Failed: $TESTS_FAILED${NC}"
    echo ""
    
    if [ $TESTS_FAILED -eq 0 ]; then
        print_success "ALL TESTS PASSED! ✨"
        echo ""
        print_info "To test persistence after restart, run:"
        echo "  $0 restart-test"
        echo ""
        print_info "To test nuclear scenario (full shutdown), run:"
        echo "  $0 nuclear-test"
        return 0
    else
        print_error "Some tests failed. Check the output above."
        return 1
    fi
}

# Command line interface
case "${1:-all}" in
    "all")
        run_all_tests
        ;;
    "volume")
        test_volume_exists
        ;;
    "list")
        test_list_files
        ;;
    "shared")
        test_shared_volume
        ;;
    "web")
        test_web_access
        ;;
    "restart-test")
        test_persistence_restart
        ;;
    "nuclear-test")
        test_persistence_nuclear
        ;;
    "help"|"-h"|"--help")
        echo "DOAS - Image Persistence Test Script"
        echo ""
        echo "Usage: $0 [command]"
        echo ""
        echo "Commands:"
        echo "  all            Run all basic tests (default)"
        echo "  volume         Check if uploads volume exists"
        echo "  list           List uploaded files"
        echo "  shared         Verify shared volume across servers"
        echo "  web            Test web access to files"
        echo "  restart-test   Test persistence after container restart"
        echo "  nuclear-test   Test persistence after full shutdown"
        echo "  help           Show this help message"
        echo ""
        ;;
    *)
        print_error "Unknown command: $1"
        echo "Use '$0 help' for usage information."
        exit 1
        ;;
esac

