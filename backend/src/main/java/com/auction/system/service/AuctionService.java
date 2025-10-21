package com.auction.system.service;

import com.auction.system.rmi.CoordinatorService;
import com.auction.system.model.Auction;
import com.auction.system.model.Bid;
import com.auction.system.model.User;
import com.auction.system.repository.AuctionRepository;
import com.auction.system.repository.BidRepository;
import com.auction.system.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Service for managing auctions and bids
 */
@Service
public class AuctionService {
    
    private static final Logger logger = LoggerFactory.getLogger(AuctionService.class);
    
    @Autowired
    private AuctionRepository auctionRepository;
    
    @Autowired
    private BidRepository bidRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    
    @Autowired
    private DistributedCoordinatorService coordinatorService;
    
    private static final String BID_LOCK_PREFIX = "bid_lock:";
    private static final String AUCTION_CACHE_PREFIX = "auction:";
    private static final int LOCK_TIMEOUT = 30; // seconds
    
    /**
     * Create a new auction
     */
    @Transactional
    public Auction createAuction(Auction auction) {
        try {
            auction.setId(UUID.randomUUID());
            auction.setCreatedAt(LocalDateTime.now());
            auction.setUpdatedAt(LocalDateTime.now());
            auction.setStatus("ACTIVE");
            
            Auction savedAuction = auctionRepository.save(auction);
            
            // Cache the auction
            cacheAuction(savedAuction);
            
            // Replicate to other servers
            replicateAuctionCreation(savedAuction);
            
            logger.info("Created auction: {}", savedAuction.getId());
            return savedAuction;
            
        } catch (Exception e) {
            logger.error("Failed to create auction", e);
            throw new RuntimeException("Failed to create auction", e);
        }
    }
    
    /**
     * Place a bid with distributed locking
     */
    @Transactional
    public Bid placeBid(String auctionId, String bidderId, BigDecimal amount, int logicalTimestamp) {
        String lockKey = BID_LOCK_PREFIX + auctionId;
        
        try {
            // Try to acquire distributed lock (non-blocking, Redis optional)
            acquireLock(lockKey);
            
            // Validate auction
            Auction auction = getAuction(auctionId);
            if (auction == null) {
                throw new RuntimeException("Auction not found: " + auctionId);
            }
            
            if (!"ACTIVE".equals(auction.getStatus())) {
                throw new RuntimeException("Auction is not active: " + auctionId);
            }
            
            if (LocalDateTime.now().isAfter(auction.getEndTime())) {
                throw new RuntimeException("Auction has ended: " + auctionId);
            }
            
            if (amount.compareTo(auction.getCurrentPrice()) <= 0) {
                throw new RuntimeException("Bid amount must be higher than current price");
            }
            
            // Create bid
            Bid bid = new Bid();
            bid.setId(UUID.randomUUID());
            bid.setAuctionId(UUID.fromString(auctionId));
            bid.setBidderId(UUID.fromString(bidderId));
            bid.setAmount(amount);
            bid.setTimestamp(LocalDateTime.now());
            bid.setLogicalTimestamp(logicalTimestamp);
            bid.setServerId(coordinatorService.getServerId());
            
            // Save bid
            Bid savedBid = bidRepository.save(bid);
            
            // Update auction current price
            auction.setCurrentPrice(amount);
            auction.setUpdatedAt(LocalDateTime.now());
            auctionRepository.save(auction);
            
            // Update cache (optional, non-blocking)
            try {
                cacheAuction(auction);
            } catch (Exception e) {
                logger.warn("Failed to cache auction, continuing: {}", e.getMessage());
            }
            
            // Replicate bid to other servers
            replicateBidCreation(savedBid);
            
            logger.info("Placed bid: {} on auction: {} for amount: {}", 
                       savedBid.getId(), auctionId, amount);
            
            return savedBid;
            
        } finally {
            // Release lock
            releaseLock(lockKey);
        }
    }
    
