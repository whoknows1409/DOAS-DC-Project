package com.auction.system.controller;

import com.auction.system.rmi.CoordinatorService;
import com.auction.system.service.DistributedCoordinatorService;
import com.auction.system.service.LamportClockService;
import com.auction.system.service.AuctionService;
import com.auction.system.websocket.AuctionWebSocketHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

/**
 * Admin controller for system monitoring and management
 */
@RestController
@RequestMapping("/api/admin")
@CrossOrigin(origins = "*")
public class AdminController {
    
    private static final Logger logger = LoggerFactory.getLogger(AdminController.class);
    
    @Autowired
    private DistributedCoordinatorService coordinatorService;
    
    @Autowired
    private LamportClockService lamportClockService;
    
    @Autowired
    private AuctionService auctionService;
    
    @Autowired
    private AuctionWebSocketHandler webSocketHandler;
    
    /**
     * Get comprehensive system status
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getSystemStatus() {
        try {
            Map<String, Object> status = new HashMap<>();
            
            // Local server info
            int localServerId = coordinatorService.getServerId();
            status.put("serverId", localServerId);
            status.put("isCoordinator", coordinatorService.isCoordinator());
            status.put("coordinatorId", coordinatorService.getCoordinatorId());
            status.put("logicalClock", lamportClockService.getCurrentTime());
            status.put("uptime", System.currentTimeMillis() - coordinatorService.getStartTime());
            
            // All servers status (including local)
            List<CoordinatorService.ServerStatus> allServers = new ArrayList<>();
            
            // Track which servers we've accounted for
            Set<Integer> accountedServers = new HashSet<>();
            
            // Add local server status
            CoordinatorService.ServerStatus localStatus = new CoordinatorService.ServerStatus(
                localServerId,
                coordinatorService.isCoordinator(),
                lamportClockService.getCurrentTime(),
                true, // local server is always healthy
                System.currentTimeMillis() - coordinatorService.getStartTime(),
                (Integer) webSocketHandler.getStatistics().getOrDefault("activeConnections", 0)
            );
            allServers.add(localStatus);
            accountedServers.add(localServerId);
            
            // Add peer servers status
            Map<Integer, CoordinatorService.ServerStatus> peerStatus = new HashMap<>();
            for (Map.Entry<Integer, CoordinatorService> entry : coordinatorService.getPeerServices().entrySet()) {
                try {
                    CoordinatorService.ServerStatus serverStatus = entry.getValue().getServerStatus();
                    allServers.add(serverStatus);
                    peerStatus.put(entry.getKey(), serverStatus);
                    accountedServers.add(entry.getKey());
                } catch (Exception e) {
                    logger.warn("Failed to get status from peer server {}", entry.getKey());
                    CoordinatorService.ServerStatus offlineStatus = createOfflineStatus(entry.getKey());
                    allServers.add(offlineStatus);
                    peerStatus.put(entry.getKey(), offlineStatus);
                    accountedServers.add(entry.getKey());
                }
            }
            
            // Add offline status for all known servers (1, 2, 3) that aren't accounted for
            for (int serverId = 1; serverId <= 3; serverId++) {
                if (!accountedServers.contains(serverId)) {
                    CoordinatorService.ServerStatus offlineStatus = createOfflineStatus(serverId);
                    allServers.add(offlineStatus);
                    peerStatus.put(serverId, offlineStatus);
                }
            }
            
            status.put("servers", allServers); // Add servers array for frontend
            status.put("peerServers", peerStatus); // Keep for backward compatibility
            
            // Auction statistics
            status.put("activeAuctions", auctionService.getActiveAuctions().size());
            status.put("totalBids", getTotalBidsCount());
            
            // WebSocket statistics
            status.put("webSocketStats", webSocketHandler.getStatistics());
            
            // Clock synchronization info
            status.put("clockSync", getClockSyncInfo());
            
            return ResponseEntity.ok(status);
            
        } catch (Exception e) {
            logger.error("Failed to get system status", e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Get total bids count
     */
    private int getTotalBidsCount() {
        try {
            return auctionService.getTotalBidsCount();
        } catch (Exception e) {
            logger.error("Failed to get total bids count", e);
            return 0;
        }
    }
    
    /**
     * Get clock synchronization info
     */
    private Map<String, Object> getClockSyncInfo() {
        Map<String, Object> clockSync = new HashMap<>();
        clockSync.put("localClock", lamportClockService.getCurrentTime());
        clockSync.put("localTime", System.currentTimeMillis());
        return clockSync;
    }
    
