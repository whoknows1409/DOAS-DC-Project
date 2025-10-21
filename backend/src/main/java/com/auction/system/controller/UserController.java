package com.auction.system.controller;

import com.auction.system.model.User;
import com.auction.system.service.UserService;
import com.auction.system.dto.UserRegistrationRequest;
import com.auction.system.dto.UserLoginRequest;
import com.auction.system.dto.UserResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * REST Controller for user operations
 */
@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = "*")
public class UserController {
    
    private static final Logger logger = LoggerFactory.getLogger(UserController.class);
    
    @Autowired
    private UserService userService;
    
    /**
     * Register a new user
     */
    @PostMapping("/register")
    public ResponseEntity<UserResponse> registerUser(@Valid @RequestBody UserRegistrationRequest request) {
        try {
            User user = userService.registerUser(
                request.getUsername(),
                request.getEmail(),
                request.getPassword()
            );
            
            UserResponse response = new UserResponse(user);
            logger.info("User registered successfully: {}", user.getUsername());
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Failed to register user: {}", request.getUsername(), e);
            return ResponseEntity.badRequest().build();
        }
    }
    
    /**
     * Login user
     */
    @PostMapping("/login")
    public ResponseEntity<UserResponse> loginUser(@Valid @RequestBody UserLoginRequest request) {
        try {
            User user = userService.loginUser(request.getUsername(), request.getPassword());
            if (user == null) {
                return ResponseEntity.status(401).build();
            }
            
            UserResponse response = new UserResponse(user);
            logger.info("User logged in successfully: {}", user.getUsername());
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Failed to login user: {}", request.getUsername(), e);
            return ResponseEntity.status(401).build();
        }
    }
    
    /**
     * Get user by ID
     */
    @GetMapping("/{userId}")
    public ResponseEntity<UserResponse> getUser(@PathVariable String userId) {
        try {
            User user = userService.getUserById(userId);
            if (user == null) {
                return ResponseEntity.notFound().build();
            }
            
            UserResponse response = new UserResponse(user);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Failed to get user: {}", userId, e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Get user by username
     */
    @GetMapping("/username/{username}")
    public ResponseEntity<UserResponse> getUserByUsername(@PathVariable String username) {
        try {
            User user = userService.getUserByUsername(username);
            if (user == null) {
                return ResponseEntity.notFound().build();
            }
            
            UserResponse response = new UserResponse(user);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Failed to get user by username: {}", username, e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Get all users
     */
    @GetMapping
    public ResponseEntity<List<UserResponse>> getAllUsers() {
        try {
            List<User> users = userService.getAllUsers();
            List<UserResponse> responses = users.stream()
                .map(UserResponse::new)
                .collect(Collectors.toList());
            
            return ResponseEntity.ok(responses);
            
        } catch (Exception e) {
            logger.error("Failed to get all users", e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Update user profile
     */
    @PutMapping("/{userId}")
    public ResponseEntity<UserResponse> updateUser(@PathVariable String userId, 
                                                  @Valid @RequestBody UserRegistrationRequest request) {
        try {
            User user = userService.updateUser(userId, request.getUsername(), request.getEmail());
            if (user == null) {
                return ResponseEntity.notFound().build();
            }
            
            UserResponse response = new UserResponse(user);
            logger.info("User updated successfully: {}", user.getUsername());
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Failed to update user: {}", userId, e);
            return ResponseEntity.badRequest().build();
        }
    }
    
    /**
     * Update user profile image
     */
    @PutMapping("/{userId}/profile-image")
    public ResponseEntity<UserResponse> updateProfileImage(@PathVariable String userId, 
                                                          @RequestBody Map<String, String> request) {
        try {
            String imageUrl = request.get("imageUrl");
            User user = userService.updateProfileImage(userId, imageUrl);
            if (user == null) {
                return ResponseEntity.notFound().build();
            }
            
            UserResponse response = new UserResponse(user);
            logger.info("Profile image updated for user: {}", user.getUsername());
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Failed to update profile image: {}", userId, e);
            return ResponseEntity.badRequest().build();
        }
    }
    
    /**
     * Delete user
     */
    @DeleteMapping("/{userId}")
    public ResponseEntity<Void> deleteUser(@PathVariable String userId) {
        try {
            boolean deleted = userService.deleteUser(userId);
            if (!deleted) {
                return ResponseEntity.notFound().build();
            }
            
            logger.info("User deleted successfully: {}", userId);
            return ResponseEntity.ok().build();
            
        } catch (Exception e) {
            logger.error("Failed to delete user: {}", userId, e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Get user's auctions
     */
    @GetMapping("/{userId}/auctions")
    public ResponseEntity<List<Object>> getUserAuctions(@PathVariable String userId) {
        try {
            List<Object> auctions = userService.getUserAuctions(userId);
            return ResponseEntity.ok(auctions);
            
        } catch (Exception e) {
            logger.error("Failed to get user auctions: {}", userId, e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Get user's bids
     */
    @GetMapping("/{userId}/bids")
    public ResponseEntity<List<Object>> getUserBids(@PathVariable String userId) {
        try {
            List<Object> bids = userService.getUserBids(userId);
            return ResponseEntity.ok(bids);
            
        } catch (Exception e) {
            logger.error("Failed to get user bids: {}", userId, e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Get auctions won by user
     */
    @GetMapping("/{userId}/won-auctions")
    public ResponseEntity<List<Object>> getWonAuctions(@PathVariable String userId) {
        try {
            List<Object> wonAuctions = userService.getWonAuctions(userId);
            return ResponseEntity.ok(wonAuctions);
            
        } catch (Exception e) {
            logger.error("Failed to get won auctions: {}", userId, e);
            return ResponseEntity.internalServerError().build();
        }
    }
}
