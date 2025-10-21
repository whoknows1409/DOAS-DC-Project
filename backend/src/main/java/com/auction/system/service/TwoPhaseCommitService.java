package com.auction.system.service;

import com.auction.system.rmi.CoordinatorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Two-Phase Commit implementation for distributed transaction management
 */
@Service
public class TwoPhaseCommitService {
    
    private static final Logger logger = LoggerFactory.getLogger(TwoPhaseCommitService.class);
    
    @Value("${app.server.id}")
    private int serverId;
    
    @Value("${app.coordination.replication.timeout}")
    private int replicationTimeout;
    
    @Autowired
    private DistributedCoordinatorService coordinatorService;
    
    @Autowired
    private AuctionService auctionService;
    
    private final ExecutorService executorService = Executors.newFixedThreadPool(20);
    private final Map<String, TransactionState> transactions = new ConcurrentHashMap<>();
    private final Map<String, Set<Integer>> transactionParticipants = new ConcurrentHashMap<>();
    
    /**
     * Execute a distributed transaction using Two-Phase Commit
     */
    public boolean executeTransaction(String transactionId, List<CoordinatorService.Operation> operations) {
        try {
            logger.info("Executing 2PC transaction: {}", transactionId);
            
            // Initialize transaction state
            TransactionState state = new TransactionState(transactionId, operations);
            transactions.put(transactionId, state);
            
            // Phase 1: Prepare
            boolean prepared = preparePhase(transactionId, operations);
            
            if (prepared) {
                // Phase 2: Commit
                boolean committed = commitPhase(transactionId);
                cleanupTransaction(transactionId);
                return committed;
            } else {
                // Abort transaction
                abortPhase(transactionId);
                cleanupTransaction(transactionId);
                return false;
            }
            
        } catch (Exception e) {
            logger.error("Transaction execution failed: {}", transactionId, e);
            abortPhase(transactionId);
            cleanupTransaction(transactionId);
            return false;
        }
    }
    
    /**
     * Prepare phase - ask all participants if they can commit
     */
    public boolean prepare(String transactionId, List<CoordinatorService.Operation> operations) {
        try {
            logger.info("Prepare phase for transaction: {}", transactionId);
            
            TransactionState state = transactions.computeIfAbsent(transactionId, 
                k -> new TransactionState(k, operations));
            
            // Check if we can prepare locally
            boolean canPrepare = canPrepareLocally(operations);
            state.setLocalPrepared(canPrepare);
            
            if (!canPrepare) {
                logger.warn("Local prepare failed for transaction: {}", transactionId);
                return false;
            }
            
            // Store operations for potential commit/abort
            state.setOperations(operations);
            
            // Write prepare record to transaction log
            writeToTransactionLog(transactionId, "PREPARED", operations);
            
            logger.info("Local prepare successful for transaction: {}", transactionId);
            return true;
            
        } catch (Exception e) {
            logger.error("Prepare phase failed for transaction: {}", transactionId, e);
            return false;
        }
    }
    
    /**
     * Commit phase - commit the transaction on all participants
     */
    public boolean commit(String transactionId) {
        try {
            logger.info("Commit phase for transaction: {}", transactionId);
            
            TransactionState state = transactions.get(transactionId);
            if (state == null) {
                logger.error("Transaction state not found: {}", transactionId);
                return false;
            }
            
            // Commit locally
            boolean committed = commitLocally(state.getOperations());
            state.setLocalCommitted(committed);
            
            if (!committed) {
                logger.error("Local commit failed for transaction: {}", transactionId);
                return false;
            }
            
            // Write commit record to transaction log
            writeToTransactionLog(transactionId, "COMMITTED", state.getOperations());
            
            logger.info("Local commit successful for transaction: {}", transactionId);
            return true;
            
        } catch (Exception e) {
            logger.error("Commit phase failed for transaction: {}", transactionId, e);
            return false;
        }
    }
    
    /**
     * Abort phase - abort the transaction on all participants
     */
    public boolean abort(String transactionId) {
        try {
            logger.info("Abort phase for transaction: {}", transactionId);
            
            TransactionState state = transactions.get(transactionId);
            if (state == null) {
                logger.warn("Transaction state not found for abort: {}", transactionId);
                return true; // Already cleaned up
            }
            
            // Abort locally
            abortLocally(state.getOperations());
            state.setLocalAborted(true);
            
            // Write abort record to transaction log
            writeToTransactionLog(transactionId, "ABORTED", state.getOperations());
            
            logger.info("Local abort successful for transaction: {}", transactionId);
            return true;
            
        } catch (Exception e) {
            logger.error("Abort phase failed for transaction: {}", transactionId, e);
            return false;
        }
    }
    
