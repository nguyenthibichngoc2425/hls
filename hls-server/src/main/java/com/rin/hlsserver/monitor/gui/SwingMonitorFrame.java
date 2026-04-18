package com.rin.hlsserver.monitor.gui;

import com.rin.hlsserver.monitor.model.WatchingSession;
import com.rin.hlsserver.monitor.store.OnlineWatchingStore;
import com.rin.hlsserver.model.SystemLog;
import com.rin.hlsserver.service.SystemLogService;
import lombok.extern.slf4j.Slf4j;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.net.InetAddress;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Swing GUI for monitoring HLS streaming and user activities
 */
@Slf4j
public class SwingMonitorFrame extends JFrame {
    
        private static final DateTimeFormatter TIME_FORMATTER =
            DateTimeFormatter.ofPattern("HH:mm:ss").withZone(ZoneId.systemDefault());

    private final SystemLogService systemLogService;
    private final OnlineWatchingStore onlineStore;
    
    // Logs tab components
    private JTable logsTable;
    private LogsTableModel logsTableModel;
    private JTextField logsFilterField;
    private JComboBox<String> eventFilterCombo;
    private JCheckBox logsAutoRefreshCheckbox;
    private Timer logsRefreshTimer;
    
    // Online tab components
    private JTable onlineTable;
    private OnlineTableModel onlineTableModel;
    private Timer onlineRefreshTimer;
    private JLabel onlineCountLabel;

    private final String serverName;
    private final int serverPort;
    
    public SwingMonitorFrame(SystemLogService systemLogService, OnlineWatchingStore onlineStore, String serverName, int serverPort) {
        this.systemLogService = systemLogService;
        this.onlineStore = onlineStore;
        this.serverName = serverName;
        this.serverPort = serverPort;
        
        initializeUI();
        startTimers();
    }
    
    private void initializeUI() {
        setTitle("HLS Monitor - " + serverName + " - Port " + serverPort + " - SHARED DB MODE");
        setSize(1400, 750);
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        setLocationRelativeTo(null);

        JPanel rootPanel = new JPanel(new BorderLayout());
        rootPanel.add(createStatusHeader(), BorderLayout.NORTH);
        
        // Create tabbed pane
        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.addTab("Nhật Ký Hoạt Động", createLogsPanel());
        tabbedPane.addTab("Người Dùng Đang Xem", createOnlinePanel());

        rootPanel.add(tabbedPane, BorderLayout.CENTER);
        add(rootPanel);
    }

    private JPanel createStatusHeader() {
        JPanel headerPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        headerPanel.setBorder(BorderFactory.createEmptyBorder(6, 10, 2, 10));
        String ip = resolveLocalIp();
        JLabel statusLabel = new JLabel(serverName + " | " + ip + ":" + serverPort + " | SHARED DB MODE");
        statusLabel.setFont(statusLabel.getFont().deriveFont(Font.BOLD, 13f));
        JLabel sourceLabel = new JLabel("Nguồn log: DATABASE (SHARED)");
        sourceLabel.setBorder(BorderFactory.createEmptyBorder(0, 20, 0, 0));
        sourceLabel.setFont(sourceLabel.getFont().deriveFont(Font.BOLD, 13f));
        headerPanel.add(statusLabel);
        headerPanel.add(sourceLabel);
        return headerPanel;
    }

    private String resolveLocalIp() {
        try {
            return InetAddress.getLocalHost().getHostAddress();
        } catch (Exception ignored) {
            return "127.0.0.1";
        }
    }
    
