package raven.modal.demo.forms.admin;

import com.formdev.flatlaf.FlatClientProperties;
import com.formdev.flatlaf.extras.FlatSVGIcon;
import net.miginfocom.swing.MigLayout;
import raven.modal.ModalDialog;
import raven.modal.Toast;
import raven.modal.component.SimpleModalBorder;
import raven.modal.demo.api.UserApi;
import raven.modal.demo.dto.response.ApiResponse;
import raven.modal.demo.dto.response.UserResponse;
import raven.modal.demo.system.Form;
import raven.modal.demo.utils.SystemForm;
import raven.modal.option.Location;
import raven.modal.option.Option;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@SystemForm(name = "User Management", description = "Quản lý người dùng hệ thống")
public class FormUserManagement extends Form {

    private JTable table;
    private DefaultTableModel tableModel;
    private List<UserResponse> users = new ArrayList<>();
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    public FormUserManagement() {
        init();
    }

    private void init() {
        setLayout(new MigLayout("fillx,wrap,insets 15", "[fill]", "[][fill,grow]"));

        // Header
        JPanel headerPanel = new JPanel(new MigLayout("fillx,insets 0", "[]push[]"));

        JLabel title = new JLabel("Quản Lý Người Dùng");
        title.putClientProperty(FlatClientProperties.STYLE, "font:bold +4");

        JButton btnRefresh = new JButton("Làm Mới");
        btnRefresh.putClientProperty(FlatClientProperties.STYLE, "arc:8");
        btnRefresh.addActionListener(e -> loadUsers());

        headerPanel.add(title);
        headerPanel.add(btnRefresh);

        // Table
        String[] columns = {"ID", "Email", "Họ Tên", "Vai Trò", "Trạng Thái", "Ngày Tạo", "Hành Động"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return column == 6; // chỉ cột action
            }
        };

        table = new JTable(tableModel);
        table.setRowHeight(40);
        table.getTableHeader().putClientProperty(FlatClientProperties.STYLE, "height:40;font:bold;");
        table.putClientProperty(FlatClientProperties.STYLE,
                "showHorizontalLines:true;intercellSpacing:0,1;showVerticalLines:false;");

        // Center renderer
        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(SwingConstants.CENTER);
        table.getColumnModel().getColumn(0).setCellRenderer(centerRenderer);
        table.getColumnModel().getColumn(3).setCellRenderer(centerRenderer);
        table.getColumnModel().getColumn(4).setCellRenderer(centerRenderer);

        // Status renderer với màu sắc
        table.getColumnModel().getColumn(4).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                setHorizontalAlignment(SwingConstants.CENTER);

