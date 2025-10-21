package com.auction.system.dto;

import com.auction.system.model.User;

public class UserResponse {
    
    private String id;
    private String username;
    private String email;
    // private String role;
    private String profileImageUrl;
    private String createdAt;
    private String updatedAt;
    
    public UserResponse() {}
    
    public UserResponse(User user) {
        this.id = user.getId().toString();
        this.username = user.getUsername();
        this.email = user.getEmail();
        // this.role = user.getRole().toString();
        this.profileImageUrl = user.getProfileImageUrl();
        this.createdAt = user.getCreatedAt().toString();
        this.updatedAt = user.getUpdatedAt().toString();
    }
    
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    public String getUsername() {
        return username;
    }
    
    public void setUsername(String username) {
        this.username = username;
    }
    
    public String getEmail() {
        return email;
    }
    
    public void setEmail(String email) {
        this.email = email;
    }
    
    // public String getRole() {
    //     return role;
    // }
    
    // public void setRole(String role) {
    //     this.role = role;
    // }
    
    public String getProfileImageUrl() {
        return profileImageUrl;
    }
    
    public void setProfileImageUrl(String profileImageUrl) {
        this.profileImageUrl = profileImageUrl;
    }
    
    public String getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }
    
    public String getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(String updatedAt) {
        this.updatedAt = updatedAt;
    }
}