    /**
     * Get auction by ID (with cache fallback)
     */
    public Auction getAuction(String auctionId) {
        try {
            logger.info("Getting auction with ID: {}", auctionId);
            
            // Load directly from database (Redis temporarily disabled)
            logger.info("Loading auction from database: {}", auctionId);
            Auction auction = auctionRepository.findById(UUID.fromString(auctionId)).orElse(null);
            if (auction != null) {
                logger.info("Found auction in database: {}", auctionId);
            } else {
                logger.warn("Auction not found in database: {}", auctionId);
            }
            
            return auction;
            
        } catch (Exception e) {
            logger.error("Failed to get auction: {}", auctionId, e);
            return null;
        }
    }
    
    /**
     * Get all auctions
     */
    public List<Auction> getAllAuctions() {
        try {
            return auctionRepository.findAll();
        } catch (Exception e) {
            logger.error("Failed to get all auctions", e);
            return List.of();
        }
    }
    
    /**
     * Get all active auctions
     */
    public List<Auction> getActiveAuctions() {
        try {
            return auctionRepository.findByStatusAndEndTimeAfterOrderByEndTimeAsc("ACTIVE", LocalDateTime.now());
        } catch (Exception e) {
            logger.error("Failed to get active auctions", e);
            return List.of();
        }
    }
    
    /**
     * Admin function to end an auction early
     */
    @Transactional
    public boolean endAuctionEarly(String auctionId) {
        try {
            logger.info("Ending auction early: {}", auctionId);
            
            Auction auction = auctionRepository.findById(UUID.fromString(auctionId)).orElse(null);
            if (auction == null) {
                logger.error("Auction not found: {}", auctionId);
                return false;
            }
            
            if (!"ACTIVE".equals(auction.getStatus())) {
                logger.warn("Auction is not active: {}", auctionId);
                return false;
            }
            
            // Update auction status to ENDED
            auction.setStatus("ENDED");
            auction.setEndTime(LocalDateTime.now());
            auction.setUpdatedAt(LocalDateTime.now());
            
            // Declare winner
            declareWinner(auction);
            
            auctionRepository.save(auction);
            
            // Update cache
            try {
                cacheAuction(auction);
            } catch (Exception e) {
                logger.warn("Failed to cache auction after ending, continuing: {}", e.getMessage());
            }
            
            // Replicate the update to other servers
            replicateAuctionUpdate(auction);
            
            logger.info("Successfully ended auction early: {}", auctionId);
            return true;
            
        } catch (Exception e) {
            logger.error("Failed to end auction early: {}", auctionId, e);
            return false;
        }
    }
    
    /**
     * Get auctions by seller
     */
    public List<Auction> getAuctionsBySeller(String sellerId) {
        try {
            return auctionRepository.findBySellerId(UUID.fromString(sellerId));
        } catch (Exception e) {
            logger.error("Failed to get auctions by seller: {}", sellerId, e);
            return List.of();
        }
    }
    
    /**
     * Get bids for an auction
     */
    public List<Bid> getBidsForAuction(String auctionId) {
        try {
            return bidRepository.findByAuctionIdOrderByTimestampDesc(UUID.fromString(auctionId));
        } catch (Exception e) {
            logger.error("Failed to get bids for auction: {}", auctionId, e);
            return List.of();
        }
    }
    
    /**
     * Get bids for auction with bidder names
     */
    public List<com.auction.system.dto.BidWithBidderName> getBidsWithBidderNames(String auctionId) {
        try {
            List<Bid> bids = bidRepository.findByAuctionIdOrderByTimestampDesc(UUID.fromString(auctionId));
            List<com.auction.system.dto.BidWithBidderName> result = new ArrayList<>();
            
            for (Bid bid : bids) {
                String bidderName = "Unknown";
                try {
                    User bidder = userRepository.findById(bid.getBidderId()).orElse(null);
                    if (bidder != null) {
                        bidderName = bidder.getUsername();
                    }
                } catch (Exception e) {
                    logger.warn("Failed to fetch bidder name for bid {}", bid.getId());
                }
                
                com.auction.system.dto.BidWithBidderName bidWithName = new com.auction.system.dto.BidWithBidderName(
                    bid.getId(),
                    bid.getAuctionId(),
                    bid.getBidderId(),
                    bidderName,
                    bid.getAmount(),
                    bid.getTimestamp(),
                    bid.getLogicalTimestamp(),
                    bid.getServerId()
                );
                result.add(bidWithName);
            }
            
            return result;
        } catch (Exception e) {
            logger.error("Failed to get bids with bidder names for auction: {}", auctionId, e);
            return List.of();
        }
    }
    
