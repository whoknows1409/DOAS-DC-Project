package com.auction.system.service;

import com.auction.system.rmi.CoordinatorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Implementation of distributed coordination service using RMI
 */
@Service
public class DistributedCoordinatorService extends UnicastRemoteObject implements CoordinatorService {
    
    private static final Logger logger = LoggerFactory.getLogger(DistributedCoordinatorService.class);
    
    @Value("${app.server.id}")
    private int serverId;
    
    @Value("${app.server.rmi.port}")
    private int rmiPort;
    
    @Value("${app.server.peers}")
    private String peerServers;
    
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    
    @Autowired
    private TwoPhaseCommitService twoPhaseCommitService;
    
    @Autowired
    private LamportClockService lamportClockService;
    
    @Autowired
    private AuctionService auctionService;
    
    private Registry registry;
    private Map<Integer, CoordinatorService> peerServices = new ConcurrentHashMap<>();
    private final AtomicInteger logicalClock = new AtomicInteger(0);
    private volatile boolean isCoordinator = false;
    private volatile int coordinatorId = -1;
    private final ExecutorService executorService = Executors.newFixedThreadPool(10);
    private final Map<String, TransactionState> transactions = new ConcurrentHashMap<>();
    private long startTime = System.currentTimeMillis();
    
    // Election state
    private volatile boolean electionInProgress = false;
    private final Set<Integer> electionParticipants = ConcurrentHashMap.newKeySet();
    
    public DistributedCoordinatorService() throws RemoteException {
        super();
    }
    
    @PostConstruct
    public void initialize() {
        try {
            // Start RMI registry
            registry = LocateRegistry.createRegistry(rmiPort);
            registry.rebind("CoordinatorService", this);
            logger.info("RMI CoordinatorService bound on port {} for server {}", rmiPort, serverId);
            
            // Connect to peer servers
            connectToPeers();
            
            // Start heartbeat monitoring
            startHeartbeatMonitoring();
            
            // Start election if no coordinator found
            discoverCoordinator();
            
        } catch (Exception e) {
            logger.error("Failed to initialize RMI service", e);
        }
    }
    
    @PreDestroy
    public void cleanup() {
        try {
            if (registry != null) {
                registry.unbind("CoordinatorService");
                UnicastRemoteObject.unexportObject(this, true);
            }
            executorService.shutdown();
        } catch (Exception e) {
            logger.error("Error during cleanup", e);
        }
    }
    
    private void connectToPeers() {
        String[] peers = peerServers.split(",");
        for (String peer : peers) {
            String[] parts = peer.trim().split(":");
            if (parts.length == 2) {
                try {
                    String host = parts[0];
                    int port = Integer.parseInt(parts[1]);
                    
                    // Extract server ID from hostname (e.g., auction-server-2 -> 2)
                    int peerId = extractServerIdFromHost(host, port);
                    
                    // Skip if trying to connect to self
                    if (peerId == serverId) {
                        continue;
                    }
                    
                    // Skip if already connected
                    if (peerServices.containsKey(peerId)) {
                        continue;
                    }
                    
                    Registry peerRegistry = LocateRegistry.getRegistry(host, port);
                    CoordinatorService peerService = (CoordinatorService) peerRegistry.lookup("CoordinatorService");
                    
                    peerServices.put(peerId, peerService);
                    logger.info("Connected to peer server {} at {}:{}", peerId, host, port);
                } catch (Exception e) {
                    logger.debug("Failed to connect to peer: {} (will retry)", peer);
                }
            }
        }
    }
    
    private int extractServerIdFromHost(String host, int port) {
        // Try to extract from hostname (e.g., "auction-server-2" -> 2)
        try {
            String[] hostParts = host.split("-");
            for (String part : hostParts) {
                if (part.matches("\\d+")) {
                    int id = Integer.parseInt(part);
                    if (id != serverId) {
                        return id;
                    }
                }
            }
        } catch (Exception e) {
            // Fall through to port-based mapping
        }
        
        // Fallback: map port to server ID (1101->1, 1102->2, 1103->3)
        if (port >= 1101 && port <= 1103) {
            return port - 1100;
        }
        
        return port;
    }
    
