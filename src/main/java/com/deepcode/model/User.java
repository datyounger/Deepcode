package com.deepcode.model;

import java.time.LocalDateTime;

/**
 * Represents a user/account on Codeforces or VJudge platform.
 */
public class User {
    private int id;
    private String username;
    private String platform; // "codeforces" or "vjudge"
    private LocalDateTime addedDate;
    private LocalDateTime lastCrawled;

    public User() {}

    public User(String username, String platform) {
        this.username = username;
        this.platform = platform;
        this.addedDate = LocalDateTime.now();
    }

    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPlatform() { return platform; }
    public void setPlatform(String platform) { this.platform = platform; }

    public LocalDateTime getAddedDate() { return addedDate; }
    public void setAddedDate(LocalDateTime addedDate) { this.addedDate = addedDate; }

    public LocalDateTime getLastCrawled() { return lastCrawled; }
    public void setLastCrawled(LocalDateTime lastCrawled) { this.lastCrawled = lastCrawled; }

    public String getDisplayName() {
        return username + " (" + platform + ")";
    }

    @Override
    public String toString() {
        return getDisplayName();
    }
}
