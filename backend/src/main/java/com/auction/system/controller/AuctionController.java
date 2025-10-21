package com.auction.system.controller;

import com.auction.system.model.Auction;
import com.auction.system.model.Bid;
import com.auction.system.service.AuctionService;
import com.auction.system.service.DistributedCoordinatorService;
import com.auction.system.service.LamportClockService;
import com.auction.system.dto.BidRequest;
import com.auction.system.dto.AuctionRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * REST Controller for auction operations
 */
@RestController
@RequestMapping("/api/auctions")
@CrossOrigin(origins = "*")
public class AuctionController {
    
    private static final Logger logger = LoggerFactory.getLogger(AuctionController.class);
    
    @Autowired
    private AuctionService auctionService;
    
    @Autowired
    private LamportClockService lamportClockService;
    
    @Autowired
    private DistributedCoordinatorService coordinatorService;
    
    /**
     * Get all auctions
     */
    @GetMapping
    public ResponseEntity<List<Auction>> getAllAuctions() {
        try {
            List<Auction> auctions = auctionService.getAllAuctions();
            return ResponseEntity.ok(auctions);
        } catch (Exception e) {
            logger.error("Failed to get all auctions", e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Get all active auctions
     */
    @GetMapping("/active")
    public ResponseEntity<List<Auction>> getActiveAuctions() {
        try {
            List<Auction> auctions = auctionService.getActiveAuctions();
            return ResponseEntity.ok(auctions);
        } catch (Exception e) {
            logger.error("Failed to get active auctions", e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Get auctions by seller
     */
    @GetMapping("/seller/{sellerId}")
    public ResponseEntity<List<Auction>> getAuctionsBySeller(@PathVariable String sellerId) {
        try {
            List<Auction> auctions = auctionService.getAuctionsBySeller(sellerId);
            return ResponseEntity.ok(auctions);
        } catch (Exception e) {
            logger.error("Failed to get auctions by seller: {}", sellerId, e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    
    /**
     * Create a new auction
     */
    @PostMapping
    public ResponseEntity<Auction> createAuction(@Valid @RequestBody AuctionRequest request) {
        try {
            int timestamp = lamportClockService.incrementAndGet();
            
            Auction auction = new Auction();
            auction.setTitle(request.getTitle());
            auction.setDescription(request.getDescription());
            auction.setStartingPrice(request.getStartingPrice());
            auction.setCurrentPrice(request.getStartingPrice());
            auction.setEndTime(request.getEndTime());
            auction.setSellerId(UUID.fromString(request.getSellerId()));
            auction.setImageUrl(request.getImageUrl());
            
            Auction createdAuction = auctionService.createAuction(auction);
            
            logger.info("Created auction {} at timestamp {}", createdAuction.getId(), timestamp);
            return ResponseEntity.ok(createdAuction);
            
        } catch (Exception e) {
            logger.error("Failed to create auction", e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Place a bid on an auction
     */
    @PostMapping("/{auctionId}/bids")
    public ResponseEntity<Bid> placeBid(@PathVariable String auctionId, 
                                       @Valid @RequestBody BidRequest request) {
        try {
            int timestamp = lamportClockService.incrementAndGet();
            
            Bid bid = auctionService.placeBid(
                auctionId,
                request.getBidderId(),
                request.getAmount(),
                timestamp
            );
            
            logger.info("Placed bid {} on auction {} at timestamp {}", 
                       bid.getId(), auctionId, timestamp);
            
            return ResponseEntity.ok(bid);
            
        } catch (Exception e) {
            logger.error("Failed to place bid on auction: {}", auctionId, e);
            return ResponseEntity.badRequest().build();
        }
    }
    
    /**
     * Get bids for an auction with bidder names
     */
    @GetMapping("/{auctionId}/bids")
    public ResponseEntity<List<com.auction.system.dto.BidWithBidderName>> getBidsForAuction(@PathVariable String auctionId) {
        try {
            List<com.auction.system.dto.BidWithBidderName> bids = auctionService.getBidsWithBidderNames(auctionId);
            return ResponseEntity.ok(bids);
        } catch (Exception e) {
            logger.error("Failed to get bids for auction: {}", auctionId, e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * End an auction
     */
    @PostMapping("/{auctionId}/end")
    public ResponseEntity<Auction> endAuction(@PathVariable String auctionId) {
        try {
            int timestamp = lamportClockService.incrementAndGet();
            
            Auction auction = auctionService.endAuction(auctionId);
            
            logger.info("Ended auction {} at timestamp {}", auctionId, timestamp);
            return ResponseEntity.ok(auction);
            
        } catch (Exception e) {
            logger.error("Failed to end auction: {}", auctionId, e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Get server status including logical clock
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getServerStatus() {
        try {
            Map<String, Object> status = new HashMap<>();
            status.put("logicalClock", lamportClockService.getCurrentTime());
            status.put("timestamp", System.currentTimeMillis());
            status.put("serverHealthy", true);
            status.put("serverId", coordinatorService.getServerId());
            status.put("isCoordinator", coordinatorService.isCoordinator());
            status.put("coordinatorId", coordinatorService.getCoordinatorId());
            
            return ResponseEntity.ok(status);
        } catch (Exception e) {
            logger.error("Failed to get server status", e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Debug endpoint to test auction retrieval
     */
    @GetMapping("/debug/{auctionId}")
    public ResponseEntity<Map<String, Object>> debugAuction(@PathVariable String auctionId) {
        try {
            logger.info("Debug: Getting auction with ID: {}", auctionId);
            
            // Try to get auction
            Auction auction = auctionService.getAuction(auctionId);
            
            Map<String, Object> debug = Map.of(
                "auctionId", auctionId,
                "auctionFound", auction != null,
                "auctionTitle", auction != null ? auction.getTitle() : "null",
                "timestamp", System.currentTimeMillis()
            );
            
            return ResponseEntity.ok(debug);
        } catch (Exception e) {
            logger.error("Debug failed for auction: {}", auctionId, e);
            return ResponseEntity.ok(Map.of(
                "auctionId", auctionId,
                "error", e.getMessage(),
                "timestamp", System.currentTimeMillis()
            ));
        }
    }
    
    /**
     * Debug endpoint to list all auctions
     */
    @GetMapping("/debug/all")
    public ResponseEntity<Map<String, Object>> debugAllAuctions() {
        try {
            List<Auction> auctions = auctionService.getAllAuctions();
            
            Map<String, Object> debug = Map.of(
                "totalAuctions", auctions.size(),
                "auctions", auctions.stream().map(a -> Map.of(
                    "id", a.getId().toString(),
                    "title", a.getTitle(),
                    "status", a.getStatus()
                )).collect(java.util.stream.Collectors.toList()),
                "timestamp", System.currentTimeMillis()
            );
            
            return ResponseEntity.ok(debug);
        } catch (Exception e) {
            logger.error("Debug failed for all auctions", e);
            return ResponseEntity.ok(Map.of(
                "error", e.getMessage(),
                "timestamp", System.currentTimeMillis()
            ));
        }
    }
    
    /**
     * Get auction by ID - This must be last to avoid route conflicts
     */
    @GetMapping("/{auctionId}")
    public ResponseEntity<Auction> getAuction(@PathVariable String auctionId) {
        try {
            logger.info("Getting auction with ID: {}", auctionId);
            Auction auction = auctionService.getAuction(auctionId);
            if (auction == null) {
                logger.warn("Auction not found: {}", auctionId);
                return ResponseEntity.notFound().build();
            }
            logger.info("Found auction: {}", auction.getTitle());
            return ResponseEntity.ok(auction);
        } catch (Exception e) {
            logger.error("Failed to get auction: {}", auctionId, e);
            return ResponseEntity.internalServerError().build();
        }
    }
}