package com.rin.hlsserver.monitor.gui;

import com.rin.hlsserver.monitor.model.LogEntry;
import com.rin.hlsserver.monitor.model.WatchingSession;
import com.rin.hlsserver.monitor.store.LogStore;
import com.rin.hlsserver.monitor.store.OnlineWatchingStore;
import lombok.extern.slf4j.Slf4j;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Swing GUI for monitoring HLS streaming and user activities
 */
@Slf4j
public class SwingMonitorFrame extends JFrame {
    
    private static final DateTimeFormatter TIME_FORMATTER = 
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault());
    
    private final LogStore logStore;
    private final OnlineWatchingStore onlineStore;
    
    // Logs tab components
    private JTable logsTable;
    private LogsTableModel logsTableModel;
    private JTextField logsFilterField;
    private JCheckBox logsAutoRefreshCheckbox;
    private Timer logsRefreshTimer;
    
    // Online tab components
    private JTable onlineTable;
    private OnlineTableModel onlineTableModel;
    private Timer onlineRefreshTimer;
    private JLabel onlineCountLabel;
    
    public SwingMonitorFrame(LogStore logStore, OnlineWatchingStore onlineStore) {
        this.logStore = logStore;
        this.onlineStore = onlineStore;
        
        initializeUI();
        startTimers();
    }
    
    private void initializeUI() {
        setTitle("HLS Monitor - Giám Sát Hệ Thống");
        setSize(1400, 750);
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        setLocationRelativeTo(null);
        
        // Create tabbed pane
        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.addTab("Nhật Ký Hoạt Động", createLogsPanel());
        tabbedPane.addTab("Người Dùng Đang Xem", createOnlinePanel());
        
        add(tabbedPane);
    }
    
    /**
     * Create Logs tab panel
     */
    private JPanel createLogsPanel() {
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        // Top control panel
        JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        
        controlPanel.add(new JLabel("Tìm kiếm:"));
        logsFilterField = new JTextField(30);
        logsFilterField.setToolTipText("Lọc theo tài khoản, IP, videoId, chất lượng hoặc hành động");
        controlPanel.add(logsFilterField);
        
        JButton applyFilterButton = new JButton("Áp Dụng");
        applyFilterButton.addActionListener(e -> refreshLogsTable());
        controlPanel.add(applyFilterButton);
        
        JButton clearFilterButton = new JButton("Xóa Bộ Lọc");
        clearFilterButton.addActionListener(e -> {
            logsFilterField.setText("");
            refreshLogsTable();
        });
        controlPanel.add(clearFilterButton);
        
        JButton clearLogsButton = new JButton("Xóa Tất Cả Nhật Ký");
        clearLogsButton.addActionListener(e -> {
            int confirm = JOptionPane.showConfirmDialog(this,
                    "Bạn có chắc muốn xóa tất cả nhật ký?",
                    "Xác Nhận Xóa", JOptionPane.YES_NO_OPTION);
            if (confirm == JOptionPane.YES_OPTION) {
                logStore.clear();
                refreshLogsTable();
            }
        });
        controlPanel.add(clearLogsButton);
        
        logsAutoRefreshCheckbox = new JCheckBox("Tự động làm mới", true);
        controlPanel.add(logsAutoRefreshCheckbox);
        
        panel.add(controlPanel, BorderLayout.NORTH);
        
        // Table
        logsTableModel = new LogsTableModel();
        logsTable = new JTable(logsTableModel);
        logsTable.setAutoCreateRowSorter(true);
        logsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        
        // Column widths
        logsTable.getColumnModel().getColumn(0).setPreferredWidth(150); // Time
        logsTable.getColumnModel().getColumn(1).setPreferredWidth(120); // Action
        logsTable.getColumnModel().getColumn(2).setPreferredWidth(150); // Account
        logsTable.getColumnModel().getColumn(3).setPreferredWidth(120); // IP
        logsTable.getColumnModel().getColumn(4).setPreferredWidth(80);  // VideoId
        logsTable.getColumnModel().getColumn(5).setPreferredWidth(80);  // Quality
        logsTable.getColumnModel().getColumn(6).setPreferredWidth(200); // Path
        logsTable.getColumnModel().getColumn(7).setPreferredWidth(200); // Message
        
        // Center align some columns
        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(SwingConstants.CENTER);
        logsTable.getColumnModel().getColumn(1).setCellRenderer(centerRenderer);
        logsTable.getColumnModel().getColumn(4).setCellRenderer(centerRenderer);
        logsTable.getColumnModel().getColumn(5).setCellRenderer(centerRenderer);
        
        JScrollPane scrollPane = new JScrollPane(logsTable);
        panel.add(scrollPane, BorderLayout.CENTER);
        
        // Bottom status panel
        JPanel statusPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JLabel logsCountLabel = new JLabel();
        statusPanel.add(logsCountLabel);
        panel.add(statusPanel, BorderLayout.SOUTH);
        
        // Update count label on refresh
        logsTableModel.setCountLabel(logsCountLabel);
        
        return panel;
    }
    
    /**
     * Create Online tab panel
     */
    private JPanel createOnlinePanel() {
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        // Top control panel
        JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        
        JButton refreshButton = new JButton("Làm Mới Ngay");
        refreshButton.addActionListener(e -> refreshOnlineTable());
        controlPanel.add(refreshButton);
        
        onlineCountLabel = new JLabel("Đang xem: 0 người");
        onlineCountLabel.setFont(onlineCountLabel.getFont().deriveFont(Font.BOLD, 14f));
        controlPanel.add(Box.createHorizontalStrut(20));
        controlPanel.add(onlineCountLabel);
        
        panel.add(controlPanel, BorderLayout.NORTH);
        
        // Table
        onlineTableModel = new OnlineTableModel();
        onlineTable = new JTable(onlineTableModel);
        onlineTable.setAutoCreateRowSorter(true);
        onlineTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        
        // Column widths
        onlineTable.getColumnModel().getColumn(0).setPreferredWidth(120); // Account
        onlineTable.getColumnModel().getColumn(1).setPreferredWidth(120); // IP
        onlineTable.getColumnModel().getColumn(2).setPreferredWidth(70);  // VideoId
        onlineTable.getColumnModel().getColumn(3).setPreferredWidth(80);  // Quality
        onlineTable.getColumnModel().getColumn(4).setPreferredWidth(140); // Started At
        onlineTable.getColumnModel().getColumn(5).setPreferredWidth(140); // Last Seen
        onlineTable.getColumnModel().getColumn(6).setPreferredWidth(80);  // Duration
        onlineTable.getColumnModel().getColumn(7).setPreferredWidth(250); // User Agent
        
        // Center align some columns
        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(SwingConstants.CENTER);
        onlineTable.getColumnModel().getColumn(2).setCellRenderer(centerRenderer);
        onlineTable.getColumnModel().getColumn(3).setCellRenderer(centerRenderer);
        onlineTable.getColumnModel().getColumn(6).setCellRenderer(centerRenderer);
        
        JScrollPane scrollPane = new JScrollPane(onlineTable);
        panel.add(scrollPane, BorderLayout.CENTER);
        
        // Bottom info panel
        JPanel infoPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        infoPanel.add(new JLabel("Tự động làm mới mỗi 2 giây • Người dùng offline sau 15 giây không hoạt động"));
        panel.add(infoPanel, BorderLayout.SOUTH);
        
        return panel;
    }
    
    /**
     * Start auto-refresh timers
     */
    private void startTimers() {
        // Logs refresh timer (2 seconds)
        logsRefreshTimer = new Timer(2000, e -> {
            if (logsAutoRefreshCheckbox.isSelected()) {
                refreshLogsTable();
            }
        });
        logsRefreshTimer.start();
        
        // Online refresh timer (2 seconds)
        onlineRefreshTimer = new Timer(2000, e -> refreshOnlineTable());
        onlineRefreshTimer.start();
        
        // Initial refresh
        SwingUtilities.invokeLater(() -> {
            refreshLogsTable();
            refreshOnlineTable();
        });
    }
    
    /**
     * Refresh logs table
     */
    private void refreshLogsTable() {
        SwingUtilities.invokeLater(() -> {
            String filterText = logsFilterField.getText().trim().toLowerCase();
            List<LogEntry> allLogs = logStore.snapshot();
            
            List<LogEntry> filteredLogs;
            if (filterText.isEmpty()) {
                filteredLogs = allLogs;
            } else {
                filteredLogs = allLogs.stream()
                        .filter(log -> matchesFilter(log, filterText))
                        .collect(Collectors.toList());
            }
            
            logsTableModel.setData(filteredLogs);
        });
    }
    
    /**
     * Check if log entry matches filter
     */
    private boolean matchesFilter(LogEntry log, String filterText) {
        return (log.getAccount() != null && log.getAccount().toLowerCase().contains(filterText)) ||
               (log.getIp() != null && log.getIp().toLowerCase().contains(filterText)) ||
               (log.getVideoId() != null && log.getVideoId().toLowerCase().contains(filterText)) ||
               (log.getQuality() != null && log.getQuality().toLowerCase().contains(filterText)) ||
               (log.getAction() != null && log.getAction().name().toLowerCase().contains(filterText)) ||
               (log.getMessage() != null && log.getMessage().toLowerCase().contains(filterText));
    }
    
    /**
     * Refresh online table
     */
    private void refreshOnlineTable() {
        SwingUtilities.invokeLater(() -> {
            List<WatchingSession> sessions = onlineStore.getAllSessions();
            
            // Sort by lastSeen descending
            sessions.sort(Comparator.comparing(WatchingSession::getLastSeen).reversed());
            
            onlineTableModel.setData(sessions);
            onlineCountLabel.setText("Đang xem: " + sessions.size() + " người");
        });
    }
    
    /**
     * Stop timers when closing
     */
    public void stopTimers() {
        if (logsRefreshTimer != null) {
            logsRefreshTimer.stop();
        }
        if (onlineRefreshTimer != null) {
            onlineRefreshTimer.stop();
        }
    }
    
    /**
     * Table model for logs
     */
    private static class LogsTableModel extends AbstractTableModel {
        private final String[] columnNames = {"Thời Gian", "Hành Động", "Tài Khoản", "Địa Chỉ IP", "Video ID", "Chất Lượng", "Đường Dẫn", "Thông Báo"};
        private List<LogEntry> data = new ArrayList<>();
        private JLabel countLabel;
        
        public void setCountLabel(JLabel label) {
            this.countLabel = label;
        }
        
        public void setData(List<LogEntry> data) {
            this.data = data;
            fireTableDataChanged();
            if (countLabel != null) {
                countLabel.setText("Tổng số nhật ký: " + data.size());
            }
        }
        
        @Override
        public int getRowCount() {
            return data.size();
        }
        
        @Override
        public int getColumnCount() {
            return columnNames.length;
        }
        
        @Override
        public String getColumnName(int column) {
            return columnNames[column];
        }
        
        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            LogEntry log = data.get(rowIndex);
            switch (columnIndex) {
                case 0: return TIME_FORMATTER.format(log.getTime());
                case 1: return log.getAction() != null ? log.getAction().name() : "";
                case 2: return log.getAccount() != null ? log.getAccount() : "";
                case 3: return log.getIp() != null ? log.getIp() : "";
                case 4: return log.getVideoId() != null ? log.getVideoId() : "";
                case 5: return log.getQuality() != null ? log.getQuality() : "";
                case 6: return log.getPath() != null ? log.getPath() : "";
                case 7: return log.getMessage() != null ? log.getMessage() : "";
                default: return "";
            }
        }
    }
    
    /**
     * Table model for online sessions
     */
    private static class OnlineTableModel extends AbstractTableModel {
        private final String[] columnNames = {"Tài Khoản", "Địa Chỉ IP", "Video ID", "Chất Lượng", "Bắt Đầu Lúc", "Hoạt Động Cuối", "Thời Gian Xem", "Trình Duyệt"};
        private List<WatchingSession> data = new ArrayList<>();
        
        public void setData(List<WatchingSession> data) {
            this.data = data;
            fireTableDataChanged();
        }
        
        @Override
        public int getRowCount() {
            return data.size();
        }
        
        @Override
        public int getColumnCount() {
            return columnNames.length;
        }
        
        @Override
        public String getColumnName(int column) {
            return columnNames[column];
        }
        
        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            WatchingSession session = data.get(rowIndex);
            switch (columnIndex) {
                case 0: return session.getAccount() != null && !session.getAccount().isEmpty() ? session.getAccount() : "Khách";
                case 1: return session.getIp() != null ? session.getIp() : "";
                case 2: return session.getVideoId() != null ? session.getVideoId() : "";
                case 3: return session.getQuality() != null ? session.getQuality() : "";
                case 4: return TIME_FORMATTER.format(session.getStartedAt());
                case 5: return TIME_FORMATTER.format(session.getLastSeen());
                case 6: {
                    // Calculate duration
                    long seconds = java.time.Duration.between(session.getStartedAt(), session.getLastSeen()).getSeconds();
                    long minutes = seconds / 60;
                    long secs = seconds % 60;
                    return String.format("%d:%02d", minutes, secs);
                }
                case 7: {
                    String ua = session.getUserAgent();
                    if (ua == null || ua.isEmpty()) return "";
                    // Shorten user agent
                    if (ua.length() > 60) return ua.substring(0, 57) + "...";
                    return ua;
                }
                default: return "";
            }
        }
    }
}