    private void discoverCoordinator() {
        executorService.submit(() -> {
            try {
                Thread.sleep(2000); // Wait for initial connections
                
                boolean coordinatorFound = false;
                for (Map.Entry<Integer, CoordinatorService> entry : peerServices.entrySet()) {
                    try {
                        HeartbeatResponse response = entry.getValue().heartbeat(serverId);
                        if (response.isCoordinator()) {
                            coordinatorId = entry.getKey();
                            coordinatorFound = true;
                            logger.info("Discovered coordinator: {}", coordinatorId);
                            break;
                        }
                    } catch (RemoteException e) {
                        logger.warn("Failed to check coordinator status for peer {}", entry.getKey());
                    }
                }
                
                if (!coordinatorFound && !electionInProgress) {
                    logger.info("No coordinator found, starting election");
                    try {
                        startBullyElection(serverId);
                    } catch (RemoteException e) {
                        logger.error("Failed to start bully election", e);
                    }
                }
                
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
    }
    
    private void startHeartbeatMonitoring() {
        executorService.submit(() -> {
            int reconnectCounter = 0;
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    Thread.sleep(5000);
                    
                    // Retry connecting to missing peers every 10 seconds (every 2nd heartbeat)
                    reconnectCounter++;
                    if (reconnectCounter % 2 == 0) {
                        connectToPeers();
                    }
                    
                    checkCoordinatorHealth();
                    checkPeerHealth();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        });
    }
    
    private void checkPeerHealth() {
        // Check health of all peers and remove dead connections
        List<Integer> deadPeers = new ArrayList<>();
        for (Map.Entry<Integer, CoordinatorService> entry : peerServices.entrySet()) {
            try {
                HeartbeatResponse response = entry.getValue().heartbeat(serverId);
                if (!response.isAlive()) {
                    logger.warn("Peer server {} is not responding", entry.getKey());
                    deadPeers.add(entry.getKey());
                }
            } catch (Exception e) {
                logger.debug("Failed to contact peer server {}, will retry connection", entry.getKey());
                deadPeers.add(entry.getKey());
            }
        }
        
        // Remove dead peers so they can be reconnected
        for (Integer peerId : deadPeers) {
            peerServices.remove(peerId);
            logger.info("Removed dead peer {} from peer list", peerId);
        }
    }
    
    private void checkCoordinatorHealth() {
        if (coordinatorId != -1 && coordinatorId != serverId) {
            try {
                CoordinatorService coordinator = peerServices.get(coordinatorId);
                if (coordinator != null) {
                    HeartbeatResponse response = coordinator.heartbeat(serverId);
                    if (!response.isAlive()) {
                        logger.warn("Coordinator {} is not responding, starting election", coordinatorId);
                        try {
                            startBullyElection(serverId);
                        } catch (RemoteException e) {
                            logger.error("Failed to start bully election", e);
                        }
                    }
                } else {
                    logger.warn("Coordinator service not found, starting election");
                    try {
                        startBullyElection(serverId);
                    } catch (RemoteException e) {
                        logger.error("Failed to start bully election", e);
                    }
                }
            } catch (RemoteException e) {
                logger.warn("Coordinator {} heartbeat failed, starting election", coordinatorId);
                try {
                    startBullyElection(serverId);
                } catch (RemoteException ex) {
                    logger.error("Failed to start bully election", ex);
                }
            }
        }
    }
    
    @Override
    public BidResponse processBid(BidRequest request) throws RemoteException {
        int timestamp = lamportClockService.incrementAndGet();
        logger.info("Processing bid request: auctionId={}, amount={}, timestamp={}", 
                   request.getAuctionId(), request.getAmount(), timestamp);
        
        try {
            // Use Two-Phase Commit for bid processing
            String transactionId = UUID.randomUUID().toString();
            List<Operation> operations = Arrays.asList(
                new Operation("INSERT", "bids", request.getAuctionId(), 
                             Map.of("auctionId", request.getAuctionId(),
                                   "bidderId", request.getBidderId(),
                                   "amount", request.getAmount(),
                                   "timestamp", new Date(),
                                   "logicalTimestamp", timestamp,
                                   "serverId", serverId))
            );
            
            boolean prepared = twoPhaseCommitService.executeTransaction(transactionId, operations);
            
            if (prepared) {
                // Notify WebSocket clients
                notifyBidUpdate(request, timestamp);
                return new BidResponse(true, "Bid processed successfully", timestamp);
            } else {
                return new BidResponse(false, "Failed to process bid", timestamp);
            }
            
        } catch (Exception e) {
            logger.error("Error processing bid", e);
            return new BidResponse(false, "Error processing bid: " + e.getMessage(), timestamp);
        }
    }
    
    @Override
    public boolean prepare(String transactionId, List<Operation> operations) throws RemoteException {
        return twoPhaseCommitService.prepare(transactionId, operations);
    }
    
    @Override
    public boolean commit(String transactionId) throws RemoteException {
        return twoPhaseCommitService.commit(transactionId);
    }
    
    @Override
    public boolean abort(String transactionId) throws RemoteException {
        return twoPhaseCommitService.abort(transactionId);
    }
    
    @Override
    public ClockSyncResponse synchronizeClocks(int localTime, int requestingServerId) throws RemoteException {
        return lamportClockService.synchronizeClock(localTime, requestingServerId);
    }
    
    @Override
    public HeartbeatResponse heartbeat(int fromServerId) throws RemoteException {
        int timestamp = lamportClockService.incrementAndGet();
        return new HeartbeatResponse(true, timestamp, isCoordinator);
    }
    
    @Override
    public void startBullyElection(int initiatorId) throws RemoteException {
        if (electionInProgress) {
            return;
        }
        
        electionInProgress = true;
        electionParticipants.clear();
        electionParticipants.add(initiatorId);
        
        logger.info("Starting bully election initiated by server {}", initiatorId);
        
        // Send election messages to higher ID servers
        List<Integer> higherIdServers = peerServices.keySet().stream()
                .filter(id -> id > serverId)
                .sorted()
                .toList();
        
        if (higherIdServers.isEmpty()) {
            // No higher ID servers, become coordinator
            becomeCoordinator();
        } else {
            // Send election messages to higher ID servers
            for (int higherId : higherIdServers) {
                try {
                    peerServices.get(higherId).handleElectionMessage(serverId, initiatorId);
                } catch (RemoteException e) {
                    logger.warn("Failed to send election message to server {}", higherId);
                }
            }
            
            // Wait for responses
            executorService.submit(() -> {
                try {
                    Thread.sleep(5000);
                    if (electionInProgress) {
                        becomeCoordinator();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
        }
    }
    
    @Override
    public void handleElectionMessage(int candidateId, int senderId) throws RemoteException {
        logger.info("Received election message from server {} for candidate {}", senderId, candidateId);
        
        if (candidateId < serverId) {
            // Send OK message and start new election
            try {
                peerServices.get(senderId).heartbeat(serverId); // Send OK as heartbeat
                startBullyElection(serverId);
            } catch (RemoteException e) {
                logger.warn("Failed to respond to election message from {}", senderId);
            }
        }
    }
    
    @Override
    public void handleCoordinatorMessage(int newCoordinatorId) throws RemoteException {
        logger.info("Received coordinator message: new coordinator is {}", newCoordinatorId);
        coordinatorId = newCoordinatorId;
        isCoordinator = (newCoordinatorId == serverId);
        electionInProgress = false;
        electionParticipants.clear();
    }
    
    @Override
    public void startRingElection(int initiatorId) throws RemoteException {
        logger.info("Starting ring election initiated by server {}", initiatorId);
        
        ElectionToken token = new ElectionToken(serverId, new ArrayList<>(), true);
        token.getParticipants().add(serverId);
        
        // Pass token to next server in ring
        passRingToken(token);
    }
    
    @Override
    public void handleRingToken(ElectionToken token) throws RemoteException {
        if (!token.isActive()) {
            return;
        }
        
        token.getParticipants().add(serverId);
        
        // Check if token has completed the ring
        if (token.getCandidateId() == serverId && token.getParticipants().size() > 1) {
            // Election complete, select highest ID as coordinator
            int newCoordinator = token.getParticipants().stream()
                    .mapToInt(Integer::intValue)
                    .max()
                    .orElse(serverId);
            
            token.setActive(false);
            announceCoordinator(newCoordinator);
        } else {
            // Pass token to next server
            passRingToken(token);
        }
    }
    
    private void passRingToken(ElectionToken token) {
        // Find next server in ring (simple implementation)
        List<Integer> allServers = new ArrayList<>(peerServices.keySet());
        allServers.add(serverId);
        Collections.sort(allServers);
        
        int currentIndex = allServers.indexOf(serverId);
        int nextIndex = (currentIndex + 1) % allServers.size();
        int nextServerId = allServers.get(nextIndex);
        
        if (nextServerId != serverId) {
            try {
                peerServices.get(nextServerId).handleRingToken(token);
            } catch (RemoteException e) {
                logger.warn("Failed to pass ring token to server {}", nextServerId);
                // Try next server
                passRingToken(token);
            }
        }
    }
    
    private void becomeCoordinator() {
        if (!electionInProgress) {
            return;
        }
        
        isCoordinator = true;
        coordinatorId = serverId;
        electionInProgress = false;
        
        logger.info("Server {} became coordinator", serverId);
        
        // Announce coordinator to all peers
        for (Map.Entry<Integer, CoordinatorService> entry : peerServices.entrySet()) {
            try {
                entry.getValue().handleCoordinatorMessage(serverId);
            } catch (RemoteException e) {
                logger.warn("Failed to announce coordinator to server {}", entry.getKey());
            }
        }
    }
    
    private void announceCoordinator(int coordinatorId) {
        logger.info("Announcing coordinator: {}", coordinatorId);
        
        for (Map.Entry<Integer, CoordinatorService> entry : peerServices.entrySet()) {
            try {
                entry.getValue().handleCoordinatorMessage(coordinatorId);
            } catch (RemoteException e) {
                logger.warn("Failed to announce coordinator to server {}", entry.getKey());
            }
        }
    }
    
    @Override
    public boolean replicateData(ReplicationRequest request) throws RemoteException {
        try {
            // Apply replication with logical timestamp
            lamportClockService.updateClock(request.getLogicalTimestamp());
            
            // Store in Redis for immediate consistency
            String redisKey = "replication:" + request.getOperationId();
            redisTemplate.opsForValue().set(redisKey, request);
            
            logger.info("Replicated operation {} from server {}", 
                       request.getOperationId(), request.getLogicalTimestamp());
            return true;
        } catch (Exception e) {
            logger.error("Failed to replicate data", e);
            return false;
        }
    }
    
    @Override
    public ServerStatus getServerStatus() throws RemoteException {
        long uptime = System.currentTimeMillis() - startTime;
        int activeConnections = getActiveConnections();
        
        return new ServerStatus(
            serverId,
            isCoordinator,
            logicalClock.get(),
            true, // isHealthy
            uptime,
            activeConnections
        );
    }
    
    private int getActiveConnections() {
        // Implementation would track active WebSocket connections
        return 0; // Placeholder
    }
    
    public Map<Integer, CoordinatorService> getPeerServices() {
        return new ConcurrentHashMap<>(peerServices);
    }
    
    public CoordinatorService getPeerService(int serverId) {
        return peerServices.get(serverId);
    }
    
    public int getServerId() {
        return serverId;
    }
    
    public boolean isCoordinator() {
        return isCoordinator;
    }
    
    public int getCoordinatorId() {
        return coordinatorId;
    }
    
    public long getStartTime() {
        return startTime;
    }
    
    public int getLogicalClock() {
        return logicalClock.get();
    }
    
    public boolean isElectionInProgress() {
        return electionInProgress;
    }
    
    private void notifyBidUpdate(BidRequest request, int timestamp) {
        // Implementation would notify WebSocket clients
        logger.info("Notifying bid update: auctionId={}, amount={}, timestamp={}", 
                   request.getAuctionId(), request.getAmount(), timestamp);
    }
    
    // Scheduled tasks
    @Scheduled(fixedDelay = 30000)
    public void performClockSynchronization() {
        if (!isCoordinator) {
            return;
        }
        
        try {
            BerkeleyClockSynchronization.syncClocks(serverId, peerServices, logicalClock);
        } catch (Exception e) {
            logger.error("Clock synchronization failed", e);
        }
    }
    
    @Scheduled(fixedDelay = 60000)
    public void performHealthCheck() {
        for (Map.Entry<Integer, CoordinatorService> entry : peerServices.entrySet()) {
            try {
                HeartbeatResponse response = entry.getValue().heartbeat(serverId);
                if (!response.isAlive()) {
                    logger.warn("Server {} is not responding", entry.getKey());
                }
            } catch (RemoteException e) {
                logger.warn("Health check failed for server {}", entry.getKey());
            }
        }
    }
    
    // Inner class for transaction state
    private static class TransactionState {
        private String transactionId;
        private Map<Integer, Boolean> preparedResponses;
        private Map<Integer, Boolean> commitResponses;
        private long timestamp;
        
        public TransactionState(String transactionId, int participantCount) {
            this.transactionId = transactionId;
            this.preparedResponses = new ConcurrentHashMap<>();
            this.commitResponses = new ConcurrentHashMap<>();
            this.timestamp = System.currentTimeMillis();
        }
        
        public boolean allPrepared() {
            return preparedResponses.values().stream().allMatch(Boolean::booleanValue);
        }
        
        public boolean allCommitted() {
            return commitResponses.values().stream().allMatch(Boolean::booleanValue);
        }
    }
}