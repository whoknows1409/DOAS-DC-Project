import SockJS from 'sockjs-client';
import { Client } from '@stomp/stompjs';

class WebSocketService {
  constructor() {
    this.client = null;
    this.connected = false;
    this.subscriptions = new Map();
    this.reconnectAttempts = 0;
    this.maxReconnectAttempts = 5;
  }

  connect() {
    if (this.connected) {
      return Promise.resolve();
    }

    return new Promise((resolve, reject) => {
      // Try different servers for WebSocket connection
      const servers = [
        'http://localhost:8081/ws',
        'http://localhost:8082/ws',
        'http://localhost:8083/ws'
      ];

      let currentServerIndex = 0;

      const tryConnect = () => {
        const serverUrl = servers[currentServerIndex];
        
        this.client = new Client({
          webSocketFactory: () => new SockJS(serverUrl),
          connectHeaders: {},
          debug: (str) => {
            console.log('WebSocket Debug:', str);
          },
          reconnectDelay: 5000,
          heartbeatIncoming: 4000,
          heartbeatOutgoing: 4000,
          onConnect: () => {
            console.log(`Connected to WebSocket server: ${serverUrl}`);
            this.connected = true;
            this.reconnectAttempts = 0;
            
            // Resubscribe to previous subscriptions
            this.subscriptions.forEach((callback, destination) => {
              this.subscribe(destination, callback);
            });
            
            resolve();
          },
          onDisconnect: () => {
            console.log('WebSocket disconnected');
            this.connected = false;
          },
          onStompError: (frame) => {
            console.error('WebSocket STOMP error:', frame);
            
            // Try next server
            currentServerIndex = (currentServerIndex + 1) % servers.length;
            if (currentServerIndex === 0) {
              this.reconnectAttempts++;
            }
            
            if (this.reconnectAttempts < this.maxReconnectAttempts) {
              console.log(`Trying next server: ${servers[currentServerIndex]}`);
              setTimeout(tryConnect, 2000);
            } else {
              reject(new Error('Failed to connect to any WebSocket server'));
            }
          }
        });

        this.client.activate();
      };

      tryConnect();
    });
  }

  disconnect() {
    if (this.client) {
      this.client.deactivate();
      this.connected = false;
    }
  }

  subscribe(destination, callback) {
    if (!this.connected) {
      console.warn('WebSocket not connected, storing subscription');
      this.subscriptions.set(destination, callback);
      return;
    }

    const subscription = this.client.subscribe(destination, (message) => {
      try {
        const data = JSON.parse(message.body);
        callback(data);
      } catch (error) {
        console.error('Error parsing WebSocket message:', error);
        callback(message.body);
      }
    });

    this.subscriptions.set(destination, callback);
    return subscription;
  }

  unsubscribe(destination) {
    if (this.subscriptions.has(destination)) {
      this.subscriptions.delete(destination);
    }
  }

  send(destination, headers = {}, body = {}) {
    if (!this.connected) {
      console.warn('WebSocket not connected, cannot send message');
      return;
    }

    this.client.publish({
      destination,
      headers,
      body: JSON.stringify(body)
    });
  }

  isConnected() {
    return this.connected;
  }

  // Auction-specific methods
  subscribeToAuction(auctionId, callback) {
    return this.subscribe(`/topic/auction/${auctionId}`, callback);
  }

  subscribeToAllAuctions(callback) {
    return this.subscribe('/topic/auctions', callback);
  }

  subscribeToServerStatus(callback) {
    return this.subscribe('/topic/server-status', callback);
  }

  placeBid(auctionId, bidderId, amount) {
    const sessionId = this.generateSessionId();
    this.send('/app/bid', {}, {
      auctionId,
      bidderId,
      amount,
      sessionId
    });
  }

  subscribeToAuctionUpdates(auctionId, userId, callback) {
    const sessionId = this.generateSessionId();
    this.send('/app/subscribe', {}, {
      auctionId,
      userId,
      sessionId
    });
    
    return this.subscribe(`/user/queue/subscription-response`, callback);
  }

  unsubscribeFromAuction(auctionId) {
    const sessionId = this.generateSessionId();
    this.send('/app/unsubscribe', {}, {
      auctionId,
      sessionId
    });
    
    this.unsubscribe(`/topic/auction/${auctionId}`);
  }

  generateSessionId() {
    return 'session_' + Math.random().toString(36).substr(2, 9);
  }
}

export default new WebSocketService();