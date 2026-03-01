package raven.modal.demo.forms.admin;

import com.formdev.flatlaf.FlatClientProperties;
import com.formdev.flatlaf.extras.FlatSVGIcon;
import net.miginfocom.swing.MigLayout;
import raven.modal.ModalDialog;
import raven.modal.Toast;
import raven.modal.component.SimpleModalBorder;
import raven.modal.demo.api.GenreApi;
import raven.modal.demo.dto.request.GenreRequest;
import raven.modal.demo.dto.response.ApiResponse;
import raven.modal.demo.dto.response.GenreResponse;
import raven.modal.demo.system.Form;
import raven.modal.demo.system.FormManager;
import raven.modal.demo.utils.GenreManager;
import raven.modal.demo.utils.SystemForm;
import raven.modal.option.Location;
import raven.modal.option.Option;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.format.DateTimeFormatter;
import java.util.List;

@SystemForm(name = "Genre Management", description = "Quản lý thể loại phim")
public class FormGenreManagement extends Form {

    private JTable table;
    private DefaultTableModel tableModel;

    public FormGenreManagement() {
        init();
    }

    private void init() {
        setLayout(new MigLayout("fillx,wrap,insets 15", "[fill]", "[][fill,grow]"));

        // Header
        JPanel headerPanel = new JPanel(new MigLayout("fillx,insets 0", "[]push[]"));

        JLabel title = new JLabel("Quản Lý Thể Loại Phim");
        title.putClientProperty(FlatClientProperties.STYLE, "font:bold +4");

        JButton btnAdd = new JButton("Thêm Thể Loại");
        btnAdd.putClientProperty(FlatClientProperties.STYLE, "arc:8");
        btnAdd.setIcon(new FlatSVGIcon("raven/modal/demo/icons/add.svg", 0.5f));
        btnAdd.addActionListener(e -> showAddGenreDialog());

        headerPanel.add(title);
        headerPanel.add(btnAdd);

        // Table
        String[] columns = {"ID", "Genre ID", "Tên Thể Loại", "Mô Tả", "Ngày Tạo", "Hành Động"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return column == 5;
            }
        };

        table = new JTable(tableModel);
        table.setRowHeight(40);
        table.getTableHeader().putClientProperty(FlatClientProperties.STYLE, "height:40;font:bold;");
        table.putClientProperty(FlatClientProperties.STYLE,
                "showHorizontalLines:true;intercellSpacing:0,1;showVerticalLines:false;");

        // column widths
        table.getColumnModel().getColumn(0).setPreferredWidth(50);
        table.getColumnModel().getColumn(1).setPreferredWidth(100);
        table.getColumnModel().getColumn(2).setPreferredWidth(150);
        table.getColumnModel().getColumn(3).setPreferredWidth(250);
        table.getColumnModel().getColumn(4).setPreferredWidth(140);
        table.getColumnModel().getColumn(5).setPreferredWidth(150);

        // Center renderer
        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(SwingConstants.CENTER);
        table.getColumnModel().getColumn(0).setCellRenderer(centerRenderer);
        table.getColumnModel().getColumn(1).setCellRenderer(centerRenderer);
        table.getColumnModel().getColumn(4).setCellRenderer(centerRenderer);

