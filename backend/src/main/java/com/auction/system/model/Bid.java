package com.auction.system.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "bids")
public class Bid {
    
    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;
    
    @Column(name = "auction_id", nullable = false)
    private UUID auctionId;
    
    @Column(name = "bidder_id", nullable = false)
    private UUID bidderId;
    
    @Column(name = "amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;
    
    @Column(name = "timestamp", nullable = false)
    private LocalDateTime timestamp;
    
    @Column(name = "logical_timestamp", nullable = false)
    private Integer logicalTimestamp;
    
    @Column(name = "server_id", nullable = false)
    private Integer serverId;
    
    // Constructors
    public Bid() {}
    
    public Bid(UUID auctionId, UUID bidderId, BigDecimal amount, Integer logicalTimestamp, Integer serverId) {
        this.auctionId = auctionId;
        this.bidderId = bidderId;
        this.amount = amount;
        this.logicalTimestamp = logicalTimestamp;
        this.serverId = serverId;
        this.timestamp = LocalDateTime.now();
    }
    
    // JPA lifecycle callback
    @PrePersist
    protected void onCreate() {
        if (timestamp == null) {
            timestamp = LocalDateTime.now();
        }
    }
    
    // Getters and Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    
    public UUID getAuctionId() { return auctionId; }
    public void setAuctionId(UUID auctionId) { this.auctionId = auctionId; }
    
    public UUID getBidderId() { return bidderId; }
    public void setBidderId(UUID bidderId) { this.bidderId = bidderId; }
    
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    
    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
    
    public Integer getLogicalTimestamp() { return logicalTimestamp; }
    public void setLogicalTimestamp(Integer logicalTimestamp) { this.logicalTimestamp = logicalTimestamp; }
    
    public Integer getServerId() { return serverId; }
    public void setServerId(Integer serverId) { this.serverId = serverId; }
    
    @Override
    public String toString() {
        return "Bid{" +
                "id=" + id +
                ", auctionId=" + auctionId +
                ", amount=" + amount +
                ", logicalTimestamp=" + logicalTimestamp +
                ", serverId=" + serverId +
                '}';
    }
}