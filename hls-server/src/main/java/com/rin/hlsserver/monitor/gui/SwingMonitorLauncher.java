package com.rin.hlsserver.monitor.gui;

import com.rin.hlsserver.monitor.store.LogStore;
import com.rin.hlsserver.monitor.store.OnlineWatchingStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import javax.swing.*;
import java.awt.*;

/**
 * Launcher for Swing Monitor GUI
 * Runs when Spring Boot application starts (only if display is available)
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SwingMonitorLauncher implements ApplicationRunner {
    
    private final LogStore logStore;
    private final OnlineWatchingStore onlineStore;
    
    @Override
    public void run(ApplicationArguments args) {
        log.info("Attempting to launch HLS Monitor GUI...");
        
        // Force non-headless mode
        System.setProperty("java.awt.headless", "false");
        
        // Launch Swing GUI on EDT thread
        SwingUtilities.invokeLater(() -> {
            try {
                // Check if display is available
                if (GraphicsEnvironment.isHeadless()) {
                    log.warn("Running in headless mode - HLS Monitor GUI will not be displayed");
                    log.info("Monitoring is still active and collecting data in the background");
                    return;
                }
                
                // Set look and feel
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                
                // Create and show frame
                SwingMonitorFrame frame = new SwingMonitorFrame(logStore, onlineStore);
                frame.setVisible(true);
                log.info("HLS Monitor GUI launched successfully");
                
            } catch (HeadlessException e) {
                log.warn("Cannot launch GUI - no display available: {}", e.getMessage());
                log.info("Monitoring is still active and collecting data in the background");
            } catch (Exception e) {
                log.error("Error launching HLS Monitor GUI", e);
                log.info("Monitoring is still active and collecting data in the background");
            }
        });
    }
}
