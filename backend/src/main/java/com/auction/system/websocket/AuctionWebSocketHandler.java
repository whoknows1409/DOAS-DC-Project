package com.auction.system.websocket;

import com.auction.system.model.Auction;
import com.auction.system.model.Bid;
import com.auction.system.service.AuctionService;
import com.auction.system.service.LamportClockService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Controller;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * WebSocket handler for real-time auction updates
 */
@Controller
public class AuctionWebSocketHandler {
    
    private static final Logger logger = LoggerFactory.getLogger(AuctionWebSocketHandler.class);
    
    @Autowired
    private SimpMessagingTemplate messagingTemplate;
    
    @Autowired
    private AuctionService auctionService;
    
    @Autowired
    private LamportClockService lamportClockService;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    // Track active connections and subscriptions
    private final Map<String, String> userSessions = new ConcurrentHashMap<>();
    private final Map<String, Integer> auctionSubscribers = new ConcurrentHashMap<>();
    
    /**
     * Handle bid placement via WebSocket
     */
    @MessageMapping("/bid")
    @SendToUser("/queue/bid-response")
    public Map<String, Object> handleBid(@Payload Map<String, Object> bidMessage) {
        try {
            String auctionId = (String) bidMessage.get("auctionId");
            String bidderId = (String) bidMessage.get("bidderId");
            BigDecimal amount = new BigDecimal(bidMessage.get("amount").toString());
            String sessionId = (String) bidMessage.get("sessionId");
            
            // Increment logical clock
            int timestamp = lamportClockService.incrementAndGet();
            
            // Place the bid
            Bid bid = auctionService.placeBid(auctionId, bidderId, amount, timestamp);
            
            // Prepare response
            Map<String, Object> response = new HashMap<>();
            response.put("type", "BID_PLACED");
            response.put("success", true);
            response.put("bid", bid);
            response.put("logicalTimestamp", timestamp);
            response.put("serverTime", LocalDateTime.now());
            
            // Broadcast to all subscribers of this auction
            broadcastBidUpdate(auctionId, bid, timestamp);
            
            logger.info("WebSocket bid placed: {} on auction {} at timestamp {}", 
                       bid.getId(), auctionId, timestamp);
            
            return response;
            
        } catch (Exception e) {
            logger.error("Failed to place bid via WebSocket", e);
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("type", "BID_ERROR");
            errorResponse.put("success", false);
            errorResponse.put("error", e.getMessage());
            errorResponse.put("logicalTimestamp", lamportClockService.getCurrentTime());
            
            return errorResponse;
        }
    }
    