    private boolean preparePhase(String transactionId, List<CoordinatorService.Operation> operations) {
        // Get all participants (including self)
        Set<Integer> participants = getParticipants();
        transactionParticipants.put(transactionId, participants);
        
        // Prepare locally first
        boolean localPrepared = prepare(transactionId, operations);
        if (!localPrepared) {
            return false;
        }
        
        // Prepare on remote participants
        List<Future<Boolean>> futures = new ArrayList<>();
        
        for (Integer participantId : participants) {
            if (participantId != serverId) {
                Future<Boolean> future = executorService.submit(() -> {
                    try {
                        CoordinatorService peer = coordinatorService.getPeerService(participantId);
                        return peer.prepare(transactionId, operations);
                    } catch (Exception e) {
                        logger.error("Prepare failed on participant {}", participantId, e);
                        return false;
                    }
                });
                futures.add(future);
            }
        }
        
        // Wait for all prepare responses
        boolean allPrepared = true;
        for (Future<Boolean> future : futures) {
            try {
                Boolean prepared = future.get(replicationTimeout, TimeUnit.MILLISECONDS);
                if (!prepared) {
                    allPrepared = false;
                    break;
                }
            } catch (TimeoutException e) {
                logger.error("Prepare timeout for transaction: {}", transactionId);
                allPrepared = false;
                break;
            } catch (Exception e) {
                logger.error("Prepare response error for transaction: {}", transactionId, e);
                allPrepared = false;
                break;
            }
        }
        
        logger.info("Prepare phase completed for transaction {}: {}", transactionId, allPrepared);
        return allPrepared;
    }
    
    private boolean commitPhase(String transactionId) {
        Set<Integer> participants = transactionParticipants.get(transactionId);
        if (participants == null) {
            logger.error("No participants found for transaction: {}", transactionId);
            return false;
        }
        
        // Commit locally first
        boolean localCommitted = commit(transactionId);
        if (!localCommitted) {
            return false;
        }
        
        // Commit on remote participants
        List<Future<Boolean>> futures = new ArrayList<>();
        
        for (Integer participantId : participants) {
            if (participantId != serverId) {
                Future<Boolean> future = executorService.submit(() -> {
                    try {
                        CoordinatorService peer = coordinatorService.getPeerService(participantId);
                        return peer.commit(transactionId);
                    } catch (Exception e) {
                        logger.error("Commit failed on participant {}", participantId, e);
                        return false;
                    }
                });
                futures.add(future);
            }
        }
        
        // Wait for all commit responses
        boolean allCommitted = true;
        for (Future<Boolean> future : futures) {
            try {
                Boolean committed = future.get(replicationTimeout, TimeUnit.MILLISECONDS);
                if (!committed) {
                    allCommitted = false;
                    // Continue trying to commit on others even if one fails
                    logger.warn("Some participants failed to commit for transaction: {}", transactionId);
                }
            } catch (TimeoutException e) {
                logger.error("Commit timeout for transaction: {}", transactionId);
                allCommitted = false;
            } catch (Exception e) {
                logger.error("Commit response error for transaction: {}", transactionId, e);
                allCommitted = false;
            }
        }
        
        logger.info("Commit phase completed for transaction {}: {}", transactionId, allCommitted);
        return allCommitted;
    }
    
    private void abortPhase(String transactionId) {
        Set<Integer> participants = transactionParticipants.get(transactionId);
        if (participants == null) {
            return;
        }
        
        // Abort locally first
        abort(transactionId);
        
        // Abort on remote participants (fire and forget)
        for (Integer participantId : participants) {
            if (participantId != serverId) {
                executorService.submit(() -> {
                    try {
                        CoordinatorService peer = coordinatorService.getPeerService(participantId);
                        peer.abort(transactionId);
                    } catch (Exception e) {
                        logger.error("Abort failed on participant {}", participantId, e);
                    }
                });
            }
        }
        
        logger.info("Abort phase completed for transaction: {}", transactionId);
    }
    
    private boolean canPrepareLocally(List<CoordinatorService.Operation> operations) {
        try {
            // Validate operations
            for (CoordinatorService.Operation operation : operations) {
                if (!validateOperation(operation)) {
                    return false;
                }
            }
            
            // Check resource availability (locks, etc.)
            return checkResourceAvailability(operations);
            
        } catch (Exception e) {
            logger.error("Local prepare validation failed", e);
            return false;
        }
    }
    
    private boolean commitLocally(List<CoordinatorService.Operation> operations) {
        try {
            for (CoordinatorService.Operation operation : operations) {
                executeOperation(operation);
            }
            return true;
        } catch (Exception e) {
            logger.error("Local commit failed", e);
            return false;
        }
    }
    
    private void abortLocally(List<CoordinatorService.Operation> operations) {
        try {
            // Release any acquired resources
            releaseResources(operations);
        } catch (Exception e) {
            logger.error("Local abort cleanup failed", e);
        }
    }
    
    private boolean validateOperation(CoordinatorService.Operation operation) {
        // Validate operation based on type and table
        switch (operation.getType().toUpperCase()) {
            case "INSERT":
                return validateInsert(operation);
            case "UPDATE":
                return validateUpdate(operation);
            case "DELETE":
                return validateDelete(operation);
            default:
                logger.warn("Unknown operation type: {}", operation.getType());
                return false;
        }
    }
    
    private boolean validateInsert(CoordinatorService.Operation operation) {
        // Validate insert operation
        return operation.getData() != null && !operation.getData().isEmpty();
    }
    
