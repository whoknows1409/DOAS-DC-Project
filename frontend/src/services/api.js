import axios from 'axios';

// Load balancing configuration
const API_SERVERS = [
  'http://localhost:8081',
  'http://localhost:8082',
  'http://localhost:8083'
];

// Client-side load-balancing implementation
let currentServerIndex = 0;
let serverHealth = {};

// Initialize server health
API_SERVERS.forEach(server => {
  serverHealth[server] = true;
});

// Round-robin load balancing
function getNextServer() {
  const attempts = API_SERVERS.length;
  let attempt = 0;
  
  while (attempt < attempts) {
    const server = API_SERVERS[currentServerIndex];
    currentServerIndex = (currentServerIndex + 1) % API_SERVERS.length;
    
    if (serverHealth[server]) {
      return server;
    }
    attempt++;
  }
  
  // Fallback to first server if all are marked unhealthy
  return API_SERVERS[0];
}

// Least connections load balancing
function getLeastConnectionsServer() {
  let minConnections = Infinity;
  let selectedServer = API_SERVERS[0];
  
  for (const server of API_SERVERS) {
    if (serverHealth[server]) {
      const connections = getConnectionCount(server);
      if (connections < minConnections) {
        minConnections = connections;
        selectedServer = server;
      }
    }
  }
  
  return selectedServer;
}

function getConnectionCount(server) {
  // This would track active connections per server
  // For now, return a mock value
  return Math.floor(Math.random() * 10);
}

// Health check for servers
async function checkServerHealth(server) {
  try {
    const response = await axios.get(`${server}/api/auctions/status`, { timeout: 5000 });
    serverHealth[server] = response.status === 200;
    return serverHealth[server];
  } catch (error) {
    serverHealth[server] = false;
    console.warn(`Server ${server} is unhealthy:`, error.message);
    return false;
  }
}

// Periodic health checks
setInterval(async () => {
  for (const server of API_SERVERS) {
    await checkServerHealth(server);
  }
}, 30000); // Check every 30 seconds

// Create axios instance with load balancing
const api = axios.create();

api.interceptors.request.use(
  (config) => {
    // Choose load balancing strategy
    const strategy = localStorage.getItem('loadBalancingStrategy') || 'round-robin';
    const server = strategy === 'least-connections' 
      ? getLeastConnectionsServer() 
      : getNextServer();
    
    // Replace the base URL with selected server
    const originalUrl = config.url;
    if (originalUrl.startsWith('/api/')) {
      config.url = `${server}${originalUrl}`;
    }
    
    console.log(`Request to ${config.url} via ${server}`);
    return config;
  },
  (error) => {
    return Promise.reject(error);
  }
);

api.interceptors.response.use(
  (response) => response,
  async (error) => {
    const originalRequest = error.config;
    
    // If request fails, try next server
    if (error.code === 'ECONNREFUSED' || error.code === 'NETWORK_ERROR') {
      const server = new URL(originalRequest.url).origin;
      serverHealth[server] = false;
      
      // Retry with next server
      const strategy = localStorage.getItem('loadBalancingStrategy') || 'round-robin';
      const nextServer = strategy === 'least-connections' 
        ? getLeastConnectionsServer() 
        : getNextServer();
      
      originalRequest.url = originalRequest.url.replace(server, nextServer);
      
      console.log(`Retrying request to ${originalRequest.url}`);
      return api(originalRequest);
    }
    
    return Promise.reject(error);
  }
);

// Load balancing strategy management
export const loadBalancing = {
  setStrategy: (strategy) => {
    localStorage.setItem('loadBalancingStrategy', strategy);
  },
  
  getStrategy: () => {
    return localStorage.getItem('loadBalancingStrategy') || 'round-robin';
  },
  
  getServerHealth: () => {
    return { ...serverHealth };
  },
  
  getAvailableServers: () => {
    return API_SERVERS.filter(server => serverHealth[server]);
  }
};

// API functions
export const auctionAPI = {
  // Get all active auctions
  getActiveAuctions: async () => {
    const response = await api.get('/api/auctions/active');
    return response.data;
  },

  // Get auction by ID
  getAuction: async (id) => {
    const response = await api.get(`/api/auctions/${id}`);
    return response.data;
  },

  // Create new auction
  createAuction: async (auctionData) => {
    const response = await api.post('/api/auctions', auctionData);
    return response.data;
  },

  // Place a bid
  placeBid: async (auctionId, bidData) => {
    const response = await api.post(`/api/auctions/${auctionId}/bids`, bidData);
    return response.data;
  },

  // Get bids for auction
  getBids: async (auctionId) => {
    const response = await api.get(`/api/auctions/${auctionId}/bids`);
    return response.data;
  },

  // End auction
  endAuction: async (auctionId) => {
    const response = await api.post(`/api/auctions/${auctionId}/end`);
    return response.data;
  },

  // Get server status
  getServerStatus: async () => {
    const response = await api.get('/api/auctions/status');
    return response.data;
  },

  // Upload file
  uploadFile: async (file) => {
    const formData = new FormData();
    formData.append('file', file);
    // Use axios directly without load balancer to go through nginx proxy
    const response = await axios.post('/api/files/upload', formData, {
      headers: {
        'Content-Type': 'multipart/form-data'
      }
    });
    return response.data;
  },

  // Update user profile image
  updateProfileImage: async (userId, imageUrl) => {
    // Use axios directly without load balancer to go through nginx proxy
    const response = await axios.put(`/api/users/${userId}/profile-image`, {
      imageUrl
    });
    return response.data;
  }
};

export const adminAPI = {
  // Get system status
  getSystemStatus: async () => {
    const response = await api.get('/api/admin/status');
    return response.data;
  },

  // Get clock sync status
  getClockSyncStatus: async () => {
    const response = await api.get('/api/admin/clock-sync');
    return response.data;
  },

  // Trigger clock synchronization
  triggerClockSync: async () => {
    const response = await api.post('/api/admin/clock-sync/synchronize');
    return response.data;
  },

  // Get replication lag
  getReplicationLag: async () => {
    const response = await api.get('/api/admin/replication-lag');
    return response.data;
  },

  // Get load distribution
  getLoadDistribution: async () => {
    const response = await api.get('/api/admin/load-distribution');
    return response.data;
  },

  // Trigger election
  triggerElection: async () => {
    const response = await api.post('/api/admin/election/trigger');
    return response.data;
  },

  // Get election status
  getElectionStatus: async () => {
    const response = await api.get('/api/admin/election/status');
    return response.data;
  }
};

export default api;