    /**
     * Create Logs tab panel
     */
    private JPanel createLogsPanel() {
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        // Top control panel
        JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));

        controlPanel.add(new JLabel("Event:"));
        eventFilterCombo = new JComboBox<>(new String[]{"ALL", "LOGIN", "HLS", "ERROR"});
        eventFilterCombo.addActionListener(e -> refreshLogsTable());
        controlPanel.add(eventFilterCombo);
        
        controlPanel.add(new JLabel("Tìm kiếm:"));
        logsFilterField = new JTextField(30);
        logsFilterField.setToolTipText("Loc theo server, event, user, IP hoac endpoint");
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
                systemLogService.clearAll();
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
        logsTable.setDefaultRenderer(Object.class, new EventTypeColorRenderer());
        
        // Column widths
        logsTable.getColumnModel().getColumn(0).setPreferredWidth(90);  // Time
        logsTable.getColumnModel().getColumn(1).setPreferredWidth(100); // Server
        logsTable.getColumnModel().getColumn(2).setPreferredWidth(140); // Event
        logsTable.getColumnModel().getColumn(3).setPreferredWidth(180); // User
        logsTable.getColumnModel().getColumn(4).setPreferredWidth(120); // IP
        logsTable.getColumnModel().getColumn(5).setPreferredWidth(420); // Endpoint
        
        // Center align some columns
        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(SwingConstants.CENTER);
        logsTable.getColumnModel().getColumn(0).setCellRenderer(centerRenderer);
        logsTable.getColumnModel().getColumn(1).setCellRenderer(centerRenderer);
        logsTable.getColumnModel().getColumn(2).setCellRenderer(centerRenderer);
        
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
        
        onlineCountLabel = new JLabel("Dang xem tren " + serverName + ": 0 nguoi");
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
        infoPanel.add(new JLabel("Auto refresh 2 giay • Tab nay hien thi nguoi dang xem tren " + serverName));
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
            String category = String.valueOf(eventFilterCombo.getSelectedItem());
            List<SystemLog> allLogs = systemLogService.getLatestLogsByCategory(200, category);
            
            List<SystemLog> filteredLogs;
            if (filterText.isEmpty()) {
                filteredLogs = allLogs;
            } else {
                filteredLogs = allLogs.stream()
                        .filter(log -> matchesFilter(log, filterText))
                        .toList();
            }
            
            logsTableModel.setData(filteredLogs);
        });
    }
    
    /**
     * Check if log entry matches filter
     */
    private boolean matchesFilter(SystemLog log, String filterText) {
        return containsIgnoreCase(log.getServerName(), filterText)
                || containsIgnoreCase(log.getEventType(), filterText)
                || containsIgnoreCase(log.getUserEmail(), filterText)
                || containsIgnoreCase(log.getIpAddress(), filterText)
                || containsIgnoreCase(log.getEndpoint(), filterText)
                || containsIgnoreCase(log.getMessage(), filterText);
    }

    private boolean containsIgnoreCase(String value, String filterText) {
        return value != null && value.toLowerCase().contains(filterText);
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
            onlineCountLabel.setText("Dang xem tren " + serverName + ": " + sessions.size() + " nguoi");
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
        private final String[] columnNames = {"Time", "Server", "Event", "User", "IP", "Endpoint"};
        private List<SystemLog> data = new ArrayList<>();
        private JLabel countLabel;
        
        public void setCountLabel(JLabel label) {
            this.countLabel = label;
        }
        
        public void setData(List<SystemLog> data) {
            this.data = data;
            fireTableDataChanged();
            if (countLabel != null) {
                countLabel.setText("Tong so ban ghi: " + data.size());
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
            SystemLog log = data.get(rowIndex);
            switch (columnIndex) {
                case 0: return log.getCreatedAt() != null
                        ? TIME_FORMATTER.format(log.getCreatedAt().atZone(ZoneId.systemDefault()).toInstant()) : "";
                case 1: return safe(log.getServerName());
                case 2: return safe(log.getEventType());
                case 3: return safe(log.getUserEmail());
                case 4: return safe(log.getIpAddress());
                case 5: return safe(log.getEndpoint());
                default: return "";
            }
        }

        private String safe(String value) {
            return value == null ? "" : value;
        }

        public String getEventTypeAt(int rowIndex) {
            if (rowIndex < 0 || rowIndex >= data.size()) {
                return "";
            }
            String eventType = data.get(rowIndex).getEventType();
            return eventType == null ? "" : eventType;
        }
    }

    private class EventTypeColorRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                       boolean hasFocus, int row, int column) {
            Component component = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            if (isSelected) {
                return component;
            }

            int modelRow = table.convertRowIndexToModel(row);
            String eventType = logsTableModel.getEventTypeAt(modelRow);

            if (eventType.startsWith("LOGIN") || eventType.equals("LOGOUT")) {
                component.setForeground(new Color(20, 128, 42));
            } else if (eventType.startsWith("HLS")) {
                component.setForeground(new Color(0, 74, 173));
            } else if (eventType.equals("ERROR")) {
                component.setForeground(new Color(194, 24, 7));
            } else {
                component.setForeground(Color.DARK_GRAY);
            }
            return component;
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