    /**
     * Get total bids count
     */
    public int getTotalBidsCount() {
        try {
            return (int) bidRepository.count();
        } catch (Exception e) {
            logger.error("Failed to get total bids count", e);
            return 0;
        }
    }
    
    /**
     * End an auction
     */
    @Transactional
    public Auction endAuction(String auctionId) {
        try {
            Auction auction = getAuction(auctionId);
            if (auction == null) {
                throw new RuntimeException("Auction not found: " + auctionId);
            }
            
            auction.setStatus("ENDED");
            auction.setUpdatedAt(LocalDateTime.now());
            
            Auction savedAuction = auctionRepository.save(auction);
            cacheAuction(savedAuction);
            
            // Replicate auction end
            replicateAuctionUpdate(savedAuction);
            
            logger.info("Ended auction: {}", auctionId);
            return savedAuction;
            
        } catch (Exception e) {
            logger.error("Failed to end auction: {}", auctionId, e);
            throw new RuntimeException("Failed to end auction", e);
        }
    }
    
    /**
     * Execute operation for Two-Phase Commit
     */
    public void executeInsert(CoordinatorService.Operation operation) {
        try {
            String table = operation.getTable();
            Map<String, Object> data = operation.getData();
            
            switch (table.toLowerCase()) {
                case "bids":
                    executeBidInsert(data);
                    break;
                case "auctions":
                    executeAuctionInsert(data);
                    break;
                default:
                    logger.warn("Unknown table for insert: {}", table);
            }
            
        } catch (Exception e) {
            logger.error("Failed to execute insert operation", e);
            throw new RuntimeException("Insert operation failed", e);
        }
    }
    
    /**
     * Execute update operation for Two-Phase Commit
     */
    public void executeUpdate(CoordinatorService.Operation operation) {
        try {
            String table = operation.getTable();
            String recordId = operation.getRecordId();
            Map<String, Object> data = operation.getData();
            
            switch (table.toLowerCase()) {
                case "auctions":
                    executeAuctionUpdate(recordId, data);
                    break;
                case "bids":
                    executeBidUpdate(recordId, data);
                    break;
                default:
                    logger.warn("Unknown table for update: {}", table);
            }
            
        } catch (Exception e) {
            logger.error("Failed to execute update operation", e);
            throw new RuntimeException("Update operation failed", e);
        }
    }
    
    /**
     * Execute delete operation for Two-Phase Commit
     */
    public void executeDelete(CoordinatorService.Operation operation) {
        try {
            String table = operation.getTable();
            String recordId = operation.getRecordId();
            
            switch (table.toLowerCase()) {
                case "bids":
                    executeBidDelete(recordId);
                    break;
                case "auctions":
                    executeAuctionDelete(recordId);
                    break;
                default:
                    logger.warn("Unknown table for delete: {}", table);
            }
            
        } catch (Exception e) {
            logger.error("Failed to execute delete operation", e);
            throw new RuntimeException("Delete operation failed", e);
        }
    }
    
    private boolean acquireLock(String lockKey) {
        try {
            Boolean result = redisTemplate.opsForValue().setIfAbsent(lockKey, "locked", LOCK_TIMEOUT, TimeUnit.SECONDS);
            return Boolean.TRUE.equals(result);
        } catch (Exception e) {
            logger.error("Failed to acquire lock: {}", lockKey, e);
            return false;
        }
    }
    
    private void releaseLock(String lockKey) {
        try {
            redisTemplate.delete(lockKey);
        } catch (Exception e) {
            logger.error("Failed to release lock: {}", lockKey, e);
        }
    }
    
