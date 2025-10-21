package com.auction.system.service;

import com.auction.system.model.User;
import com.auction.system.repository.UserRepository;
import com.auction.system.repository.AuctionRepository;
import com.auction.system.repository.BidRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Service for managing users
 */
@Service
public class UserService {
    
    private static final Logger logger = LoggerFactory.getLogger(UserService.class);
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private AuctionRepository auctionRepository;
    
    @Autowired
    private BidRepository bidRepository;
    
    /**
     * Register a new user
     */
    @Transactional
    public User registerUser(String username, String email, String password) {
        try {
            // Check if username already exists
            if (userRepository.findByUsername(username).isPresent()) {
                throw new RuntimeException("Username already exists: " + username);
            }
            
            // Check if email already exists
            if (userRepository.findByEmail(email).isPresent()) {
                throw new RuntimeException("Email already exists: " + email);
            }
            
            // Create new user
            User user = new User();
            user.setUsername(username);
            user.setEmail(email);
            user.setPasswordHash(hashPassword(password));
            user.setCreatedAt(LocalDateTime.now());
            user.setUpdatedAt(LocalDateTime.now());
            
            User savedUser = userRepository.save(user);
            logger.info("User registered successfully: {}", username);
            return savedUser;
            
        } catch (Exception e) {
            logger.error("Failed to register user: {}", username, e);
            throw new RuntimeException("Failed to register user", e);
        }
    }
    
    /**
     * Login user
     */
    public User loginUser(String username, String password) {
        try {
            User user = userRepository.findByUsername(username).orElse(null);
            if (user == null) {
                logger.warn("Login attempt with non-existent username: {}", username);
                return null;
            }
            
            if (verifyPassword(password, user.getPasswordHash())) {
                logger.info("User logged in successfully: {}", username);
                return user;
            } else {
                logger.warn("Invalid password for user: {}", username);
                return null;
            }
            
        } catch (Exception e) {
            logger.error("Failed to login user: {}", username, e);
            return null;
        }
    }
    
    /**
     * Get user by ID
     */
    public User getUserById(String userId) {
        try {
            return userRepository.findById(UUID.fromString(userId)).orElse(null);
        } catch (Exception e) {
            logger.error("Failed to get user by ID: {}", userId, e);
            return null;
        }
    }
    
    /**
     * Get user by username
     */
    public User getUserByUsername(String username) {
        try {
            return userRepository.findByUsername(username).orElse(null);
        } catch (Exception e) {
            logger.error("Failed to get user by username: {}", username, e);
            return null;
        }
    }
    
    /**
     * Get all users
     */
    public List<User> getAllUsers() {
        try {
            return userRepository.findAll();
        } catch (Exception e) {
            logger.error("Failed to get all users", e);
            return List.of();
        }
    }
    
    /**
     * Update user
     */
    @Transactional
    public User updateUser(String userId, String username, String email) {
        try {
            User user = userRepository.findById(UUID.fromString(userId)).orElse(null);
            if (user == null) {
                return null;
            }
            
            // Check if new username is available (if changed)
            if (!user.getUsername().equals(username)) {
                if (userRepository.findByUsername(username).isPresent()) {
                    throw new RuntimeException("Username already exists: " + username);
                }
                user.setUsername(username);
            }
            
            // Check if new email is available (if changed)
            if (!user.getEmail().equals(email)) {
                if (userRepository.findByEmail(email).isPresent()) {
                    throw new RuntimeException("Email already exists: " + email);
                }
                user.setEmail(email);
            }
            
            user.setUpdatedAt(LocalDateTime.now());
            User savedUser = userRepository.save(user);
            
            logger.info("User updated successfully: {}", username);
            return savedUser;
            
        } catch (Exception e) {
            logger.error("Failed to update user: {}", userId, e);
            throw new RuntimeException("Failed to update user", e);
        }
    }
    
