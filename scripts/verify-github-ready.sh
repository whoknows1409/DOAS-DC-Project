#!/bin/bash

# ============================================
# DOAS - GitHub Upload Verification Script
# ============================================

echo "╔═══════════════════════════════════════════════════════════════╗"
echo "║                                                               ║"
echo "║     🚀 DOAS - GitHub Upload Verification Script              ║"
echo "║                                                               ║"
echo "╚═══════════════════════════════════════════════════════════════╝"
echo ""

ERRORS=0
WARNINGS=0

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Check functions
check_file_exists() {
    if [ -f "$1" ]; then
        echo -e "${GREEN}✓${NC} $1 exists"
    else
        echo -e "${RED}✗${NC} $1 is missing"
        ERRORS=$((ERRORS + 1))
    fi
}

check_file_not_exists() {
    if [ ! -f "$1" ]; then
        echo -e "${GREEN}✓${NC} $1 correctly excluded"
    else
        echo -e "${YELLOW}⚠${NC} $1 should be removed/gitignored"
        WARNINGS=$((WARNINGS + 1))
    fi
}

check_dir_not_exists() {
    if [ ! -d "$1" ]; then
        echo -e "${GREEN}✓${NC} $1 correctly excluded"
    else
        echo -e "${YELLOW}⚠${NC} $1 should be removed/gitignored"
        WARNINGS=$((WARNINGS + 1))
    fi
}

echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "📄 Checking Essential Documentation Files"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
check_file_exists "README.md"
check_file_exists "CONTRIBUTING.md"
check_file_exists "LICENSE"
check_file_exists "DEMO_GUIDE.md"
check_file_exists "DATABASE_COMMANDS.md"
check_file_exists ".env.example"
echo ""

echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "🔧 Checking Configuration Files"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
check_file_exists ".gitignore"
check_file_exists ".dockerignore"
check_file_exists "docker-compose.yml"
check_file_exists "Dockerfile.frontend"
check_file_exists "backend/Dockerfile"
check_file_exists "backend/pom.xml"
check_file_exists "frontend/package.json"
check_file_exists "nginx/nginx.conf"
echo ""

echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "🚫 Checking Excluded Files/Directories"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
check_file_not_exists ".env"
check_dir_not_exists "backend/target"
check_dir_not_exists "frontend/build"
check_dir_not_exists "frontend/node_modules"
check_dir_not_exists "src"
check_dir_not_exists "prisma"
check_dir_not_exists "db"
check_file_not_exists "next.config.ts"
check_file_not_exists "tsconfig.json"
echo ""

echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "📁 Checking Project Structure"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
check_file_exists "backend/src/main/java/com/auction/system/service/DistributedCoordinatorService.java"
check_file_exists "backend/src/main/java/com/auction/system/service/LamportClockService.java"
check_file_exists "backend/src/main/java/com/auction/system/service/TwoPhaseCommitService.java"
check_file_exists "frontend/src/App.js"
check_file_exists "frontend/src/pages/AdminDashboard.js"
check_file_exists "frontend/src/components/ServerHealthFooter.js"
check_file_exists "database/init.sql"
check_file_exists "uploads/.gitkeep"
echo ""

echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "🔍 Checking for Sensitive Data"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

# Check for common secrets in files
if grep -r "password.*=" --include="*.java" --include="*.js" --include="*.yml" --exclude-dir=node_modules . | grep -v "example" | grep -v "your-" | grep -v "auctionpass" > /dev/null 2>&1; then
    echo -e "${YELLOW}⚠${NC} Potential hardcoded passwords found - please review"
    WARNINGS=$((WARNINGS + 1))
else
    echo -e "${GREEN}✓${NC} No hardcoded passwords detected"
fi

if grep -r "api.?key.*=" --include="*.java" --include="*.js" --include="*.yml" --exclude-dir=node_modules . | grep -v "example" > /dev/null 2>&1; then
    echo -e "${YELLOW}⚠${NC} Potential API keys found - please review"
    WARNINGS=$((WARNINGS + 1))
else
    echo -e "${GREEN}✓${NC} No API keys detected"
fi

echo ""

echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "📜 Checking Scripts"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

for script in scripts/*.sh; do
    if [ -x "$script" ]; then
        echo -e "${GREEN}✓${NC} $script is executable"
    else
        echo -e "${YELLOW}⚠${NC} $script is not executable"
        WARNINGS=$((WARNINGS + 1))
    fi
done

echo ""

echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "📊 Repository Statistics"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

echo "📂 Project Structure:"
echo "   - Java files: $(find backend/src -name "*.java" 2>/dev/null | wc -l)"
echo "   - JavaScript files: $(find frontend/src -name "*.js" -o -name "*.jsx" 2>/dev/null | wc -l)"
echo "   - Configuration files: $(find . -maxdepth 2 -name "*.yml" -o -name "*.yaml" -o -name "*.conf" -o -name "*.json" 2>/dev/null | wc -l)"
echo "   - Scripts: $(find scripts -name "*.sh" 2>/dev/null | wc -l)"
echo "   - Documentation: $(find . -maxdepth 1 -name "*.md" 2>/dev/null | wc -l)"

echo ""

if [ -d .git ]; then
    echo "🔗 Git Status:"
    echo "   - Repository initialized: ✓"
    echo "   - Remote configured: $(git remote -v | wc -l > 0 && echo '✓' || echo '✗')"
else
    echo "🔗 Git Status:"
    echo "   - Repository initialized: ✗ (run 'git init')"
fi

echo ""

echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "📋 Summary"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

if [ $ERRORS -eq 0 ] && [ $WARNINGS -eq 0 ]; then
    echo -e "${GREEN}✓ Repository is ready for GitHub upload!${NC}"
    echo ""
    echo "Next steps:"
    echo "1. git init (if not already initialized)"
    echo "2. git add ."
    echo "3. git commit -m 'feat: initial commit - DOAS project'"
    echo "4. git remote add origin <your-github-repo-url>"
    echo "5. git push -u origin main"
elif [ $ERRORS -eq 0 ]; then
    echo -e "${YELLOW}⚠ Repository has $WARNINGS warning(s) but is uploadable${NC}"
    echo "Please review warnings above before uploading."
else
    echo -e "${RED}✗ Repository has $ERRORS error(s) and $WARNINGS warning(s)${NC}"
    echo "Please fix errors before uploading to GitHub."
fi

echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

exit $ERRORS