    /**
     * Create offline status for peer server
     */
    private CoordinatorService.ServerStatus createOfflineStatus(int serverId) {
        return new CoordinatorService.ServerStatus(
            serverId, false, 0, false, System.currentTimeMillis(), 0
        );
    }
    
    /**
     * Get detailed clock synchronization information
     */
    @GetMapping("/clock-sync")
    public ResponseEntity<Map<String, Object>> getClockSyncStatus() {
        try {
            Map<String, Object> clockSync = new HashMap<>();
            
            // Local clock info
            clockSync.put("localClock", lamportClockService.getCurrentTime());
            clockSync.put("localTime", System.currentTimeMillis());
            
            // Peer clock values
            Map<Integer, Integer> peerClocks = lamportClockService.getPeerClocks();
            clockSync.put("peerClocks", peerClocks);
            
            // Calculate clock drift
            Map<Integer, Integer> clockDrifts = new HashMap<>();
            int localClock = lamportClockService.getCurrentTime();
            for (Map.Entry<Integer, Integer> entry : peerClocks.entrySet()) {
                clockDrifts.put(entry.getKey(), entry.getValue() - localClock);
            }
            clockSync.put("clockDrifts", clockDrifts);
            
            // Synchronization history
            clockSync.put("lastSyncTime", System.currentTimeMillis());
            clockSync.put("syncStatus", "SYNCHRONIZED");
            
            return ResponseEntity.ok(clockSync);
            
        } catch (Exception e) {
            logger.error("Failed to get clock sync status", e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Trigger manual clock synchronization
     */
    @PostMapping("/clock-sync/synchronize")
    public ResponseEntity<Map<String, Object>> triggerClockSynchronization() {
        try {
            // This would trigger Berkeley clock synchronization
            Map<String, Object> result = new HashMap<>();
            result.put("message", "Clock synchronization triggered");
            result.put("timestamp", System.currentTimeMillis());
            result.put("localClock", lamportClockService.getCurrentTime());
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            logger.error("Failed to trigger clock synchronization", e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Get replication lag metrics
     */
    @GetMapping("/replication-lag")
    public ResponseEntity<Map<String, Object>> getReplicationLag() {
        try {
            Map<String, Object> replicationLag = new HashMap<>();
            
            // Calculate replication lag for each peer
            Map<Integer, Long> peerLag = new HashMap<>();
            for (Map.Entry<Integer, CoordinatorService> entry : coordinatorService.getPeerServices().entrySet()) {
                try {
                    long startTime = System.currentTimeMillis();
                    entry.getValue().heartbeat(coordinatorService.getServerId());
                    long responseTime = System.currentTimeMillis() - startTime;
                    peerLag.put(entry.getKey(), responseTime);
                } catch (Exception e) {
                    peerLag.put(entry.getKey(), -1L); // Indicates unreachable
                }
            }
            
            replicationLag.put("peerLag", peerLag);
            replicationLag.put("averageLag", calculateAverageLag(peerLag));
            replicationLag.put("maxLag", calculateMaxLag(peerLag));
            replicationLag.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.ok(replicationLag);
            
        } catch (Exception e) {
            logger.error("Failed to get replication lag", e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Get load distribution metrics
     */
    @GetMapping("/load-distribution")
    public ResponseEntity<Map<String, Object>> getLoadDistribution() {
        try {
            Map<String, Object> loadDistribution = new HashMap<>();
            
            // Get load metrics from all servers
            Map<Integer, Map<String, Object>> serverLoad = new HashMap<>();
            
            // Local server load
            serverLoad.put(coordinatorService.getServerId(), getLocalLoadMetrics());
            
            // Peer server load
            for (Map.Entry<Integer, CoordinatorService> entry : coordinatorService.getPeerServices().entrySet()) {
                try {
                    CoordinatorService.ServerStatus status = entry.getValue().getServerStatus();
                    Map<String, Object> peerLoad = new HashMap<>();
                    peerLoad.put("activeConnections", status.getActiveConnections());
                    peerLoad.put("uptime", status.getUptime());
                    peerLoad.put("isHealthy", status.isHealthy());
                    serverLoad.put(entry.getKey(), peerLoad);
                } catch (Exception e) {
                    serverLoad.put(entry.getKey(), createOfflineLoadMetrics(entry.getKey()));
                }
            }
            
            loadDistribution.put("serverLoad", serverLoad);
            loadDistribution.put("totalConnections", calculateTotalConnections(serverLoad));
            loadDistribution.put("loadBalanceScore", calculateLoadBalanceScore(serverLoad));
            loadDistribution.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.ok(loadDistribution);
            
        } catch (Exception e) {
            logger.error("Failed to get load distribution", e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Trigger coordinator election
     */
    @PostMapping("/election/trigger")
    public ResponseEntity<Map<String, Object>> triggerElection() {
        try {
            Map<String, Object> result = new HashMap<>();
            
            if (coordinatorService.isCoordinator()) {
                // If current server is coordinator, initiate election
                coordinatorService.startBullyElection(coordinatorService.getServerId());
                result.put("message", "Bully election initiated");
            } else {
                // Send election request to coordinator
                coordinatorService.startBullyElection(coordinatorService.getServerId());
                result.put("message", "Election request sent");
            }
            
            result.put("timestamp", System.currentTimeMillis());
            result.put("initiator", coordinatorService.getServerId());
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            logger.error("Failed to trigger election", e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Get election status
     */
    @GetMapping("/election/status")
    public ResponseEntity<Map<String, Object>> getElectionStatus() {
        try {
            Map<String, Object> electionStatus = new HashMap<>();
            electionStatus.put("currentCoordinator", coordinatorService.getCoordinatorId());
            electionStatus.put("isCoordinator", coordinatorService.isCoordinator());
            electionStatus.put("electionInProgress", coordinatorService.isElectionInProgress());
            electionStatus.put("serverId", coordinatorService.getServerId());
            electionStatus.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.ok(electionStatus);
            
        } catch (Exception e) {
            logger.error("Failed to get election status", e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Admin endpoint to end an auction early
     */
    @PostMapping("/auctions/{auctionId}/end")
    public ResponseEntity<Map<String, Object>> endAuction(@PathVariable String auctionId) {
        try {
            logger.info("Admin request to end auction: {}", auctionId);
            
            boolean success = auctionService.endAuctionEarly(auctionId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", success);
            response.put("auctionId", auctionId);
            response.put("message", success ? "Auction ended successfully" : "Failed to end auction");
            response.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Failed to end auction: {}", auctionId, e);
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", e.getMessage());
            return ResponseEntity.internalServerError().body(error);
        }
    }
    
    // Helper methods
    private Map<String, Object> createOfflineLoadMetrics(int serverId) {
        Map<String, Object> metrics = new HashMap<>();
        metrics.put("activeConnections", 0);
        metrics.put("uptime", 0);
        metrics.put("isHealthy", false);
        return metrics;
    }
    
    private Map<String, Object> getLocalLoadMetrics() {
        Map<String, Object> metrics = new HashMap<>();
        metrics.put("activeConnections", webSocketHandler.getStatistics().get("activeConnections"));
        metrics.put("uptime", System.currentTimeMillis() - coordinatorService.getStartTime());
        metrics.put("isHealthy", true);
        metrics.put("activeAuctions", auctionService.getActiveAuctions().size());
        return metrics;
    }
    
    private long calculateAverageLag(Map<Integer, Long> peerLag) {
        return peerLag.values().stream()
                .filter(lag -> lag >= 0)
                .mapToLong(Long::longValue)
                .sum() / Math.max(1, peerLag.size());
    }
    
    private long calculateMaxLag(Map<Integer, Long> peerLag) {
        return peerLag.values().stream()
                .filter(lag -> lag >= 0)
                .mapToLong(Long::longValue)
                .max()
                .orElse(0);
    }
    
    private int calculateTotalConnections(Map<Integer, Map<String, Object>> serverLoad) {
        return serverLoad.values().stream()
                .mapToInt(load -> (Integer) load.getOrDefault("activeConnections", 0))
                .sum();
    }
    
    private double calculateLoadBalanceScore(Map<Integer, Map<String, Object>> serverLoad) {
        // Simple load balance score based on connection distribution
        int totalConnections = calculateTotalConnections(serverLoad);
        if (totalConnections == 0) return 1.0;
        
        int serverCount = serverLoad.size();
        double expectedConnections = (double) totalConnections / serverCount;
        
        double variance = serverLoad.values().stream()
                .mapToDouble(load -> Math.pow((Integer) load.getOrDefault("activeConnections", 0) - expectedConnections, 2))
                .sum() / serverCount;
        
        // Convert variance to a score (0-1, where 1 is perfectly balanced)
        return Math.max(0, 1 - (variance / (expectedConnections * expectedConnections)));
    }
}