package com.auction.system.service;

import com.auction.system.rmi.CoordinatorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Lamport logical clock implementation
 */
@Service
public class LamportClockService {
    
    private static final Logger logger = LoggerFactory.getLogger(LamportClockService.class);
    
    @Value("${app.server.id}")
    private int serverId;
    
    @Autowired
    private DistributedCoordinatorService coordinatorService;
    
    private final AtomicInteger logicalClock = new AtomicInteger(0);
    private final Map<Integer, Integer> peerClocks = new ConcurrentHashMap<>();
    
    /**
     * Increment the logical clock and return the new value
     */
    public int incrementAndGet() {
        int newValue = logicalClock.incrementAndGet();
        logger.debug("Incremented logical clock to {}", newValue);
        return newValue;
    }
    
    /**
     * Update the logical clock based on received timestamp
     */
    public void updateClock(int receivedTimestamp) {
        int currentClock = logicalClock.get();
        int newClock = Math.max(currentClock, receivedTimestamp) + 1;
        logicalClock.set(newClock);
        logger.debug("Updated logical clock from {} to {} based on received timestamp {}", 
                    currentClock, newClock, receivedTimestamp);
    }
    
    /**
     * Get current logical clock value
     */
    public int getCurrentTime() {
        return logicalClock.get();
    }
    
    /**
     * Synchronize clocks using Berkeley algorithm
     */
    public CoordinatorService.ClockSyncResponse synchronizeClock(int localTime, int requestingServerId) {
        try {
            // Store peer's clock value
            peerClocks.put(requestingServerId, localTime);
            
            // If this server is coordinator, calculate time adjustment
            if (coordinatorService.isCoordinator()) {
                return calculateTimeAdjustment(localTime, requestingServerId);
            }
            
            // Otherwise, return current time
            return new CoordinatorService.ClockSyncResponse(logicalClock.get(), true);
            
        } catch (Exception e) {
            logger.error("Clock synchronization failed", e);
            return new CoordinatorService.ClockSyncResponse(logicalClock.get(), false);
        }
    }
    
    /**
     * Synchronize clocks using Berkeley algorithm (RMI interface method)
     */
    public CoordinatorService.ClockSyncResponse synchronizeClocks(int localTime, int requestingServerId) {
        return synchronizeClock(localTime, requestingServerId);
    }
    
    private CoordinatorService.ClockSyncResponse calculateTimeAdjustment(int peerTime, int peerId) {
        // Berkeley algorithm: calculate average time difference
        int currentTime = logicalClock.get();
        int timeDifference = peerTime - currentTime;
        
        // Store all peer times for averaging
        Map<Integer, Integer> allTimes = new ConcurrentHashMap<>(peerClocks);
        allTimes.put(serverId, currentTime);
        
        // Calculate average time
        double averageTime = allTimes.values().stream()
                .mapToInt(Integer::intValue)
                .average()
                .orElse(currentTime);
        
        int adjustedTime = (int) Math.round(averageTime);
        
        logger.info("Berkeley clock sync: server {} time={}, average={}, adjusted={}", 
                   peerId, peerTime, averageTime, adjustedTime);
        
        return new CoordinatorService.ClockSyncResponse(adjustedTime, true);
    }
    
    /**
     * Get all peer clock values
     */
    public Map<Integer, Integer> getPeerClocks() {
        return new ConcurrentHashMap<>(peerClocks);
    }
    
    /**
     * Reset the logical clock (for testing/recovery)
     */
    public void reset() {
        logicalClock.set(0);
        peerClocks.clear();
        logger.info("Logical clock reset");
    }
}

/**
 * Berkeley clock synchronization algorithm implementation
 */
class BerkeleyClockSynchronization {
    
    private static final Logger logger = LoggerFactory.getLogger(BerkeleyClockSynchronization.class);
    
    public static void syncClocks(int coordinatorId, 
                                 Map<Integer, CoordinatorService> peerServices,
                                 AtomicInteger logicalClock) {
        
        try {
            logger.info("Starting Berkeley clock synchronization from coordinator {}", coordinatorId);
            
            // Collect time from all peers
            Map<Integer, Integer> peerTimes = new ConcurrentHashMap<>();
            Map<Integer, Integer> timeAdjustments = new ConcurrentHashMap<>();
            
            // Get coordinator's current time
            int coordinatorTime = logicalClock.get();
            peerTimes.put(coordinatorId, coordinatorTime);
            
            // Request time from all peers
            for (Map.Entry<Integer, CoordinatorService> entry : peerServices.entrySet()) {
                try {
                    CoordinatorService.ClockSyncResponse response = 
                        entry.getValue().synchronizeClocks(coordinatorTime, coordinatorId);
                    
                    if (response.isSuccess()) {
                        peerTimes.put(entry.getKey(), response.getAdjustedTime());
                        logger.debug("Received time from server {}: {}", entry.getKey(), response.getAdjustedTime());
                    }
                } catch (Exception e) {
                    logger.warn("Failed to get time from server {}", entry.getKey());
                }
            }
            
            // Calculate average time
            double averageTime = peerTimes.values().stream()
                    .mapToInt(Integer::intValue)
                    .average()
                    .orElse(coordinatorTime);
            
            int synchronizedTime = (int) Math.round(averageTime);
            
            // Calculate adjustments for each peer
            for (Map.Entry<Integer, Integer> entry : peerTimes.entrySet()) {
                int adjustment = synchronizedTime - entry.getValue();
                timeAdjustments.put(entry.getKey(), adjustment);
            }
            
            // Send adjustments to peers
            for (Map.Entry<Integer, CoordinatorService> entry : peerServices.entrySet()) {
                try {
                    int adjustment = timeAdjustments.getOrDefault(entry.getKey(), 0);
                    CoordinatorService.ClockSyncResponse response = 
                        entry.getValue().synchronizeClocks(synchronizedTime + adjustment, coordinatorId);
                    
                    if (response.isSuccess()) {
                        logger.info("Clock adjustment sent to server {}: {} ms", entry.getKey(), adjustment);
                    }
                } catch (Exception e) {
                    logger.warn("Failed to send clock adjustment to server {}", entry.getKey());
                }
            }
            
            // Update coordinator's own clock
            int coordinatorAdjustment = timeAdjustments.get(coordinatorId);
            logicalClock.set(synchronizedTime + coordinatorAdjustment);
            
            logger.info("Berkeley clock synchronization completed. Synchronized time: {}", synchronizedTime);
            
        } catch (Exception e) {
            logger.error("Berkeley clock synchronization failed", e);
        }
    }
}