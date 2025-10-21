package com.auction.system.rmi;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;
import java.util.Map;

/**
 * RMI interface for coordinator services between auction servers
 */
public interface CoordinatorService extends Remote {
    
    /**
     * Process a bid request through the coordinator
     */
    BidResponse processBid(BidRequest request) throws RemoteException;
    
    /**
     * Prepare phase of Two-Phase Commit
     */
    boolean prepare(String transactionId, List<Operation> operations) throws RemoteException;
    
    /**
     * Commit phase of Two-Phase Commit
     */
    boolean commit(String transactionId) throws RemoteException;
    
    /**
     * Abort phase of Two-Phase Commit
     */
    boolean abort(String transactionId) throws RemoteException;
    
    /**
     * Synchronize logical clocks using Berkeley algorithm
     */
    ClockSyncResponse synchronizeClocks(int localTime, int serverId) throws RemoteException;
    
    /**
     * Heartbeat to check server health
     */
    HeartbeatResponse heartbeat(int serverId) throws RemoteException;
    
    /**
     * Start bully election
     */
    void startBullyElection(int initiatorId) throws RemoteException;
    
    /**
     * Handle bully election message
     */
    void handleElectionMessage(int candidateId, int senderId) throws RemoteException;
    
    /**
     * Handle coordinator message
     */
    void handleCoordinatorMessage(int coordinatorId) throws RemoteException;
    
    /**
     * Start ring election
     */
    void startRingElection(int initiatorId) throws RemoteException;
    
    /**
     * Handle ring election token
     */
    void handleRingToken(ElectionToken token) throws RemoteException;
    
    /**
     * Replicate data to other servers
     */
    boolean replicateData(ReplicationRequest request) throws RemoteException;
    
    /**
     * Get server status information
     */
    ServerStatus getServerStatus() throws RemoteException;
    
    // Inner classes for data transfer
    class BidRequest implements java.io.Serializable {
        private String auctionId;
        private String bidderId;
        private double amount;
        private int logicalTimestamp;
        private int serverId;
        
        // Constructors, getters, setters
        public BidRequest() {}
        
        public BidRequest(String auctionId, String bidderId, double amount, int logicalTimestamp, int serverId) {
            this.auctionId = auctionId;
            this.bidderId = bidderId;
            this.amount = amount;
            this.logicalTimestamp = logicalTimestamp;
            this.serverId = serverId;
        }
        
        public String getAuctionId() { return auctionId; }
        public void setAuctionId(String auctionId) { this.auctionId = auctionId; }
        public String getBidderId() { return bidderId; }
        public void setBidderId(String bidderId) { this.bidderId = bidderId; }
        public double getAmount() { return amount; }
        public void setAmount(double amount) { this.amount = amount; }
        public int getLogicalTimestamp() { return logicalTimestamp; }
        public void setLogicalTimestamp(int logicalTimestamp) { this.logicalTimestamp = logicalTimestamp; }
        public int getServerId() { return serverId; }
        public void setServerId(int serverId) { this.serverId = serverId; }
    }
    
    class BidResponse implements java.io.Serializable {
        private boolean success;
        private String message;
        private int logicalTimestamp;
        
        public BidResponse() {}
        
        public BidResponse(boolean success, String message, int logicalTimestamp) {
            this.success = success;
            this.message = message;
            this.logicalTimestamp = logicalTimestamp;
        }
        
        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        public int getLogicalTimestamp() { return logicalTimestamp; }
        public void setLogicalTimestamp(int logicalTimestamp) { this.logicalTimestamp = logicalTimestamp; }
    }
    
    class Operation implements java.io.Serializable {
        private String type;
        private String table;
        private String recordId;
        private Map<String, Object> data;
        
        public Operation() {}
        
        public Operation(String type, String table, String recordId, Map<String, Object> data) {
            this.type = type;
            this.table = table;
            this.recordId = recordId;
            this.data = data;
        }
        
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        public String getTable() { return table; }
        public void setTable(String table) { this.table = table; }
        public String getRecordId() { return recordId; }
        public void setRecordId(String recordId) { this.recordId = recordId; }
        public Map<String, Object> getData() { return data; }
        public void setData(Map<String, Object> data) { this.data = data; }
    }
    
    class ClockSyncResponse implements java.io.Serializable {
        private int adjustedTime;
        private boolean success;
        
        public ClockSyncResponse() {}
        
        public ClockSyncResponse(int adjustedTime, boolean success) {
            this.adjustedTime = adjustedTime;
            this.success = success;
        }
        
