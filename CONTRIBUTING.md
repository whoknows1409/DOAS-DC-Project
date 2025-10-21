# Contributing to DOAS - Distributed Online Auction System

**Author:** Omkar Bhoir ([@whoknows1409](https://github.com/whoknows1409))

First off, thank you for considering contributing to DOAS! It's people like you that make DOAS such a great tool for learning distributed systems.

## Table of Contents

- [Code of Conduct](#code-of-conduct)
- [Getting Started](#getting-started)
- [Development Process](#development-process)
- [How Can I Contribute?](#how-can-i-contribute)
- [Style Guidelines](#style-guidelines)
- [Commit Messages](#commit-messages)
- [Pull Request Process](#pull-request-process)

---

## Code of Conduct

This project and everyone participating in it is governed by our Code of Conduct. By participating, you are expected to uphold this code. Please report unacceptable behavior to the project maintainers.

### Our Standards

- **Be respectful** of differing viewpoints and experiences
- **Accept constructive criticism** gracefully
- **Focus on what is best** for the community and the project
- **Show empathy** towards other community members

---

## Getting Started

### Prerequisites

Before you begin, ensure you have the following installed:

- **Docker** 20.10+
- **Docker Compose** 2.0+
- **Java JDK** 17+
- **Maven** 3.8+
- **Node.js** 16+
- **Git**

### Setting Up Development Environment

1. **Fork the repository** on GitHub

2. **Clone your fork**:
   ```bash
   git clone https://github.com/YOUR_USERNAME/auction-system.git
   cd auction-system
   ```

3. **Add upstream remote**:
   ```bash
   git remote add upstream https://github.com/ORIGINAL_OWNER/auction-system.git
   ```

4. **Create a branch**:
   ```bash
   git checkout -b feature/your-feature-name
   ```

5. **Set up the environment**:
   ```bash
   cp .env.example .env
   # Edit .env with your local configuration
   ```

6. **Start the development environment**:
   ```bash
   docker-compose up -d
   ```

---

## Development Process

### Backend Development (Java/Spring Boot)

1. **Navigate to backend directory**:
   ```bash
   cd backend
   ```

2. **Install dependencies**:
   ```bash
   ./mvnw clean install
   ```

3. **Run tests**:
   ```bash
   ./mvnw test
   ```

4. **Run locally** (outside Docker):
   ```bash
   ./mvnw spring-boot:run -Dspring-boot.run.arguments="--server.port=8081 --rmi.port=1101"
   ```

### Frontend Development (React)

1. **Navigate to frontend directory**:
   ```bash
   cd frontend
   ```

2. **Install dependencies**:
   ```bash
   npm install
   ```

3. **Run development server**:
   ```bash
   npm start
   ```
   The app will open at http://localhost:3000

4. **Run tests**:
   ```bash
   npm test
   ```

5. **Build for production**:
   ```bash
   npm run build
   ```

### Testing Changes in Docker

After making changes, rebuild and test in Docker:

```bash
# Rebuild specific service
docker-compose build auction-server-1

# Restart service
docker-compose restart auction-server-1

# View logs
docker-compose logs -f auction-server-1
```

---

## How Can I Contribute?

### Reporting Bugs

Before creating bug reports, please check the existing issues to avoid duplicates.

**When submitting a bug report, include**:

- **Clear title** describing the issue
- **Steps to reproduce** the behavior
- **Expected behavior** vs actual behavior
- **Screenshots** if applicable
- **Environment details**:
  - OS (Linux, macOS, Windows)
  - Docker version
  - Browser (for frontend issues)
- **Logs** from relevant services

**Example**:

```markdown
## Bug: Auction bids not updating in real-time

**Steps to Reproduce:**
1. Open auction detail page
2. Place bid from another browser
3. Original browser doesn't show new bid

**Expected:** Real-time update via WebSocket
**Actual:** Page requires manual refresh

**Environment:**
- OS: Ubuntu 22.04
- Browser: Firefox 115
- Docker: 24.0.5

**Logs:**
[Attach relevant WebSocket logs]
```

### Suggesting Enhancements

Enhancement suggestions are tracked as GitHub issues.

**When suggesting an enhancement, include**:

- **Clear title** and description
- **Use case** - why is this enhancement needed?
- **Proposed solution** - how would you implement it?
- **Alternatives considered**
- **Impact** on existing functionality

### Contributing Code

#### Areas for Contribution

1. **Distributed Systems Features**:
   - Improve election algorithms
   - Add new clock synchronization methods
   - Enhance 2PC protocol
   - Implement consensus algorithms (Paxos, Raft)

2. **Backend Improvements**:
   - Add more REST endpoints
   - Improve error handling
   - Optimize database queries
   - Add caching strategies

3. **Frontend Enhancements**:
   - UI/UX improvements
   - New visualization components
   - Mobile responsiveness
   - Accessibility features

4. **DevOps**:
   - CI/CD pipeline improvements
   - Kubernetes deployment
   - Monitoring and observability
   - Performance optimization

5. **Documentation**:
   - Code comments
   - API documentation
   - Architecture diagrams
   - Tutorial videos

#### Before Starting Work

1. **Check existing issues** to avoid duplicate work
2. **Comment on the issue** you'd like to work on
3. **Wait for approval** from maintainers
4. **Discuss approach** if it's a significant change

---

## Style Guidelines

### Java Code Style

Follow the **Google Java Style Guide** with these specifics:

- **Indentation**: 4 spaces (not tabs)
- **Line length**: Maximum 120 characters
- **Braces**: K&R style (opening brace on same line)
- **Naming**:
  - Classes: `PascalCase`
  - Methods/Variables: `camelCase`
  - Constants: `UPPER_SNAKE_CASE`
  - Packages: `lowercase`

**Example**:

```java
public class AuctionService {
    private static final int MAX_BID_AMOUNT = 1000000;
    
    public Auction createAuction(AuctionRequest request) {
        if (request.getStartingPrice() > MAX_BID_AMOUNT) {
            throw new IllegalArgumentException("Price exceeds maximum");
        }
        
        // Implementation
    }
}
```

### JavaScript/React Code Style

Follow **Airbnb JavaScript Style Guide**:

- **Indentation**: 2 spaces
- **Quotes**: Single quotes for strings
- **Semicolons**: Always use them
- **Arrow functions**: Prefer arrow functions for callbacks
- **Destructuring**: Use object/array destructuring

**Example**:

```javascript
const AuctionCard = ({ auction, onBidClick }) => {
  const { title, currentPrice, endTime } = auction;
  
  const handleBid = () => {
    onBidClick(auction.id);
  };
  
  return (
    <Card>
      <h3>{title}</h3>
      <p>${currentPrice}</p>
      <Button onClick={handleBid}>Place Bid</Button>
    </Card>
  );
};
```

### Documentation Style

- **JavaDoc** for all public methods in Java
- **JSDoc** for complex JavaScript functions
- **Inline comments** for complex logic
- **README updates** for new features

**Java Example**:

```java
/**
 * Places a bid on an auction using distributed locking.
 * 
 * @param auctionId the UUID of the auction
 * @param bid the bid details including amount and bidder
 * @return true if bid was placed successfully, false otherwise
 * @throws AuctionNotFoundException if auction doesn't exist
 * @throws InvalidBidException if bid amount is invalid
 */
public boolean placeBid(String auctionId, Bid bid) {
    // Implementation
}
```

---

## Commit Messages

Follow **Conventional Commits** specification:

### Format

```
<type>(<scope>): <subject>

<body>

<footer>
```

### Types

- `feat`: New feature
- `fix`: Bug fix
- `docs`: Documentation changes
- `style`: Code style changes (formatting, no logic change)
- `refactor`: Code refactoring
- `perf`: Performance improvements
- `test`: Adding or updating tests
- `chore`: Maintenance tasks
- `ci`: CI/CD changes

### Examples

```
feat(auction): add real-time bid notifications via WebSocket

Implemented WebSocket connection to push live bid updates to all 
connected clients viewing the same auction.

Closes #42
```

```
fix(election): resolve race condition in Bully election algorithm

The election process could start multiple times concurrently,
leading to multiple coordinators. Added synchronization lock
to prevent this.

Fixes #87
```

```
docs(readme): add troubleshooting section for RMI issues

Added common RMI connection problems and their solutions based
on user feedback.
```

---

## Pull Request Process

### Before Submitting

1. **Update your branch** with latest upstream changes:
   ```bash
   git fetch upstream
   git rebase upstream/main
   ```

2. **Run all tests**:
   ```bash
   # Backend
   cd backend && ./mvnw test
   
   # Frontend
   cd frontend && npm test
   ```

3. **Ensure code follows style guidelines**:
   ```bash
   # Backend (if checkstyle is configured)
   ./mvnw checkstyle:check
   
   # Frontend
   npm run lint
   ```

4. **Update documentation**:
   - Update README.md if adding new features
   - Add/update JSDoc or JavaDoc comments
   - Update API documentation if endpoints changed

### Submitting Pull Request

1. **Push your branch**:
   ```bash
   git push origin feature/your-feature-name
   ```

2. **Create Pull Request** on GitHub

3. **Fill in the PR template**:

   ```markdown
   ## Description
   Brief description of changes
   
   ## Type of Change
   - [ ] Bug fix
   - [ ] New feature
   - [ ] Breaking change
   - [ ] Documentation update
   
   ## Related Issue
   Closes #123
   
   ## How Has This Been Tested?
   Describe testing approach
   
   ## Checklist
   - [ ] Code follows style guidelines
   - [ ] Self-review completed
   - [ ] Comments added for complex code
   - [ ] Documentation updated
   - [ ] Tests added/updated
   - [ ] All tests passing
   - [ ] No merge conflicts
   ```

4. **Respond to review comments** promptly

5. **Update PR** if changes are requested:
   ```bash
   # Make changes
   git add .
   git commit -m "refactor: address review comments"
   git push origin feature/your-feature-name
   ```

### PR Review Process

- At least **one approval** required from maintainers
- All **automated checks** must pass
- **No merge conflicts** with main branch
- **Documentation** must be updated
- **Tests** must cover new code

---

## Development Tips

### Testing Distributed Features

1. **Test with multiple servers**:
   ```bash
   docker-compose up -d
   # Kill a server to test failover
   docker stop auction-system-auction-server-3-1
   ```

2. **Monitor logs across all servers**:
   ```bash
   docker-compose logs -f auction-server-1 auction-server-2 auction-server-3
   ```

3. **Test concurrent operations**:
   ```bash
   # Use Apache Bench for load testing
   ab -n 100 -c 10 http://localhost/api/auctions/
   ```

### Debugging

1. **Backend debugging**:
   - Add debug logs with SLF4J logger
   - Use IntelliJ IDEA remote debugging
   - Check RMI connections with `jconsole`

2. **Frontend debugging**:
   - Use React DevTools browser extension
   - Check Network tab for API calls
   - Use console.log strategically (remove before PR)

3. **Database debugging**:
   ```bash
   docker exec -it auction-system-postgres-1 psql -U auctionuser -d auctiondb
   ```

### Common Gotchas

- **RMI hostname issues**: Use `java.rmi.server.hostname` property
- **Docker networking**: Services communicate via service names, not localhost
- **Clock synchronization**: Test with time drift scenarios
- **Concurrent modifications**: Always test with multiple simultaneous requests

---

## Questions?

Feel free to:
- **Open an issue** with the `question` label
- **Join our Discord** server (if applicable)
- **Email maintainers** at bhoiromkar1409@gmail.com

---

**Thank you for contributing to DOAS!** 🎉

Your contributions help make this a better learning resource for distributed systems students worldwide.

