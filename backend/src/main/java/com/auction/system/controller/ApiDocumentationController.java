package com.auction.system.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.CrossOrigin;

import java.util.Map;

/**
 * Controller to provide API documentation and endpoints information
 */
@RestController
@RequestMapping("/api/docs")
@CrossOrigin(origins = "*")
public class ApiDocumentationController {
    
    @GetMapping("/endpoints")
    public Map<String, Object> getApiEndpoints() {
        return Map.of(
            "userEndpoints", Map.of(
                "POST /api/users/register", "Register a new user",
                "POST /api/users/login", "Login user",
                "GET /api/users", "Get all users",
                "GET /api/users/{userId}", "Get user by ID",
                "GET /api/users/username/{username}", "Get user by username",
                "PUT /api/users/{userId}", "Update user",
                "DELETE /api/users/{userId}", "Delete user",
                "GET /api/users/{userId}/auctions", "Get user's auctions",
                "GET /api/users/{userId}/bids", "Get user's bids"
            ),
            "auctionEndpoints", Map.of(
                "GET /api/auctions", "Get all auctions",
                "GET /api/auctions/active", "Get active auctions",
                "GET /api/auctions/{auctionId}", "Get auction by ID",
                "GET /api/auctions/seller/{sellerId}", "Get auctions by seller",
                "POST /api/auctions", "Create new auction",
                "POST /api/auctions/{auctionId}/bids", "Place bid on auction",
                "GET /api/auctions/{auctionId}/bids", "Get bids for auction",
                "POST /api/auctions/{auctionId}/end", "End auction",
                "GET /api/auctions/status", "Get server status"
            ),
            "adminEndpoints", Map.of(
                "GET /api/admin/status", "Get comprehensive system status",
                "GET /api/admin/clock-sync", "Get clock synchronization status",
                "GET /api/admin/peers", "Get peer server status",
                "POST /api/admin/trigger-election", "Trigger coordinator election",
                "GET /api/admin/metrics", "Get system metrics"
            ),
            "websocketEndpoints", Map.of(
                "WS /ws", "WebSocket connection for real-time updates",
                "Message: /app/bid", "Send bid message",
                "Subscribe: /queue/bid-response", "Receive bid responses",
                "Subscribe: /topic/auction-updates", "Receive auction updates"
            )
        );
    }
    
    @GetMapping("/system-info")
    public Map<String, Object> getSystemInfo() {
        return Map.of(
            "systemName", "Distributed Auction System",
            "version", "1.0.0",
            "description", "A distributed auction system with RMI coordination, Two-Phase Commit, and Bully Election algorithms",
            "features", new String[]{
                "Distributed Architecture",
                "RMI Communication",
                "Two-Phase Commit Protocol",
                "Bully Election Algorithm",
                "Lamport Logical Clocks",
                "Real-time WebSocket Updates",
                "Redis Caching",
                "PostgreSQL Database",
                "Load Balancing with Nginx"
            },
            "servers", new String[]{
                "Server 1: http://localhost:8081",
                "Server 2: http://localhost:8082", 
                "Server 3: http://localhost:8083"
            },
            "frontend", "http://localhost"
        );
    }
}