    /**
     * Declare winner for an auction
     */
    private void declareWinner(Auction auction) {
        try {
            // Find the highest bid for this auction
            List<Bid> bids = bidRepository.findByAuctionIdOrderByAmountDesc(auction.getId());
            
            if (bids != null && !bids.isEmpty()) {
                Bid winningBid = bids.get(0);
                auction.setWinnerId(winningBid.getBidderId());
                
                // Get winner name from User table
                try {
                    UUID winnerId = winningBid.getBidderId();
                    User winner = userRepository.findById(winnerId).orElse(null);
                    if (winner != null) {
                        auction.setWinnerName(winner.getUsername());
                        logger.info("Auction {} won by user {} ({})", auction.getId(), winner.getUsername(), winnerId);
                    } else {
                        auction.setWinnerName("Unknown User");
                        logger.warn("Winner user not found for auction {}", auction.getId());
                    }
                } catch (Exception e) {
                    auction.setWinnerName("Unknown User");
                    logger.error("Failed to fetch winner name", e);
                }
            } else {
                logger.info("No bids for auction {}, no winner declared", auction.getId());
                auction.setWinnerId(null);
                auction.setWinnerName(null);
            }
        } catch (Exception e) {
            logger.error("Failed to declare winner for auction {}", auction.getId(), e);
        }
    }
    
    /**
     * Scheduled task to auto-end expired auctions and declare winners
     */
    @org.springframework.scheduling.annotation.Scheduled(fixedRate = 60000) // Run every minute
    public void autoEndExpiredAuctions() {
        try {
            List<Auction> activeAuctions = auctionRepository.findByStatus("ACTIVE");
            LocalDateTime now = LocalDateTime.now();
            
            for (Auction auction : activeAuctions) {
                if (now.isAfter(auction.getEndTime())) {
                    logger.info("Auto-ending expired auction: {}", auction.getId());
                    auction.setStatus("ENDED");
                    auction.setUpdatedAt(now);
                    
                    // Declare winner
                    declareWinner(auction);
                    
                    auctionRepository.save(auction);
                    
                    // Update cache
                    try {
                        cacheAuction(auction);
                    } catch (Exception e) {
                        logger.warn("Failed to cache ended auction", e);
                    }
                    
                    // Replicate update
                    replicateAuctionUpdate(auction);
                }
            }
        } catch (Exception e) {
            logger.error("Failed to auto-end expired auctions", e);
        }
    }
    
    private void cacheAuction(Auction auction) {
        try {
            String cacheKey = AUCTION_CACHE_PREFIX + auction.getId();
            redisTemplate.opsForValue().set(cacheKey, auction, 300, TimeUnit.SECONDS); // 5 minutes cache
        } catch (Exception e) {
            logger.error("Failed to cache auction: {}", auction.getId(), e);
        }
    }
    
    private void replicateAuctionCreation(Auction auction) {
        try {
            CoordinatorService.ReplicationRequest request = new CoordinatorService.ReplicationRequest(
                UUID.randomUUID().toString(),
                "CREATE",
                "auctions",
                auction.getId().toString(),
                Map.of("id", auction.getId(), "title", auction.getTitle(), "currentPrice", auction.getCurrentPrice()),
                coordinatorService.getLogicalClock()
            );
            
            // Send to all peers
            for (Map.Entry<Integer, CoordinatorService> entry : coordinatorService.getPeerServices().entrySet()) {
                try {
                    entry.getValue().replicateData(request);
                } catch (Exception e) {
                    logger.warn("Failed to replicate auction creation to server {}", entry.getKey());
                }
            }
            
        } catch (Exception e) {
            logger.error("Failed to replicate auction creation", e);
        }
    }
    
    private void replicateBidCreation(Bid bid) {
        try {
            CoordinatorService.ReplicationRequest request = new CoordinatorService.ReplicationRequest(
                UUID.randomUUID().toString(),
                "CREATE",
                "bids",
                bid.getId().toString(),
                Map.of("id", bid.getId(), "auctionId", bid.getAuctionId(), "amount", bid.getAmount()),
                coordinatorService.getLogicalClock()
            );
            
            // Send to all peers
            for (Map.Entry<Integer, CoordinatorService> entry : coordinatorService.getPeerServices().entrySet()) {
                try {
                    entry.getValue().replicateData(request);
                } catch (Exception e) {
                    logger.warn("Failed to replicate bid creation to server {}", entry.getKey());
                }
            }
            
        } catch (Exception e) {
            logger.error("Failed to replicate bid creation", e);
        }
    }
    