    /**
     * Update user profile image
     */
    @Transactional
    public User updateProfileImage(String userId, String imageUrl) {
        try {
            User user = userRepository.findById(UUID.fromString(userId)).orElse(null);
            if (user == null) {
                return null;
            }
            
            user.setProfileImageUrl(imageUrl);
            user.setUpdatedAt(LocalDateTime.now());
            User savedUser = userRepository.save(user);
            
            logger.info("Profile image updated for user: {}", user.getUsername());
            return savedUser;
            
        } catch (Exception e) {
            logger.error("Failed to update profile image: {}", userId, e);
            throw new RuntimeException("Failed to update profile image", e);
        }
    }
    
    /**
     * Delete user
     */
    @Transactional
    public boolean deleteUser(String userId) {
        try {
            User user = userRepository.findById(UUID.fromString(userId)).orElse(null);
            if (user == null) {
                return false;
            }
            
            userRepository.delete(user);
            logger.info("User deleted successfully: {}", user.getUsername());
            return true;
            
        } catch (Exception e) {
            logger.error("Failed to delete user: {}", userId, e);
            return false;
        }
    }
    
    /**
     * Get user's auctions
     */
    public List<Object> getUserAuctions(String userId) {
        try {
            return auctionRepository.findBySellerId(UUID.fromString(userId))
                .stream()
                .map(auction -> Map.of(
                    "id", auction.getId(),
                    "title", auction.getTitle(),
                    "description", auction.getDescription(),
                    "startingPrice", auction.getStartingPrice(),
                    "currentPrice", auction.getCurrentPrice(),
                    "endTime", auction.getEndTime(),
                    "status", auction.getStatus(),
                    "createdAt", auction.getCreatedAt()
                ))
                .collect(java.util.stream.Collectors.toList());
        } catch (Exception e) {
            logger.error("Failed to get user auctions: {}", userId, e);
            return List.of();
        }
    }
    
    /**
     * Get user's bids
     */
    public List<Object> getUserBids(String userId) {
        try {
            return bidRepository.findByBidderId(UUID.fromString(userId))
                .stream()
                .map(bid -> Map.of(
                    "id", bid.getId(),
                    "auctionId", bid.getAuctionId(),
                    "amount", bid.getAmount(),
                    "timestamp", bid.getTimestamp(),
                    "logicalTimestamp", bid.getLogicalTimestamp(),
                    "serverId", bid.getServerId()
                ))
                .collect(java.util.stream.Collectors.toList());
        } catch (Exception e) {
            logger.error("Failed to get user bids: {}", userId, e);
            return List.of();
        }
    }
    
    /**
     * Get auctions won by user
     */
    public List<Object> getWonAuctions(String userId) {
        try {
            return auctionRepository.findByWinnerId(UUID.fromString(userId))
                .stream()
                .map(auction -> Map.of(
                    "id", auction.getId(),
                    "title", auction.getTitle(),
                    "description", auction.getDescription(),
                    "startingPrice", auction.getStartingPrice(),
                    "currentPrice", auction.getCurrentPrice(),
                    "endTime", auction.getEndTime(),
                    "status", auction.getStatus(),
                    "winnerId", auction.getWinnerId() != null ? auction.getWinnerId() : "",
                    "winnerName", auction.getWinnerName() != null ? auction.getWinnerName() : "",
                    "createdAt", auction.getCreatedAt()
                ))
                .collect(java.util.stream.Collectors.toList());
        } catch (Exception e) {
            logger.error("Failed to get won auctions: {}", userId, e);
            return List.of();
        }
    }
    
    /**
     * Hash password (simple implementation - in production use BCrypt)
     */
    private String hashPassword(String password) {
        // Simple hash for demo - in production use BCrypt
        return String.valueOf(password.hashCode());
    }
    
    /**
     * Verify password
     */
    private boolean verifyPassword(String password, String hash) {
        return hashPassword(password).equals(hash);
    }
}
