package com.auction.system.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

public class BidRequest {
    
    @NotBlank(message = "Bidder ID is required")
    private String bidderId;
    
    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
    private BigDecimal amount;
    
    public BidRequest() {}
    
    public BidRequest(String bidderId, BigDecimal amount) {
        this.bidderId = bidderId;
        this.amount = amount;
    }
    
    public String getBidderId() {
        return bidderId;
    }
    
    public void setBidderId(String bidderId) {
        this.bidderId = bidderId;
    }
    
    public BigDecimal getAmount() {
        return amount;
    }
    
    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }
}