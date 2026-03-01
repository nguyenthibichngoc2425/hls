package raven.modal.demo.forms.admin;

import com.formdev.flatlaf.FlatClientProperties;
import com.formdev.flatlaf.extras.FlatSVGIcon;
import net.miginfocom.swing.MigLayout;
import raven.modal.ModalDialog;
import raven.modal.Toast;
import raven.modal.component.SimpleModalBorder;
import raven.modal.demo.api.MovieApi;
import raven.modal.demo.dto.request.MovieRequest;
import raven.modal.demo.dto.response.ApiResponse;
import raven.modal.demo.dto.response.MovieResponse;
import raven.modal.demo.model.Genre;
import raven.modal.demo.system.Form;
import raven.modal.demo.utils.GenreManager;
import raven.modal.demo.utils.SystemForm;
import raven.modal.option.Location;
import raven.modal.option.Option;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.Year;
import java.util.ArrayList;
import java.util.List;

@SystemForm(name = "Movie Management", description = "Quản lý danh sách phim với FFmpeg HLS processing")
public class FormMovieManagement extends Form {

    private JTable table;
    private DefaultTableModel tableModel;
    private List<MovieResponse> movies = new ArrayList<>();
    private javax.swing.Timer refreshTimer;

    public FormMovieManagement() {
        init();
    }

    private void init() {
        setLayout(new MigLayout("fillx,wrap,insets 15", "[fill]", "[][fill,grow]"));

        // Header
        JPanel headerPanel = new JPanel(new MigLayout("fillx,insets 0", "[]push[]"));

        JLabel title = new JLabel("Quản Lý Phim");
        title.putClientProperty(FlatClientProperties.STYLE, "font:bold +4");

        JButton btnAdd = new JButton("Thêm Phim Mới");
        btnAdd.putClientProperty(FlatClientProperties.STYLE, "arc:8");
        btnAdd.setIcon(new FlatSVGIcon("raven/modal/demo/icons/add.svg", 0.5f));
        btnAdd.addActionListener(e -> showAddMovieDialog());

        JButton btnRefresh = new JButton("Làm Mới");
        btnRefresh.putClientProperty(FlatClientProperties.STYLE, "arc:8");
        btnRefresh.addActionListener(e -> loadMovies());

        headerPanel.add(title);
        headerPanel.add(btnRefresh);
        headerPanel.add(btnAdd);

        // Table
        String[] columns = {"ID", "Tên Phim", "Thể Loại", "Năm", "Thời Lượng", "Trạng Thái", "Progress", "Hành Động"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return column == 7; // Chỉ cột hành động
            }
        };

        table = new JTable(tableModel);
        table.setRowHeight(45);
        table.getTableHeader().putClientProperty(FlatClientProperties.STYLE, "height:40;font:bold;");
        table.putClientProperty(FlatClientProperties.STYLE,
                "showHorizontalLines:true;intercellSpacing:0,1;showVerticalLines:false;");

        // Renderers
        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(SwingConstants.CENTER);
        table.getColumnModel().getColumn(0).setCellRenderer(centerRenderer);
        table.getColumnModel().getColumn(3).setCellRenderer(centerRenderer);
        table.getColumnModel().getColumn(4).setCellRenderer(centerRenderer);