    private boolean validateUpdate(CoordinatorService.Operation operation) {
        // Validate update operation
        return operation.getRecordId() != null && operation.getData() != null;
    }
    
    private boolean validateDelete(CoordinatorService.Operation operation) {
        // Validate delete operation
        return operation.getRecordId() != null;
    }
    
    private boolean checkResourceAvailability(List<CoordinatorService.Operation> operations) {
        // Check if required resources are available
        // This would include checking locks, constraints, etc.
        return true;
    }
    
    private void executeOperation(CoordinatorService.Operation operation) {
        // Execute the actual operation
        switch (operation.getType().toUpperCase()) {
            case "INSERT":
                auctionService.executeInsert(operation);
                break;
            case "UPDATE":
                auctionService.executeUpdate(operation);
                break;
            case "DELETE":
                auctionService.executeDelete(operation);
                break;
        }
    }
    
    private void releaseResources(List<CoordinatorService.Operation> operations) {
        // Release any resources acquired during prepare phase
        // This would include releasing locks, etc.
    }
    
    private Set<Integer> getParticipants() {
        // Get all active servers in the cluster
        Set<Integer> participants = new HashSet<>();
        participants.add(serverId);
        
        // Add peer servers
        Map<Integer, CoordinatorService> peers = coordinatorService.getPeerServices();
        participants.addAll(peers.keySet());
        
        return participants;
    }
    
    private void writeToTransactionLog(String transactionId, String status, 
                                     List<CoordinatorService.Operation> operations) {
        try {
            // Write to transaction log for recovery
            TransactionLogEntry entry = new TransactionLogEntry(
                transactionId, serverId, status, operations, System.currentTimeMillis()
            );
            
            // Store in persistent storage
            // This would typically be written to a database transaction log
            logger.debug("Transaction log entry: {} - {}", transactionId, status);
            
        } catch (Exception e) {
            logger.error("Failed to write transaction log", e);
        }
    }
    
    private void cleanupTransaction(String transactionId) {
        transactions.remove(transactionId);
        transactionParticipants.remove(transactionId);
        logger.debug("Cleaned up transaction: {}", transactionId);
    }
    
    /**
     * Recovery process for incomplete transactions
     */
    public void recover() {
        logger.info("Starting transaction recovery process");
        
        // Read transaction log and recover incomplete transactions
        List<TransactionLogEntry> incompleteTransactions = readIncompleteTransactions();
        
        for (TransactionLogEntry entry : incompleteTransactions) {
            try {
                if ("PREPARED".equals(entry.getStatus())) {
                    logger.info("Recovering prepared transaction: {}", entry.getTransactionId());
                    abort(entry.getTransactionId()); // Abort prepared transactions on recovery
                }
            } catch (Exception e) {
                logger.error("Failed to recover transaction: {}", entry.getTransactionId(), e);
            }
        }
        
        logger.info("Transaction recovery completed");
    }
    
    private List<TransactionLogEntry> readIncompleteTransactions() {
        // Read transaction log and return incomplete transactions
        // This would typically read from a database transaction log
        return new ArrayList<>();
    }
    
    // Transaction state class
    private static class TransactionState {
        private final String transactionId;
        private List<CoordinatorService.Operation> operations;
        private boolean localPrepared = false;
        private boolean localCommitted = false;
        private boolean localAborted = false;
        private final long timestamp;
        
        public TransactionState(String transactionId, List<CoordinatorService.Operation> operations) {
            this.transactionId = transactionId;
            this.operations = new ArrayList<>(operations);
            this.timestamp = System.currentTimeMillis();
        }
        
        public String getTransactionId() { return transactionId; }
        public List<CoordinatorService.Operation> getOperations() { return operations; }
        public void setOperations(List<CoordinatorService.Operation> operations) { 
            this.operations = new ArrayList<>(operations); 
        }
        public boolean isLocalPrepared() { return localPrepared; }
        public void setLocalPrepared(boolean localPrepared) { this.localPrepared = localPrepared; }
        public boolean isLocalCommitted() { return localCommitted; }
        public void setLocalCommitted(boolean localCommitted) { this.localCommitted = localCommitted; }
        public boolean isLocalAborted() { return localAborted; }
        public void setLocalAborted(boolean localAborted) { this.localAborted = localAborted; }
        public long getTimestamp() { return timestamp; }
    }
    
    // Transaction log entry class
    private static class TransactionLogEntry {
        private final String transactionId;
        private final int serverId;
        private final String status;
        private final List<CoordinatorService.Operation> operations;
        private final long timestamp;
        
        public TransactionLogEntry(String transactionId, int serverId, String status,
                                 List<CoordinatorService.Operation> operations, long timestamp) {
            this.transactionId = transactionId;
            this.serverId = serverId;
            this.status = status;
            this.operations = new ArrayList<>(operations);
            this.timestamp = timestamp;
        }
        
        public String getTransactionId() { return transactionId; }
        public int getServerId() { return serverId; }
        public String getStatus() { return status; }
        public List<CoordinatorService.Operation> getOperations() { return operations; }
        public long getTimestamp() { return timestamp; }
    }
}