    /**
     * Handle auction subscription
     */
    @MessageMapping("/subscribe")
    @SendToUser("/queue/subscription-response")
    public Map<String, Object> handleSubscription(@Payload Map<String, Object> subscriptionMessage) {
        try {
            String auctionId = (String) subscriptionMessage.get("auctionId");
            String sessionId = (String) subscriptionMessage.get("sessionId");
            String userId = (String) subscriptionMessage.get("userId");
            
            // Track subscription
            userSessions.put(sessionId, userId);
            auctionSubscribers.merge(auctionId, 1, Integer::sum);
            
            // Get current auction state
            Auction auction = auctionService.getAuction(auctionId);
            List<Bid> bids = auctionService.getBidsForAuction(auctionId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("type", "SUBSCRIPTION_CONFIRMED");
            response.put("auctionId", auctionId);
            response.put("auction", auction);
            response.put("bids", bids);
            response.put("subscriberCount", auctionSubscribers.get(auctionId));
            response.put("logicalTimestamp", lamportClockService.getCurrentTime());
            
            logger.info("User {} subscribed to auction {}", userId, auctionId);
            
            return response;
            
        } catch (Exception e) {
            logger.error("Failed to handle subscription", e);
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("type", "SUBSCRIPTION_ERROR");
            errorResponse.put("error", e.getMessage());
            
            return errorResponse;
        }
    }
    
    /**
     * Handle unsubscription
     */
    @MessageMapping("/unsubscribe")
    public void handleUnsubscription(@Payload Map<String, Object> unsubscriptionMessage) {
        try {
            String auctionId = (String) unsubscriptionMessage.get("auctionId");
            String sessionId = (String) unsubscriptionMessage.get("sessionId");
            
            // Remove subscription tracking
            userSessions.remove(sessionId);
            auctionSubscribers.computeIfPresent(auctionId, (k, v) -> v > 1 ? v - 1 : null);
            
            logger.info("Session {} unsubscribed from auction {}", sessionId, auctionId);
            
        } catch (Exception e) {
            logger.error("Failed to handle unsubscription", e);
        }
    }
    
    /**
     * Broadcast bid update to all auction subscribers
     */
    private void broadcastBidUpdate(String auctionId, Bid bid, int timestamp) {
        try {
            Map<String, Object> update = new HashMap<>();
            update.put("type", "BID_UPDATE");
            update.put("auctionId", auctionId);
            update.put("bid", bid);
            update.put("logicalTimestamp", timestamp);
            update.put("serverTime", LocalDateTime.now());
            
            // Send to topic for all subscribers
            messagingTemplate.convertAndSend("/topic/auction/" + auctionId, update);
            
            // Also send to general auction updates topic
            messagingTemplate.convertAndSend("/topic/auctions", update);
            
        } catch (Exception e) {
            logger.error("Failed to broadcast bid update", e);
        }
    }
    
    /**
     * Broadcast auction status update
     */
    public void broadcastAuctionUpdate(String auctionId, Auction auction, int timestamp) {
        try {
            Map<String, Object> update = new HashMap<>();
            update.put("type", "AUCTION_UPDATE");
            update.put("auctionId", auctionId);
            update.put("auction", auction);
            update.put("logicalTimestamp", timestamp);
            update.put("serverTime", LocalDateTime.now());
            
            // Send to topic for all subscribers
            messagingTemplate.convertAndSend("/topic/auction/" + auctionId, update);
            
            // Also send to general auction updates topic
            messagingTemplate.convertAndSend("/topic/auctions", update);
            
        } catch (Exception e) {
            logger.error("Failed to broadcast auction update", e);
        }
    }
    
    /**
     * Broadcast server status updates
     */
    @Scheduled(fixedDelay = 10000) // Every 10 seconds
    public void broadcastServerStatus() {
        try {
            Map<String, Object> status = new HashMap<>();
            status.put("type", "SERVER_STATUS");
            status.put("logicalClock", lamportClockService.getCurrentTime());
            status.put("activeConnections", userSessions.size());
            status.put("activeAuctions", auctionService.getActiveAuctions().size());
            status.put("serverTime", LocalDateTime.now());
            
            // Send to server status topic
            messagingTemplate.convertAndSend("/topic/server-status", status);
            
        } catch (Exception e) {
            logger.error("Failed to broadcast server status", e);
        }
    }
    
    /**
     * Check for expired auctions and notify subscribers
     */
    @Scheduled(fixedDelay = 30000) // Every 30 seconds
    public void checkExpiredAuctions() {
        try {
            List<Auction> activeAuctions = auctionService.getActiveAuctions();
            LocalDateTime now = LocalDateTime.now();
            
            for (Auction auction : activeAuctions) {
                if (now.isAfter(auction.getEndTime())) {
                    // End the auction
                    Auction endedAuction = auctionService.endAuction(auction.getId().toString());
                    int timestamp = lamportClockService.incrementAndGet();
                    
                    // Broadcast auction end
                    broadcastAuctionUpdate(auction.getId().toString(), endedAuction, timestamp);
                    
                    logger.info("Auction {} ended and notifications sent", auction.getId());
                }
            }
            
        } catch (Exception e) {
            logger.error("Failed to check expired auctions", e);
        }
    }
    
    /**
     * Get current statistics
     */
    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("activeConnections", userSessions.size());
        stats.put("auctionSubscribers", new HashMap<>(auctionSubscribers));
        stats.put("logicalClock", lamportClockService.getCurrentTime());
        return stats;
    }
}