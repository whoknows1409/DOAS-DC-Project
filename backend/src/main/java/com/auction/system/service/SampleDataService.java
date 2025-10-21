package com.auction.system.service;

import com.auction.system.model.Auction;
import com.auction.system.model.User;
import com.auction.system.model.UserRole;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Service to initialize sample data for the auction system
 */
@Service
public class SampleDataService implements CommandLineRunner {
    
    private static final Logger logger = LoggerFactory.getLogger(SampleDataService.class);
    
    @Autowired
    private UserService userService;
    
    @Autowired
    private AuctionService auctionService;
    
    @Override
    public void run(String... args) throws Exception {
        // Only initialize sample data if no users exist
        if (userService.getAllUsers().isEmpty()) {
            logger.info("Initializing sample data...");
            initializeSampleData();
            logger.info("Sample data initialization completed");
        } else {
            logger.info("Sample data already exists, skipping initialization");
        }
    }
    
    private void initializeSampleData() {
        try {
            // Create sample users
            User admin = userService.registerUser("admin", "admin@example.com", "admin123");
            // admin.setRole(UserRole.ADMIN);
            // userService.updateUser(admin.getId().toString(), admin.getUsername(), admin.getEmail());
            
            User seller1 = userService.registerUser("seller1", "seller1@example.com", "password123");
            User seller2 = userService.registerUser("seller2", "seller2@example.com", "password123");
            User bidder1 = userService.registerUser("bidder1", "bidder1@example.com", "password123");
            User bidder2 = userService.registerUser("bidder2", "bidder2@example.com", "password123");
            User bidder3 = userService.registerUser("bidder3", "bidder3@example.com", "password123");
            
            // Create sample auctions
            createSampleAuction("Vintage Guitar", "Beautiful vintage acoustic guitar from the 1960s", 
                              new BigDecimal("500.00"), seller1.getId().toString(), 2);
            
            createSampleAuction("Rare Book Collection", "First edition collection of classic literature", 
                              new BigDecimal("200.00"), seller2.getId().toString(), 3);
            
            createSampleAuction("Antique Watch", "Swiss-made pocket watch from 1890", 
                              new BigDecimal("800.00"), seller1.getId().toString(), 1);
            
            createSampleAuction("Art Painting", "Original oil painting by local artist", 
                              new BigDecimal("300.00"), seller2.getId().toString(), 4);
            
            createSampleAuction("Vintage Camera", "Leica camera from the 1950s", 
                              new BigDecimal("1200.00"), seller1.getId().toString(), 5);
            
            logger.info("Created {} sample users and {} sample auctions", 5, 5);
            
        } catch (Exception e) {
            logger.error("Failed to initialize sample data", e);
        }
    }
    
    private void createSampleAuction(String title, String description, BigDecimal startingPrice, 
                                   String sellerId, int hoursFromNow) {
        try {
            Auction auction = new Auction();
            auction.setTitle(title);
            auction.setDescription(description);
            auction.setStartingPrice(startingPrice);
            auction.setCurrentPrice(startingPrice);
            auction.setEndTime(LocalDateTime.now().plusHours(hoursFromNow));
            auction.setSellerId(UUID.fromString(sellerId));
            auction.setStatus("ACTIVE");
            
            auctionService.createAuction(auction);
            
        } catch (Exception e) {
            logger.error("Failed to create sample auction: {}", title, e);
        }
    }
}
