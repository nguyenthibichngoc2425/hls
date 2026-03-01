package com.rin.hlsserver.monitor.store;

import com.rin.hlsserver.monitor.model.LogEntry;
import org.springframework.stereotype.Component;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Thread-safe ring buffer for storing log entries
 * Keeps maximum of maxEntries, oldest entries are removed when limit is reached
 */
@Component
public class LogStore {
    
    private final int maxEntries;
    private final ArrayDeque<LogEntry> logs;
    private final ReentrantLock lock;
    
    public LogStore() {
        this.maxEntries = 2000;
        this.logs = new ArrayDeque<>(maxEntries);
        this.lock = new ReentrantLock();
    }
    
    /**
     * Add a log entry (newest first)
     * If at capacity, removes oldest entry
     */
    public void add(LogEntry entry) {
        lock.lock();
        try {
            // Add to front (newest first)
            logs.addFirst(entry);
            
            // Remove oldest if over capacity
            if (logs.size() > maxEntries) {
                logs.removeLast();
            }
        } finally {
            lock.unlock();
        }
    }
    
    /**
     * Get snapshot of all logs (newest first)
     */
    public List<LogEntry> snapshot() {
        lock.lock();
        try {
            return new ArrayList<>(logs);
        } finally {
            lock.unlock();
        }
    }
    
    /**
     * Clear all logs
     */
    public void clear() {
        lock.lock();
        try {
            logs.clear();
        } finally {
            lock.unlock();
        }
    }
    
    /**
     * Get current count of logs
     */
    public int getSize() {
        lock.lock();
        try {
            return logs.size();
        } finally {
            lock.unlock();
        }
    }
}
