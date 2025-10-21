package com.auction.system.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO for Bid with bidder's username
 */
public class BidWithBidderName {
    private UUID id;
    private UUID auctionId;
    private UUID bidderId;
    private String bidderName;
    private BigDecimal amount;
    private LocalDateTime timestamp;
    private Integer logicalTimestamp;
    private Integer serverId;
    
    public BidWithBidderName() {}
    
    public BidWithBidderName(UUID id, UUID auctionId, UUID bidderId, String bidderName, 
                            BigDecimal amount, LocalDateTime timestamp, 
                            Integer logicalTimestamp, Integer serverId) {
        this.id = id;
        this.auctionId = auctionId;
        this.bidderId = bidderId;
        this.bidderName = bidderName;
        this.amount = amount;
        this.timestamp = timestamp;
        this.logicalTimestamp = logicalTimestamp;
        this.serverId = serverId;
    }
    
    // Getters and setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    
    public UUID getAuctionId() { return auctionId; }
    public void setAuctionId(UUID auctionId) { this.auctionId = auctionId; }
    
    public UUID getBidderId() { return bidderId; }
    public void setBidderId(UUID bidderId) { this.bidderId = bidderId; }
    
    public String getBidderName() { return bidderName; }
    public void setBidderName(String bidderName) { this.bidderName = bidderName; }
    
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    
    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
    
    public Integer getLogicalTimestamp() { return logicalTimestamp; }
    public void setLogicalTimestamp(Integer logicalTimestamp) { this.logicalTimestamp = logicalTimestamp; }
    
    public Integer getServerId() { return serverId; }
    public void setServerId(Integer serverId) { this.serverId = serverId; }
}