    private void replicateAuctionUpdate(Auction auction) {
        try {
            Map<String, Object> data = new HashMap<>();
            data.put("status", auction.getStatus());
            data.put("currentPrice", auction.getCurrentPrice());
            if (auction.getWinnerId() != null) {
                data.put("winnerId", auction.getWinnerId());
            }
            if (auction.getWinnerName() != null) {
                data.put("winnerName", auction.getWinnerName());
            }
            
            CoordinatorService.ReplicationRequest request = new CoordinatorService.ReplicationRequest(
                UUID.randomUUID().toString(),
                "UPDATE",
                "auctions",
                auction.getId().toString(),
                data,
                coordinatorService.getLogicalClock()
            );
            
            // Send to all peers
            for (Map.Entry<Integer, CoordinatorService> entry : coordinatorService.getPeerServices().entrySet()) {
                try {
                    entry.getValue().replicateData(request);
                } catch (Exception e) {
                    logger.warn("Failed to replicate auction update to server {}", entry.getKey());
                }
            }
            
        } catch (Exception e) {
            logger.error("Failed to replicate auction update", e);
        }
    }
    
    private void executeBidInsert(Map<String, Object> data) {
        Bid bid = new Bid();
        bid.setId(UUID.fromString(data.get("id").toString()));
        bid.setAuctionId(UUID.fromString(data.get("auctionId").toString()));
        bid.setBidderId(UUID.fromString(data.get("bidderId").toString()));
        bid.setAmount(new BigDecimal(data.get("amount").toString()));
        bid.setTimestamp((LocalDateTime) data.get("timestamp"));
        bid.setLogicalTimestamp((Integer) data.get("logicalTimestamp"));
        bid.setServerId((Integer) data.get("serverId"));
        
        bidRepository.save(bid);
    }
    
    private void executeAuctionInsert(Map<String, Object> data) {
        Auction auction = new Auction();
        auction.setId(UUID.fromString(data.get("id").toString()));
        auction.setTitle(data.get("title").toString());
        auction.setDescription(data.get("description").toString());
        auction.setStartingPrice(new BigDecimal(data.get("startingPrice").toString()));
        auction.setCurrentPrice(new BigDecimal(data.get("currentPrice").toString()));
        auction.setEndTime((LocalDateTime) data.get("endTime"));
        auction.setSellerId(UUID.fromString(data.get("sellerId").toString()));
        auction.setStatus(data.get("status").toString());
        auction.setCreatedAt((LocalDateTime) data.get("createdAt"));
        auction.setUpdatedAt((LocalDateTime) data.get("updatedAt"));
        
        auctionRepository.save(auction);
    }
    
    private void executeAuctionUpdate(String recordId, Map<String, Object> data) {
        Auction auction = auctionRepository.findById(UUID.fromString(recordId)).orElse(null);
        if (auction != null) {
            if (data.containsKey("status")) {
                auction.setStatus(data.get("status").toString());
            }
            if (data.containsKey("currentPrice")) {
                auction.setCurrentPrice(new BigDecimal(data.get("currentPrice").toString()));
            }
            if (data.containsKey("winnerId")) {
                auction.setWinnerId(UUID.fromString(data.get("winnerId").toString()));
            }
            if (data.containsKey("winnerName")) {
                auction.setWinnerName(data.get("winnerName").toString());
            }
            auction.setUpdatedAt(LocalDateTime.now());
            auctionRepository.save(auction);
        }
    }
    
    private void executeBidUpdate(String recordId, Map<String, Object> data) {
        // Bid updates are typically not allowed, but implementation provided for completeness
        logger.warn("Bid update requested for record {}: {}", recordId, data);
    }
    
    private void executeBidDelete(String recordId) {
        bidRepository.deleteById(UUID.fromString(recordId));
    }
    
    private void executeAuctionDelete(String recordId) {
        auctionRepository.deleteById(UUID.fromString(recordId));
    }
    
    // Getters for accessing coordinator service properties
    public int getLogicalClock() {
        return coordinatorService.getLogicalClock();
    }
}