        // Status renderer với màu sắc
        table.getColumnModel().getColumn(5).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                                                           boolean isSelected, boolean hasFocus, int row, int column) {
                JLabel label = (JLabel) super.getTableCellRendererComponent(
                        table, value, isSelected, hasFocus, row, column);
                label.setHorizontalAlignment(SwingConstants.CENTER);

                String status = value.toString();
                if (status.contains("Đã xuất bản") || status.contains("PUBLISHED")) {
                    label.setForeground(new Color(34, 197, 94)); // green
                } else if (status.contains("Đang xử lý") || status.contains("PROCESSING")) {
                    label.setForeground(new Color(234, 179, 8)); // yellow
                } else if (status.contains("Thất bại") || status.contains("FAILED")) {
                    label.setForeground(new Color(239, 68, 68)); // red
                } else {
                    label.setForeground(new Color(156, 163, 175)); // gray
                }
                return label;
            }
        });

        // Progress bar renderer
        table.getColumnModel().getColumn(6).setCellRenderer(new ProgressBarRenderer());

        // Action column
        table.getColumnModel().getColumn(7).setCellRenderer(new ActionButtonRenderer());
        table.getColumnModel().getColumn(7).setCellEditor(new ActionButtonEditor());

        // Column widths
        table.getColumnModel().getColumn(0).setPreferredWidth(50);
        table.getColumnModel().getColumn(1).setPreferredWidth(250);
        table.getColumnModel().getColumn(2).setPreferredWidth(100);
        table.getColumnModel().getColumn(3).setPreferredWidth(70);
        table.getColumnModel().getColumn(4).setPreferredWidth(100);
        table.getColumnModel().getColumn(5).setPreferredWidth(120);
        table.getColumnModel().getColumn(6).setPreferredWidth(100);
        table.getColumnModel().getColumn(7).setPreferredWidth(180);

        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UIManager.getColor("Component.borderColor"), 1, true),
                BorderFactory.createEmptyBorder()
        ));

        add(headerPanel);
        add(scrollPane, "grow");

        // Auto refresh mỗi 5s để cập nhật progress
        refreshTimer = new javax.swing.Timer(5000, e -> {
            boolean hasProcessing = movies.stream()
                    .anyMatch(m -> "PROCESSING".equals(m.getStatus()));
            if (hasProcessing) {
                loadMovies();
            }
        });
        refreshTimer.start();
    }

    @Override
    public void formInit() {
        loadMovies();
    }

    private void loadMovies() {
        new SwingWorker<ApiResponse<List<MovieResponse>>, Void>() {
            @Override
            protected ApiResponse<List<MovieResponse>> doInBackground() {
                return MovieApi.getAllMovies();
            }

            @Override
            protected void done() {
                try {
                    ApiResponse<List<MovieResponse>> response = get();
                    if (response != null && response.getCode() == 200 && response.getResult() != null) {
                        movies = response.getResult();
                        refreshTable();
                    } else {
                        Toast.show(FormMovieManagement.this, Toast.Type.ERROR,
                                "Lỗi: " + (response != null ? response.getMessage() : "Unknown"));
                    }
                } catch (Exception ex) {
                    Toast.show(FormMovieManagement.this, Toast.Type.ERROR, "Lỗi load movies: " + ex.getMessage());
                }
            }
        }.execute();
    }

    private void refreshTable() {
        tableModel.setRowCount(0);
        for (MovieResponse movie : movies) {
            String genreName = movie.getGenre() != null ? movie.getGenre().getName() : "N/A";

            tableModel.addRow(new Object[]{
                    movie.getId(),
                    movie.getTitle(),
                    genreName,
                    movie.getReleaseYear(),
                    movie.getDurationDisplay(),
                    movie.getStatusDisplay(),
                    movie.getProcessingProgress() != null ? movie.getProcessingProgress() : 0,
                    movie // Pass movie object
            });
        }
    }

    private void showAddMovieDialog() {
        JPanel panel = new JPanel(new MigLayout("fillx,wrap,insets 20", "[right]15[fill,300]"));

        JLabel lblTitle = new JLabel("Thêm Phim Mới");
        lblTitle.putClientProperty(FlatClientProperties.STYLE, "font:bold +2");

        JTextField txtTitle = new JTextField();
        JTextArea txtDescription = new JTextArea(3, 20);
        txtDescription.setLineWrap(true);
        txtDescription.setWrapStyleWord(true);
        JScrollPane scrollDesc = new JScrollPane(txtDescription);

        JTextField txtImageUrl = new JTextField();
        JTextField txtSourcePath = new JTextField();
        JTextField txtDuration = new JTextField();
        JTextField txtProcessingMinutes = new JTextField("0");
        int currentYear = Year.now().getValue();
        JSpinner spinYear = new JSpinner(new SpinnerNumberModel(Integer.valueOf(currentYear), Integer.valueOf(1900), Integer.valueOf(2100), Integer.valueOf(1)));
        JComboBox<Genre> cboGenre = new JComboBox<>(GenreManager.getInstance().getGenreArray());

        // Placeholders
        txtTitle.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Nhập tên phim");
        txtDescription.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Mô tả phim");
        txtImageUrl.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "https://example.com/image.jpg");
        txtSourcePath.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "/path/to/source/video.mp4");
        txtDuration.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "142 (phút)");
        txtProcessingMinutes.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "0=skip, >0=xử lý");

        panel.add(lblTitle, "span 2,center,gapbottom 15");
        panel.add(new JLabel("Tên Phim:"));
        panel.add(txtTitle);
        panel.add(new JLabel("Mô Tả:"));
        panel.add(scrollDesc);
        panel.add(new JLabel("Image URL:"));
        panel.add(txtImageUrl);
        panel.add(new JLabel("Source Video Path:"));
        panel.add(txtSourcePath);
        panel.add(new JLabel("Thời Lượng (phút):"));
        panel.add(txtDuration);
        panel.add(new JLabel("Xử Lý Video (phút):"));
        panel.add(txtProcessingMinutes);
        panel.add(new JLabel("Năm Phát Hành:"));
        panel.add(spinYear);
        panel.add(new JLabel("Thể Loại:"));
        panel.add(cboGenre);

        Option option = ModalDialog.createOption();
        option.getLayoutOption().setSize(520, -1)
                .setLocation(Location.CENTER, Location.CENTER)
                .setAnimateDistance(0.7f, 0);

        ModalDialog.showModal(this, new SimpleModalBorder(
                panel, "Thêm Phim", SimpleModalBorder.YES_NO_OPTION,
                (controller, action) -> {
                    if (action == SimpleModalBorder.YES_OPTION) {
                        if (txtTitle.getText().trim().isEmpty()) {
                            controller.consume();
                            Toast.show(FormMovieManagement.this, Toast.Type.WARNING, "Vui lòng nhập tên phim!");
                            return;
                        }

                        try {
                            Genre selectedGenre = (Genre) cboGenre.getSelectedItem();
                            if (selectedGenre == null) {
                                controller.consume();
                                Toast.show(FormMovieManagement.this, Toast.Type.WARNING, "Vui lòng chọn thể loại!");
                                return;
                            }

                            // Validate fields
                            if (txtDuration.getText().trim().isEmpty()) {
                                controller.consume();
                                Toast.show(FormMovieManagement.this, Toast.Type.WARNING, "Vui lòng nhập thời lượng!");
                                return;
                            }

                            int duration, processingMinutes;
                            try {
                                duration = Integer.parseInt(txtDuration.getText().trim());
                            } catch (NumberFormatException e) {
                                controller.consume();
                                Toast.show(FormMovieManagement.this, Toast.Type.ERROR, "Thời lượng phải là số nguyên hợp lệ!");
                                return;
                            }

                            try {
                                processingMinutes = Integer.parseInt(txtProcessingMinutes.getText().trim());
                            } catch (NumberFormatException e) {
                                controller.consume();
                                Toast.show(FormMovieManagement.this, Toast.Type.ERROR, "Số phút xử lý phải là số nguyên hợp lệ!");
                                return;
                            }

                            MovieRequest request = MovieRequest.builder()
                                    .title(txtTitle.getText().trim())
                                    .description(txtDescription.getText().trim())
                                    .imageUrl(txtImageUrl.getText().trim())
                                    .sourceVideoPath(txtSourcePath.getText().trim())
                                    .duration(duration)
                                    .processingMinutes(processingMinutes)
                                    .releaseYear((Integer) spinYear.getValue())
                                    .genreId(selectedGenre.getId())
                                    .build();

                            // Dialog đóng ngay, chạy API trong background
                            Toast.show(FormMovieManagement.this, Toast.Type.INFO, "Đang tạo phim...");
                            
                            new SwingWorker<ApiResponse<MovieResponse>, Void>() {
                                @Override
                                protected ApiResponse<MovieResponse> doInBackground() {
                                    return MovieApi.createMovie(request);
                                }

                                @Override
                                protected void done() {
                                    try {
                                        ApiResponse<MovieResponse> response = get();
                                        if (response != null && response.getCode() == 200) {
                                            MovieResponse newMovie = response.getResult();
                                            
                                            // Thêm movie ngay, UI update tức thì
                                            SwingUtilities.invokeLater(() -> {
                                                movies.add(0, newMovie);
                                                refreshTable();
                                                table.revalidate();
                                                table.repaint();
                                            });
                                            
                                            Toast.show(FormMovieManagement.this, Toast.Type.SUCCESS, "Thêm phim thành công!");
                                            
                                            if (request.getProcessingMinutes() > 0) {
                                                Toast.show(FormMovieManagement.this, Toast.Type.INFO,
                                                        "Video đang được xử lý với FFmpeg (CUDA)...");
                                            }
                                            
                                            // Timer 2s sẽ tự động refresh progress
                                        } else {
                                            Toast.show(FormMovieManagement.this, Toast.Type.ERROR,
                                                    "Lỗi: " + (response != null ? response.getMessage() : "Unknown"));
                                        }
                                    } catch (Exception e) {
                                        Toast.show(FormMovieManagement.this, Toast.Type.ERROR, "Lỗi: " + e.getMessage());
                                    }
                                }
                            }.execute();
                        } catch (Exception e) {
                            controller.consume();
                           e.printStackTrace();
                            Toast.show(FormMovieManagement.this, Toast.Type.ERROR, "Lỗi: " + e.getMessage());
                        }
                    }
                }), option);
    }

    private void showEditMovieDialog(MovieResponse movie) {
        JPanel panel = new JPanel(new MigLayout("fillx,wrap,insets 20", "[right]15[fill,300]"));

        JLabel lblTitle = new JLabel("Chỉnh Sửa Phim");
        lblTitle.putClientProperty(FlatClientProperties.STYLE, "font:bold +2");

        JTextField txtTitle = new JTextField(movie.getTitle());
        JTextArea txtDescription = new JTextArea(movie.getDescription(), 3, 20);
        txtDescription.setLineWrap(true);
        txtDescription.setWrapStyleWord(true);
        JScrollPane scrollDesc = new JScrollPane(txtDescription);

        JTextField txtImageUrl = new JTextField(movie.getImageUrl());
        JTextField txtSourcePath = new JTextField(movie.getSourceVideoPath());
        JTextField txtDuration = new JTextField(movie.getDuration().toString());
        JTextField txtProcessingMinutes = new JTextField(movie.getProcessingMinutes().toString());
        JSpinner spinYear = new JSpinner(new SpinnerNumberModel(Integer.valueOf(movie.getReleaseYear()), Integer.valueOf(1900), Integer.valueOf(2100), Integer.valueOf(1)));

        JComboBox<Genre> cboGenre = new JComboBox<>(GenreManager.getInstance().getGenreArray());
        if (movie.getGenre() != null) {
            Genre[] genres = GenreManager.getInstance().getGenreArray();
            for (Genre g : genres) {
                if (g.getId().equals(movie.getGenre().getId())) {
                    cboGenre.setSelectedItem(g);
                    break;
                }
            }
        }

        panel.add(lblTitle, "span 2,center,gapbottom 15");
        panel.add(new JLabel("Tên Phim:"));
        panel.add(txtTitle);
        panel.add(new JLabel("Mô Tả:"));
        panel.add(scrollDesc);
        panel.add(new JLabel("Image URL:"));
        panel.add(txtImageUrl);
        panel.add(new JLabel("Source Video Path:"));
        panel.add(txtSourcePath);
        panel.add(new JLabel("Thời Lượng (phút):"));
        panel.add(txtDuration);
        panel.add(new JLabel("Xử Lý Video (phút):"));
        panel.add(txtProcessingMinutes);
        panel.add(new JLabel("Năm Phát Hành:"));
        panel.add(spinYear);
        panel.add(new JLabel("Thể Loại:"));
        panel.add(cboGenre);

        Option option = ModalDialog.createOption();
        option.getLayoutOption().setSize(520, -1)
                .setLocation(Location.CENTER, Location.CENTER)
                .setAnimateDistance(0.7f, 0);

        ModalDialog.showModal(this, new SimpleModalBorder(
                panel, "Chỉnh Sửa Phim", SimpleModalBorder.YES_NO_OPTION,
                (controller, action) -> {
                    if (action == SimpleModalBorder.YES_OPTION) {
                        if (txtTitle.getText().trim().isEmpty()) {
                            controller.consume();
                            Toast.show(FormMovieManagement.this, Toast.Type.WARNING, "Vui lòng nhập tên phim!");
                            return;
                        }

                        try {
                            Genre selectedGenre = (Genre) cboGenre.getSelectedItem();

                            // Validate fields
                            if (txtDuration.getText().trim().isEmpty()) {
                                controller.consume();
                                Toast.show(FormMovieManagement.this, Toast.Type.WARNING, "Vui lòng nhập thời lượng!");
                                return;
                            }

                            int duration, processingMinutes;
                            try {
                                duration = Integer.parseInt(txtDuration.getText().trim());
                            } catch (NumberFormatException e) {
                                controller.consume();
                                Toast.show(FormMovieManagement.this, Toast.Type.ERROR, "Thời lượng phải là số nguyên hợp lệ!");
                                return;
                            }

                            try {
                                processingMinutes = Integer.parseInt(txtProcessingMinutes.getText().trim());
                            } catch (NumberFormatException e) {
                                controller.consume();
                                Toast.show(FormMovieManagement.this, Toast.Type.ERROR, "Số phút xử lý phải là số nguyên hợp lệ!");
                                return;
                            }

                            MovieRequest request = MovieRequest.builder()
                                    .title(txtTitle.getText().trim())
                                    .description(txtDescription.getText().trim())
                                    .imageUrl(txtImageUrl.getText().trim())
                                    .sourceVideoPath(txtSourcePath.getText().trim())
                                    .duration(duration)
                                    .processingMinutes(processingMinutes)
                                    .releaseYear((Integer) spinYear.getValue())
                                    .genreId(selectedGenre.getId())
                                    .build();

                            // Dialog sẽ đóng ngay khi callback return
                            // Chạy API call SAU KHI dialog đã đóng
                            SwingUtilities.invokeLater(() -> {
                                Toast.show(FormMovieManagement.this, Toast.Type.INFO, "Đang cập nhật...");
                                
                                new SwingWorker<ApiResponse<MovieResponse>, Void>() {
                                    @Override
                                    protected ApiResponse<MovieResponse> doInBackground() {
                                        return MovieApi.updateMovie(movie.getId(), request);
                                    }

                                    @Override
                                    protected void done() {
                                        try {
                                            ApiResponse<MovieResponse> response = get();
                                            if (response != null && response.getCode() == 200) {
                                                MovieResponse updatedMovie = response.getResult();
                                                
                                                // Cập nhật movie trong list ngay lập tức
                                                SwingUtilities.invokeLater(() -> {
                                                    for (int i = 0; i < movies.size(); i++) {
                                                        if (movies.get(i).getId().equals(updatedMovie.getId())) {
                                                            movies.set(i, updatedMovie);
                                                            break;
                                                        }
                                                    }
                                                    refreshTable();
                                                    table.revalidate();
                                                    table.repaint();
                                                });
                                                
                                                Toast.show(FormMovieManagement.this, Toast.Type.SUCCESS, "Cập nhật phim thành công!");
                                                
                                                // Timer 2s sẽ tự động refresh khi có PROCESSING
                                            } else {
                                                Toast.show(FormMovieManagement.this, Toast.Type.ERROR,
                                                        "Lỗi: " + (response != null ? response.getMessage() : "Unknown"));
                                            }
                                        } catch (Exception e) {
                                            Toast.show(FormMovieManagement.this, Toast.Type.ERROR, "Lỗi: " + e.getMessage());
                                        }
                                    }
                                }.execute();
                            });
                        } catch (Exception e) {
                            controller.consume();
                            Toast.show(FormMovieManagement.this, Toast.Type.ERROR, "Lỗi: " + e.getMessage());
                        }
                    }
                }), option);
    }

    private void deleteMovie(MovieResponse movie) {
        JLabel message = new JLabel("<html><div style='text-align:center;padding:20px;'>" +
                "Bạn có chắc muốn xóa phim<br><b>" + movie.getTitle() + "</b>?<br>" +
                "<span style='color:red;'>Video files sẽ bị xóa vĩnh viễn!</span></div></html>");
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
                            ApiResponse<Void> response = MovieApi.deleteMovie(movie.getId());
                            if (response != null && response.getCode() == 200) {
                                Toast.show(FormMovieManagement.this, Toast.Type.SUCCESS, "Xóa phim thành công!");
                                loadMovies();
                            } else {
                                controller.consume();
                                Toast.show(FormMovieManagement.this, Toast.Type.ERROR,
                                        "Lỗi: " + (response != null ? response.getMessage() : "Unknown"));
                            }
                        } catch (Exception ex) {
                            controller.consume();
                            Toast.show(FormMovieManagement.this, Toast.Type.ERROR, "Lỗi: " + ex.getMessage());
                        }
                    }
                }), option);
    }

    private void reprocessMovie(MovieResponse movie) {
        if ("PROCESSING".equals(movie.getStatus())) {
            Toast.show(this, Toast.Type.WARNING, "Phim đang được xử lý!");
            return;
        }

        // Chạy API call ngay
        Toast.show(this, Toast.Type.INFO, "Đang gửi yêu cầu xử lý lại...");

        new SwingWorker<ApiResponse<MovieResponse>, Void>() {
            @Override
            protected ApiResponse<MovieResponse> doInBackground() {
                return MovieApi.reprocessMovie(movie.getId());
            }

            @Override
            protected void done() {
                try {
                    ApiResponse<MovieResponse> response = get();
                    if (response != null && response.getCode() == 200) {
                        MovieResponse updatedMovie = response.getResult();
                        
                        // Cập nhật movie trong list ngay lập tức
                        SwingUtilities.invokeLater(() -> {
                            for (int i = 0; i < movies.size(); i++) {
                                if (movies.get(i).getId().equals(updatedMovie.getId())) {
                                    movies.set(i, updatedMovie);
                                    break;
                                }
                            }
                            refreshTable();
                            table.revalidate();
                            table.repaint();
                        });
                        
                        Toast.show(FormMovieManagement.this, Toast.Type.SUCCESS, "Bắt đầu xử lý lại video!");
                        
                        // Timer 2s sẽ tự động refresh khi có PROCESSING
                    } else {
                        Toast.show(FormMovieManagement.this, Toast.Type.ERROR,
                                "Lỗi: " + (response != null ? response.getMessage() : "Unknown"));
                    }
                } catch (Exception ex) {
                    Toast.show(FormMovieManagement.this, Toast.Type.ERROR, "Lỗi: " + ex.getMessage());
                }
            }
        }.execute();
    }


    // Progress Bar Renderer
    private class ProgressBarRenderer extends JProgressBar implements javax.swing.table.TableCellRenderer {
        public ProgressBarRenderer() {
            super(0, 100);
            setStringPainted(true);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                                                       boolean isSelected, boolean hasFocus, int row, int column) {
            int progress = value instanceof Integer ? (Integer) value : 0;
            setValue(progress);
            setString(progress + "%");
            return this;
        }
    }

    // Action Button Renderer
    private class ActionButtonRenderer extends JPanel implements javax.swing.table.TableCellRenderer {
        private final JButton btnEdit;
        private final JButton btnDelete;
        private final JButton btnReprocess;

        public ActionButtonRenderer() {
            setLayout(new FlowLayout(FlowLayout.CENTER, 3, 2));

            btnEdit = new JButton("Sửa");
            btnEdit.putClientProperty(FlatClientProperties.STYLE, "arc:5;border:2,6,2,6");
            btnEdit.setIcon(new FlatSVGIcon("raven/modal/demo/icons/edit.svg", 0.35f));

            btnDelete = new JButton("Xóa");
            btnDelete.putClientProperty(FlatClientProperties.STYLE, "arc:5;border:2,6,2,6");
            btnDelete.setIcon(new FlatSVGIcon("raven/modal/demo/icons/delete.svg", 0.35f));

            btnReprocess = new JButton("↻");
            btnReprocess.setToolTipText("Xử lý lại video");
            btnReprocess.putClientProperty(FlatClientProperties.STYLE, "arc:5;border:2,6,2,6");

            add(btnEdit);
            add(btnDelete);
            add(btnReprocess);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                                                       boolean isSelected, boolean hasFocus, int row, int column) {
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
        private final JButton btnEdit;
        private final JButton btnDelete;
        private final JButton btnReprocess;
        private MovieResponse currentMovie;

        public ActionButtonEditor() {
            panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 3, 2));

            btnEdit = new JButton("Sửa");
            btnEdit.putClientProperty(FlatClientProperties.STYLE, "arc:5;border:2,6,2,6");
            btnEdit.setIcon(new FlatSVGIcon("raven/modal/demo/icons/edit.svg", 0.35f));
            btnEdit.addActionListener(e -> {
                fireEditingStopped();
                showEditMovieDialog(currentMovie);
            });

            btnDelete = new JButton("Xóa");
            btnDelete.putClientProperty(FlatClientProperties.STYLE, "arc:5;border:2,6,2,6");
            btnDelete.setIcon(new FlatSVGIcon("raven/modal/demo/icons/delete.svg", 0.35f));
            btnDelete.addActionListener(e -> {
                fireEditingStopped();
                deleteMovie(currentMovie);
            });

            btnReprocess = new JButton("↻");
            btnReprocess.setToolTipText("Xử lý lại video");
            btnReprocess.putClientProperty(FlatClientProperties.STYLE, "arc:5;border:2,6,2,6");
            btnReprocess.addActionListener(e -> {
                fireEditingStopped();
                reprocessMovie(currentMovie);
            });

            panel.add(btnEdit);
            panel.add(btnDelete);
            panel.add(btnReprocess);
        }

        @Override
        public Component getTableCellEditorComponent(JTable table, Object value,
                                                     boolean isSelected, int row, int column) {
            currentMovie = (MovieResponse) value;
            return panel;
        }

        @Override
        public Object getCellEditorValue() {
            return currentMovie;
        }
    }
}