        // action column
        table.getColumnModel().getColumn(5).setCellRenderer(new ActionButtonRenderer());
        table.getColumnModel().getColumn(5).setCellEditor(new ActionButtonEditor());

        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());

        add(headerPanel);
        add(scrollPane);

        loadGenres();
    }

    // ----------------- LOAD DATA -----------------

    private void loadGenres() {
        new SwingWorker<List<GenreResponse>, Void>() {
            @Override
            protected List<GenreResponse> doInBackground() {
                GenreManager.getInstance().loadGenres();
                return GenreManager.getInstance().getGenreResponses();
            }

            @Override
            protected void done() {
                try {
                    List<GenreResponse> genreList = get();
                    tableModel.setRowCount(0);
                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

                    if (genreList != null) {
                        for (GenreResponse genre : genreList) {
                            tableModel.addRow(new Object[]{
                                    genre.getId(),
                                    genre.getGenreId(),
                                    genre.getName(),
                                    genre.getDescription() != null ? genre.getDescription() : "",
                                    genre.getCreatedAt() != null ? genre.getCreatedAt().format(formatter) : "",
                                    genre
                            });
                        }
                    }
                } catch (Exception ex) {
                    Toast.show(FormGenreManagement.this, Toast.Type.ERROR, "Load genres lỗi: " + ex.getMessage());
                }
            }
        }.execute();
    }

    // ----------------- MODAL HELPERS -----------------

    private Option createCenterOption(int width) {
        Option option = ModalDialog.createOption();
        option.getLayoutOption().setSize(width, -1)
                .setLocation(Location.CENTER, Location.CENTER)
                .setAnimateDistance(0.7f, 0);
        return option;
    }


    // ----------------- ADD / EDIT / DELETE -----------------

    private void showAddGenreDialog() {
        JPanel panel = new JPanel(new MigLayout("fillx,wrap,insets 20", "[right]15[fill,300]"));

        JLabel lblTitle = new JLabel("Thêm Thể Loại Mới");
        lblTitle.putClientProperty(FlatClientProperties.STYLE, "font:bold +2");

        JTextField txtGenreId = new JTextField();
        JTextField txtName = new JTextField();
        JTextArea txtDescription = new JTextArea(3, 20);
        txtDescription.setLineWrap(true);
        txtDescription.setWrapStyleWord(true);
        JScrollPane scrollDesc = new JScrollPane(txtDescription);

        txtGenreId.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "VD: action, drama...");
        txtName.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Nhập tên thể loại");
        txtDescription.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Mô tả thể loại");

        panel.add(lblTitle, "span 2,center,gapbottom 15");
        panel.add(new JLabel("Genre ID:"));
        panel.add(txtGenreId);
        panel.add(new JLabel("Tên Thể Loại:"));
        panel.add(txtName);
        panel.add(new JLabel("Mô Tả:"));
        panel.add(scrollDesc);

        Option option = ModalDialog.createOption();
        option.getLayoutOption().setSize(500, -1)
                .setLocation(Location.CENTER, Location.CENTER)
                .setAnimateDistance(0.7f, 0);

        ModalDialog.showModal(this, new SimpleModalBorder(
                panel, "Thêm Thể Loại", SimpleModalBorder.YES_NO_OPTION,
                (controller, action) -> {
                    if (action == SimpleModalBorder.YES_OPTION) {
                        if (txtGenreId.getText().trim().isEmpty() || txtName.getText().trim().isEmpty()) {
                            controller.consume();
                            Toast.show(FormGenreManagement.this, Toast.Type.WARNING, "Vui lòng nhập Genre ID và Tên!");
                            return;
                        }

                        try {
                            GenreRequest request = GenreRequest.builder()
                                    .genreId(txtGenreId.getText().trim())
                                    .name(txtName.getText().trim())
                                    .description(txtDescription.getText().trim().isEmpty() ? null : txtDescription.getText().trim())
                                    .build();

                            ApiResponse<GenreResponse> response = GenreApi.createGenre(request);
                            if (response != null && response.getCode() == 200) {
                                GenreManager.getInstance().addGenre(response.getResult());
                                Toast.show(FormGenreManagement.this, Toast.Type.SUCCESS, "Thêm thể loại thành công!");
                                loadGenres();
                            } else {
                                controller.consume();
                                Toast.show(FormGenreManagement.this, Toast.Type.ERROR,
                                        "Lỗi: " + (response != null ? response.getMessage() : "Unknown"));
                            }
                        } catch (Exception ex) {
                            controller.consume();
                            Toast.show(FormGenreManagement.this, Toast.Type.ERROR, "Lỗi: " + ex.getMessage());
                        }
                    }
                }), option);
    }

    private void showEditGenreDialog(GenreResponse genre) {
        JPanel panel = new JPanel(new MigLayout("fillx,wrap,insets 20", "[right]15[fill,300]"));

        JLabel lblTitle = new JLabel("Chỉnh Sửa Thể Loại");
        lblTitle.putClientProperty(FlatClientProperties.STYLE, "font:bold +2");

        JTextField txtGenreId = new JTextField(genre.getGenreId());
        JTextField txtName = new JTextField(genre.getName());
        JTextArea txtDescription = new JTextArea(genre.getDescription() != null ? genre.getDescription() : "", 3, 20);
        txtDescription.setLineWrap(true);
        txtDescription.setWrapStyleWord(true);
        JScrollPane scrollDesc = new JScrollPane(txtDescription);

        txtGenreId.setEnabled(false);
        txtGenreId.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "VD: action, drama...");
        txtName.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Nhập tên thể loại");
        txtDescription.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Mô tả thể loại");

        panel.add(lblTitle, "span 2,center,gapbottom 15");
        panel.add(new JLabel("Genre ID:"));
        panel.add(txtGenreId);
        panel.add(new JLabel("Tên Thể Loại:"));
        panel.add(txtName);
        panel.add(new JLabel("Mô Tả:"));
        panel.add(scrollDesc);

        Option option = ModalDialog.createOption();
        option.getLayoutOption().setSize(500, -1)
                .setLocation(Location.CENTER, Location.CENTER)
                .setAnimateDistance(0.7f, 0);

        ModalDialog.showModal(this, new SimpleModalBorder(
                panel, "Chỉnh Sửa Thể Loại", SimpleModalBorder.YES_NO_OPTION,
                (controller, action) -> {
                    if (action == SimpleModalBorder.YES_OPTION) {
                        if (txtName.getText().trim().isEmpty()) {
                            controller.consume();
                            Toast.show(FormGenreManagement.this, Toast.Type.WARNING, "Vui lòng nhập Tên!");
                            return;
                        }

                        try {
                            GenreRequest request = GenreRequest.builder()
                                    .genreId(txtGenreId.getText().trim())
                                    .name(txtName.getText().trim())
                                    .description(txtDescription.getText().trim().isEmpty() ? null : txtDescription.getText().trim())
                                    .build();

                            ApiResponse<GenreResponse> response = GenreApi.updateGenre(genre.getId(), request);
                            if (response != null && response.getCode() == 200) {
                                GenreManager.getInstance().updateGenre(response.getResult());
                                Toast.show(FormGenreManagement.this, Toast.Type.SUCCESS, "Cập nhật thể loại thành công!");
                                loadGenres();
                            } else {
                                controller.consume();
                                Toast.show(FormGenreManagement.this, Toast.Type.ERROR,
                                        "Lỗi: " + (response != null ? response.getMessage() : "Unknown"));
                            }
                        } catch (Exception ex) {
                            controller.consume();
                            Toast.show(FormGenreManagement.this, Toast.Type.ERROR, "Lỗi: " + ex.getMessage());
                        }
                    }
                }), option);
    }

    private void deleteGenre(GenreResponse genre) {
        JLabel message = new JLabel("<html><div style='text-align:center;padding:20px;'>" +
                "Bạn có chắc muốn xóa thể loại<br><b>" + genre.getName() + "</b>?</div></html>");
        message.setHorizontalAlignment(SwingConstants.CENTER);

        Option option = ModalDialog.createOption();
        option.getLayoutOption().setSize(400, -1)
                .setLocation(Location.CENTER, Location.CENTER)
                .setAnimateDistance(0.7f, 0);

        ModalDialog.showModal(this, new SimpleModalBorder(
                message, "Xác Nhận Xóa", SimpleModalBorder.YES_NO_OPTION,
                (controller, action) -> {
                    if (action == SimpleModalBorder.YES_OPTION) {
                        try {
                            ApiResponse<Void> response = GenreApi.deleteGenre(genre.getId());
                            if (response != null && response.getCode() == 200) {
                                GenreManager.getInstance().removeGenre(genre.getId());
                                Toast.show(FormGenreManagement.this, Toast.Type.SUCCESS, "Xóa thể loại thành công!");
                                loadGenres();
                            } else {
                                controller.consume();
                                Toast.show(FormGenreManagement.this, Toast.Type.ERROR,
                                        "Lỗi: " + (response != null ? response.getMessage() : "Unknown"));
                            }
                        } catch (Exception ex) {
                            controller.consume();
                            Toast.show(FormGenreManagement.this, Toast.Type.ERROR, "Lỗi: " + ex.getMessage());
                        }
                    }
                }), option);
    }

    private JPanel buildGenreFormPanel(GenreResponse genre) {
        JPanel panel = new JPanel(new MigLayout("fillx,wrap,insets 10", "[right]15[fill,300]"));

        JTextField txtGenreId = new JTextField(genre != null ? genre.getGenreId() : "");
        JTextField txtName = new JTextField(genre != null ? genre.getName() : "");

        JTextArea txtDescription = new JTextArea(genre != null && genre.getDescription() != null ? genre.getDescription() : "", 2, 20);
        txtDescription.setLineWrap(true);
        txtDescription.setWrapStyleWord(true);
        JScrollPane scrollDesc = new JScrollPane(txtDescription);
        scrollDesc.setPreferredSize(new Dimension(300, 60));

        txtGenreId.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "VD: action, drama...");
        txtName.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Tên hiển thị tiếng Việt");
        txtDescription.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Mô tả ngắn (tuỳ chọn)");

        panel.add(new JLabel("Genre ID:"));
        panel.add(txtGenreId);
        panel.add(new JLabel("Tên thể loại:"));
        panel.add(txtName);
        panel.add(new JLabel("Mô tả:"));
        panel.add(scrollDesc);

        panel.putClientProperty("txtGenreId", txtGenreId);
        panel.putClientProperty("txtName", txtName);
        panel.putClientProperty("txtDescription", txtDescription);

        return panel;
    }

    // ----------------- TABLE ACTIONS -----------------

    private class ActionButtonRenderer extends JPanel implements javax.swing.table.TableCellRenderer {
        private JButton btnEdit;
        private JButton btnDelete;

        public ActionButtonRenderer() {
            setLayout(new FlowLayout(FlowLayout.CENTER, 5, 0));
            setOpaque(true);

            btnEdit = new JButton();
            btnEdit.setIcon(new FlatSVGIcon("raven/modal/demo/icons/edit.svg", 0.35f));
            btnEdit.putClientProperty(FlatClientProperties.STYLE, "arc:8");

            btnDelete = new JButton();
            btnDelete.setIcon(new FlatSVGIcon("raven/modal/demo/icons/delete.svg", 0.35f));
            btnDelete.putClientProperty(FlatClientProperties.STYLE, "arc:8");

            add(btnEdit);
            add(btnDelete);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                                                       boolean isSelected, boolean hasFocus, int row, int column) {
            setBackground(isSelected ? table.getSelectionBackground() : table.getBackground());
            return this;
        }
    }

    private class ActionButtonEditor extends AbstractCellEditor implements javax.swing.table.TableCellEditor {
        private JPanel panel;
        private JButton btnEdit;
        private JButton btnDelete;
        private GenreResponse currentGenre;

        public ActionButtonEditor() {
            panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 5, 0));

            btnEdit = new JButton();
            btnEdit.setIcon(new FlatSVGIcon("raven/modal/demo/icons/edit.svg", 0.35f));
            btnEdit.putClientProperty(FlatClientProperties.STYLE, "arc:8");
            btnEdit.addActionListener(e -> {
                fireEditingStopped();
                if (currentGenre != null) {
                    showEditGenreDialog(currentGenre);
                }
            });

            btnDelete = new JButton();
            btnDelete.setIcon(new FlatSVGIcon("raven/modal/demo/icons/delete.svg", 0.35f));
            btnDelete.putClientProperty(FlatClientProperties.STYLE, "arc:8");
            btnDelete.addActionListener(e -> {
                fireEditingStopped();
                if (currentGenre != null) {
                    deleteGenre(currentGenre);
                }
            });

            panel.add(btnEdit);
            panel.add(btnDelete);
        }

        @Override
        public Component getTableCellEditorComponent(JTable table, Object value,
                                                     boolean isSelected, int row, int column) {
            currentGenre = (GenreResponse) value;
            return panel;
        }

        @Override
        public Object getCellEditorValue() {
            return currentGenre;
        }
    }
}
