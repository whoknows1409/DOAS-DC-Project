package com.auction.system.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Future;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class AuctionRequest {
    
    @NotBlank(message = "Title is required")
    private String title;
    
    private String description;
    
    @NotNull(message = "Starting price is required")
    @DecimalMin(value = "0.01", message = "Starting price must be greater than 0")
    private BigDecimal startingPrice;
    
    @NotNull(message = "End time is required")
    @Future(message = "End time must be in the future")
    private LocalDateTime endTime;
    
    @NotBlank(message = "Seller ID is required")
    private String sellerId;
    
    private String imageUrl;
    
    public AuctionRequest() {}
    
    public AuctionRequest(String title, String description, BigDecimal startingPrice, 
                         LocalDateTime endTime, String sellerId) {
        this.title = title;
        this.description = description;
        this.startingPrice = startingPrice;
        this.endTime = endTime;
        this.sellerId = sellerId;
    }
    
    public String getTitle() {
        return title;
    }
    
    public void setTitle(String title) {
        this.title = title;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public BigDecimal getStartingPrice() {
        return startingPrice;
    }
    
    public void setStartingPrice(BigDecimal startingPrice) {
        this.startingPrice = startingPrice;
    }
    
    public LocalDateTime getEndTime() {
        return endTime;
    }
    
    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }
    
    public String getSellerId() {
        return sellerId;
    }
    
    public void setSellerId(String sellerId) {
        this.sellerId = sellerId;
    }
    
    public String getImageUrl() {
        return imageUrl;
    }
    
    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }
}