        public int getAdjustedTime() { return adjustedTime; }
        public void setAdjustedTime(int adjustedTime) { this.adjustedTime = adjustedTime; }
        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }
    }
    
    class HeartbeatResponse implements java.io.Serializable {
        private boolean alive;
        private int logicalClock;
        private boolean isCoordinator;
        
        public HeartbeatResponse() {}
        
        public HeartbeatResponse(boolean alive, int logicalClock, boolean isCoordinator) {
            this.alive = alive;
            this.logicalClock = logicalClock;
            this.isCoordinator = isCoordinator;
        }
        
        public boolean isAlive() { return alive; }
        public void setAlive(boolean alive) { this.alive = alive; }
        public int getLogicalClock() { return logicalClock; }
        public void setLogicalClock(int logicalClock) { this.logicalClock = logicalClock; }
        public boolean isCoordinator() { return isCoordinator; }
        public void setCoordinator(boolean coordinator) { isCoordinator = coordinator; }
    }
    
    class ElectionToken implements java.io.Serializable {
        private int candidateId;
        private List<Integer> participants;
        private boolean isActive;
        
        public ElectionToken() {}
        
        public ElectionToken(int candidateId, List<Integer> participants, boolean isActive) {
            this.candidateId = candidateId;
            this.participants = participants;
            this.isActive = isActive;
        }
        
        public int getCandidateId() { return candidateId; }
        public void setCandidateId(int candidateId) { this.candidateId = candidateId; }
        public List<Integer> getParticipants() { return participants; }
        public void setParticipants(List<Integer> participants) { this.participants = participants; }
        public boolean isActive() { return isActive; }
        public void setActive(boolean active) { isActive = active; }
    }
    
    class ReplicationRequest implements java.io.Serializable {
        private String operationId;
        private String operationType;
        private String tableName;
        private String recordId;
        private Map<String, Object> data;
        private int logicalTimestamp;
        
        public ReplicationRequest() {}
        
        public ReplicationRequest(String operationId, String operationType, String tableName, 
                                String recordId, Map<String, Object> data, int logicalTimestamp) {
            this.operationId = operationId;
            this.operationType = operationType;
            this.tableName = tableName;
            this.recordId = recordId;
            this.data = data;
            this.logicalTimestamp = logicalTimestamp;
        }
        
        public String getOperationId() { return operationId; }
        public void setOperationId(String operationId) { this.operationId = operationId; }
        public String getOperationType() { return operationType; }
        public void setOperationType(String operationType) { this.operationType = operationType; }
        public String getTableName() { return tableName; }
        public void setTableName(String tableName) { this.tableName = tableName; }
        public String getRecordId() { return recordId; }
        public void setRecordId(String recordId) { this.recordId = recordId; }
        public Map<String, Object> getData() { return data; }
        public void setData(Map<String, Object> data) { this.data = data; }
        public int getLogicalTimestamp() { return logicalTimestamp; }
        public void setLogicalTimestamp(int logicalTimestamp) { this.logicalTimestamp = logicalTimestamp; }
    }
    
    class ServerStatus implements java.io.Serializable {
        private int serverId;
        private boolean isCoordinator;
        private int logicalClock;
        private boolean isHealthy;
        private long uptime;
        private int activeConnections;
        
        public ServerStatus() {}
        
        public ServerStatus(int serverId, boolean isCoordinator, int logicalClock, 
                          boolean isHealthy, long uptime, int activeConnections) {
            this.serverId = serverId;
            this.isCoordinator = isCoordinator;
            this.logicalClock = logicalClock;
            this.isHealthy = isHealthy;
            this.uptime = uptime;
            this.activeConnections = activeConnections;
        }
        
        public int getServerId() { return serverId; }
        public void setServerId(int serverId) { this.serverId = serverId; }
        
        // Boolean getters - both 'is' and 'get' versions for Jackson compatibility
        public boolean isCoordinator() { return isCoordinator; }
        public boolean getIsCoordinator() { return isCoordinator; }
        public void setCoordinator(boolean coordinator) { isCoordinator = coordinator; }
        public void setIsCoordinator(boolean coordinator) { isCoordinator = coordinator; }
        
        public int getLogicalClock() { return logicalClock; }
        public void setLogicalClock(int logicalClock) { this.logicalClock = logicalClock; }
        
        public boolean isHealthy() { return isHealthy; }
        public boolean getIsHealthy() { return isHealthy; }
        public void setHealthy(boolean healthy) { isHealthy = healthy; }
        public void setIsHealthy(boolean healthy) { isHealthy = healthy; }
        
        public long getUptime() { return uptime; }
        public void setUptime(long uptime) { this.uptime = uptime; }
        public int getActiveConnections() { return activeConnections; }
        public void setActiveConnections(int activeConnections) { this.activeConnections = activeConnections; }
    }
}