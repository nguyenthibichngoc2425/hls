package com.rin.loadbalancer.model;

/**
 * Model đại diện cho 1 backend server
 */
public class BackendServer {
    public String id;
    public String host;
    public int port;
    public String url;
    public boolean alive;
    public int failureCount;
    public long lastHealthCheckTime;
    
    public BackendServer() {}
    
    public BackendServer(String id, String host, int port, String url, boolean alive, int failureCount, long lastHealthCheckTime) {
        this.id = id;
        this.host = host;
        this.port = port;
        this.url = url;
        this.alive = alive;
        this.failureCount = failureCount;
        this.lastHealthCheckTime = lastHealthCheckTime;
    }
    
    public String getUrl() {
        return url;
    }
    
    public void setUrl(String url) {
        this.url = url;
    }
    
    public void setAlive(boolean alive) {
        this.alive = alive;
    }
    
    public boolean isAlive() {
        return alive;
    }
    
    public void setFailureCount(int failureCount) {
        this.failureCount = failureCount;
    }
    
    @Override
    public String toString() {
        return String.format("%s (%s:%d) - %s", 
            id, host, port, alive ? "ALIVE" : "DOWN");
    }
}