                if (value != null) {
                    String status = value.toString();
                    if ("ACTIVE".equals(status)) {
                        setForeground(new Color(34, 197, 94)); // green
                        setText("✓ Hoạt động");
                    } else if ("BANNED".equals(status)) {
                        setForeground(new Color(239, 68, 68)); // red
                        setText("✗ Bị chặn");
                    }
                }
                return c;
            }
        });

        // Action column
        table.getColumnModel().getColumn(6).setCellRenderer(new ActionButtonRenderer());
        table.getColumnModel().getColumn(6).setCellEditor(new ActionButtonEditor());

        // Column widths
        table.getColumnModel().getColumn(0).setPreferredWidth(50);
        table.getColumnModel().getColumn(1).setPreferredWidth(200);
        table.getColumnModel().getColumn(2).setPreferredWidth(180);
        table.getColumnModel().getColumn(3).setPreferredWidth(100);
        table.getColumnModel().getColumn(4).setPreferredWidth(120);
        table.getColumnModel().getColumn(5).setPreferredWidth(150);
        table.getColumnModel().getColumn(6).setPreferredWidth(120);

        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UIManager.getColor("Component.borderColor"), 1, true),
                BorderFactory.createEmptyBorder()
        ));

        add(headerPanel);
        add(scrollPane, "grow");
    }

    @Override
    public void formInit() {
        loadUsers();
    }

    private void loadUsers() {
        new SwingWorker<ApiResponse<List<UserResponse>>, Void>() {
            @Override
            protected ApiResponse<List<UserResponse>> doInBackground() {
                return UserApi.getAllUsers();
            }

            @Override
            protected void done() {
                try {
                    ApiResponse<List<UserResponse>> response = get();
                    if (response.isSuccess() && response.getResult() != null) {
                        users = response.getResult();
                        refreshTable();
                    } else {
                        Toast.show(FormUserManagement.this, Toast.Type.ERROR,
                                "Lỗi: " + response.getMessage());
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    Toast.show(FormUserManagement.this, Toast.Type.ERROR,
                            "Lỗi khi tải danh sách người dùng: " + e.getMessage());
                }
            }
        }.execute();
    }

    private void refreshTable() {
        tableModel.setRowCount(0);
        for (UserResponse user : users) {
            String roles = String.join(", ", user.getRoles());
            String createdAt = user.getCreatedAt() != null ?
                    user.getCreatedAt().format(DATE_FORMATTER) : "";

            tableModel.addRow(new Object[]{
                    user.getId(),
                    user.getEmail(),
                    user.getFullName(),
                    roles,
                    user.getStatus(),
                    createdAt,
                    user  // object để action button xử lý
            });
        }
    }

    private void toggleBanUser(UserResponse user) {
        boolean isBanned = "BANNED".equals(user.getStatus());
        String action = isBanned ? "mở chặn" : "chặn";
        String title = isBanned ? "Mở Chặn Người Dùng" : "Chặn Người Dùng";
        String message = String.format("Bạn có chắc muốn %s người dùng '%s'?", action, user.getEmail());

        // Tạo panel với padding cho message
        JPanel panel = new JPanel(new MigLayout("fillx,wrap,insets 20", "[fill,350]"));
        JLabel lblMessage = new JLabel(message, SwingConstants.CENTER);
        lblMessage.putClientProperty(FlatClientProperties.STYLE, "font:+1");
        panel.add(lblMessage);

        Option option = ModalDialog.createOption();
        option.getLayoutOption().setSize(400, -1)
                .setLocation(Location.CENTER, Location.CENTER)
                .setAnimateDistance(0.7f, 0);

        ModalDialog.showModal(this, new SimpleModalBorder(
                panel,
                title,
                SimpleModalBorder.YES_NO_OPTION,
                (controller, actionType) -> {
                    if (actionType == SimpleModalBorder.YES_OPTION) {
                        performBanUnban(user, isBanned);
                    }
                }
        ), option);
    }

    private void performBanUnban(UserResponse user, boolean isCurrentlyBanned) {
        new SwingWorker<ApiResponse<UserResponse>, Void>() {
            @Override
            protected ApiResponse<UserResponse> doInBackground() {
                if (isCurrentlyBanned) {
                    return UserApi.unbanUser(user.getId());
                } else {
                    return UserApi.banUser(user.getId());
                }
            }

            @Override
            protected void done() {
                try {
                    ApiResponse<UserResponse> response = get();
                    if (response.isSuccess()) {
                        String msg = isCurrentlyBanned ?
                                "Đã mở chặn người dùng thành công" :
                                "Đã chặn người dùng thành công";
                        Toast.show(FormUserManagement.this, Toast.Type.SUCCESS, msg);
                        loadUsers();
                    } else {
                        Toast.show(FormUserManagement.this, Toast.Type.ERROR,
                                "Lỗi: " + response.getMessage());
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    Toast.show(FormUserManagement.this, Toast.Type.ERROR,
                            "Lỗi khi cập nhật: " + e.getMessage());
                }
            }
        }.execute();
    }

    // Action Button Renderer
    private class ActionButtonRenderer extends JPanel implements javax.swing.table.TableCellRenderer {
        private final JButton btnToggleBan;

        public ActionButtonRenderer() {
            setLayout(new FlowLayout(FlowLayout.CENTER, 5, 2));
            setOpaque(true);

            btnToggleBan = new JButton();
            btnToggleBan.putClientProperty(FlatClientProperties.STYLE, "arc:8");
            add(btnToggleBan);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                                                       boolean isSelected, boolean hasFocus,
                                                       int row, int column) {
            if (value instanceof UserResponse) {
                UserResponse user = (UserResponse) value;
                boolean isBanned = "BANNED".equals(user.getStatus());

                if (isBanned) {
                    btnToggleBan.setText("Mở Chặn");
                    btnToggleBan.setIcon(new FlatSVGIcon("raven/modal/demo/icons/check.svg", 0.4f));
                } else {
                    btnToggleBan.setText("Chặn");
                    btnToggleBan.setIcon(new FlatSVGIcon("raven/modal/demo/icons/ban.svg", 0.4f));
                }
            }

            if (isSelected) {
                setBackground(table.getSelectionBackground());
            } else {
                setBackground(table.getBackground());
            }
            return this;
        }
    }

    // Action Button Editor
    private class ActionButtonEditor extends AbstractCellEditor implements javax.swing.table.TableCellEditor {
        private final JPanel panel;
        private final JButton btnToggleBan;
        private UserResponse currentUser;

        public ActionButtonEditor() {
            panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 5, 2));

            btnToggleBan = new JButton();
            btnToggleBan.putClientProperty(FlatClientProperties.STYLE, "arc:8");
            btnToggleBan.addActionListener(e -> {
                if (currentUser != null) {
                    toggleBanUser(currentUser);
                    fireEditingStopped();
                }
            });

            panel.add(btnToggleBan);
        }

        @Override
        public Component getTableCellEditorComponent(JTable table, Object value,
                                                      boolean isSelected, int row, int column) {
            if (value instanceof UserResponse) {
                currentUser = (UserResponse) value;
                boolean isBanned = "BANNED".equals(currentUser.getStatus());

                if (isBanned) {
                    btnToggleBan.setText("Mở Chặn");
                    btnToggleBan.setIcon(new FlatSVGIcon("raven/modal/demo/icons/check.svg", 0.4f));
                } else {
                    btnToggleBan.setText("Chặn");
                    btnToggleBan.setIcon(new FlatSVGIcon("raven/modal/demo/icons/ban.svg", 0.4f));
                }
            }

            panel.setBackground(table.getSelectionBackground());
            return panel;
        }

        @Override
        public Object getCellEditorValue() {
            return currentUser;
        }
    }
}
