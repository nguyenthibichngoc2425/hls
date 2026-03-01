package raven.modal.demo.forms;

import com.formdev.flatlaf.FlatClientProperties;
import net.miginfocom.swing.MigLayout;
import raven.modal.Toast;
import raven.modal.demo.api.FavoriteApi;
import raven.modal.demo.dto.response.ApiResponse;
import raven.modal.demo.dto.response.FavoriteMovieResponse;
import raven.modal.demo.dto.response.MovieResponse;
import raven.modal.demo.menu.MyDrawerBuilder;
import raven.modal.demo.system.Form;
import raven.modal.demo.system.FormManager;
import raven.modal.demo.utils.SystemForm;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.net.URL;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@SystemForm(name = "Favorite Movies", description = "Your favorite movies collection")
public class FormFavoriteMovies extends Form {

    private JPanel gridPanel;
    private JScrollPane scrollPane;
    private List<FavoriteMovieResponse> favoriteList = new ArrayList<>();
    private Map<Long, ImageIcon> imageCache = new HashMap<>();
    private int currentPage = 0;
    private int pageSize = 12;
    private boolean hasMore = true;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private Long getCurrentUserId() {
        var user = MyDrawerBuilder.getInstance().getUser();
        return user != null ? user.getUserId() : 1L;
    }

    public FormFavoriteMovies() {
        init();
    }

    private void init() {
        setLayout(new MigLayout("wrap,fillx,insets 15", "[fill]", "[]10[fill][]"));

        // Header
        add(createHeader());
        
        // Grid panel for movies
        gridPanel = new JPanel(new MigLayout("fillx,wrap 4,gap 15", "[fill,sg][fill,sg][fill,sg][fill,sg]"));
        gridPanel.putClientProperty(FlatClientProperties.STYLE, "background:null;");

        scrollPane = new JScrollPane(gridPanel);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        scrollPane.getVerticalScrollBar().putClientProperty(FlatClientProperties.STYLE, "" +
                "width:5;" +
                "trackArc:$ScrollBar.thumbArc;" +
                "trackInsets:0,0,0,0;" +
                "thumbInsets:0,0,0,0;");

        add(scrollPane, "grow");
        
        // Pagination
        add(createPagination(), "growx");
    }

    @Override
    public void formInit() {
        loadFavorites();
    }

    @Override
    public void formOpen() {
        // Tự động reload khi mở lại form
        loadFavorites();
    }

    @Override
    public void formRefresh() {
        currentPage = 0;
        favoriteList.clear();
        loadFavorites();
    }

    private JPanel createHeader() {
        JPanel panel = new JPanel(new MigLayout("fillx,insets 0", "[grow][]", "[]"));
        panel.putClientProperty(FlatClientProperties.STYLE, "background:null;");

        JLabel title = new JLabel("Phim yêu thích");
        title.putClientProperty(FlatClientProperties.STYLE, "font:bold +5;");

        JButton btnRefresh = new JButton("Làm mới");
        btnRefresh.putClientProperty(FlatClientProperties.STYLE, "arc:12;");
        btnRefresh.addActionListener(e -> formRefresh());

        panel.add(title, "split 2,aligny center");
        panel.add(new JLabel("(" + favoriteList.size() + " phim)"), "aligny center");
        panel.add(btnRefresh, "w 100!");

        return panel;
    }

    private JPanel createPagination() {
        JPanel panel = new JPanel(new MigLayout("fillx,insets 0", "[]push[]push[]", "[]"));
        panel.putClientProperty(FlatClientProperties.STYLE, "background:null;");

        JButton btnPrev = new JButton("◀ Trước");
        btnPrev.putClientProperty(FlatClientProperties.STYLE, "arc:12;");
        btnPrev.setEnabled(currentPage > 0);
        btnPrev.addActionListener(e -> {
            if (currentPage > 0) {
                currentPage--;
                loadFavorites();
            }
        });

        JLabel pageInfo = new JLabel("Trang " + (currentPage + 1));
        pageInfo.putClientProperty(FlatClientProperties.STYLE, "font:bold;");

        JButton btnNext = new JButton("Sau ▶");
        btnNext.putClientProperty(FlatClientProperties.STYLE, "arc:12;");
        btnNext.setEnabled(hasMore);
        btnNext.addActionListener(e -> {
            if (hasMore) {
                currentPage++;
                loadFavorites();
            }
        });

        panel.add(btnPrev);
        panel.add(pageInfo);
        panel.add(btnNext);

        return panel;
    }

    private void loadFavorites() {
        // Clear grid
        gridPanel.removeAll();
        
        // Show loading
        JLabel loading = new JLabel("Đang tải...", SwingConstants.CENTER);
        loading.putClientProperty(FlatClientProperties.STYLE, "font:+2;");
        gridPanel.add(loading, "span,grow");
        gridPanel.revalidate();
        gridPanel.repaint();

        // Load in background
        new SwingWorker<ApiResponse<List<FavoriteMovieResponse>>, Void>() {
            @Override
            protected ApiResponse<List<FavoriteMovieResponse>> doInBackground() {
                return FavoriteApi.getUserFavorites(getCurrentUserId(), currentPage, pageSize);
            }

            @Override
            protected void done() {
                try {
                    ApiResponse<List<FavoriteMovieResponse>> response = get();
                    if (response != null && response.getCode() == 200) {
                        favoriteList = response.getResult();
                        hasMore = favoriteList.size() >= pageSize;
                        displayFavorites();
                        updateHeader();
                        updatePagination();
                    } else {
                        showError("Lỗi tải phim yêu thích: " + (response != null ? response.getMessage() : "Unknown"));
                    }
                } catch (Exception e) {
                    showError("Lỗi: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        }.execute();
    }

    private void displayFavorites() {
        gridPanel.removeAll();

        if (favoriteList.isEmpty()) {
            JPanel emptyPanel = new JPanel(new MigLayout("wrap,fillx,insets 20", "[center]"));
            emptyPanel.putClientProperty(FlatClientProperties.STYLE, "background:null;");
            
            JLabel emptyIcon = new JLabel("💔");
            emptyIcon.putClientProperty(FlatClientProperties.STYLE, "font:+30;");
            
            JLabel emptyText = new JLabel("Chưa có phim yêu thích");
            emptyText.putClientProperty(FlatClientProperties.STYLE, "font:bold +3;foreground:$Label.disabledForeground;");
            
            JLabel emptyHint = new JLabel("Bấm nút ❤ Yêu thích ở trang chi tiết phim");
            emptyHint.putClientProperty(FlatClientProperties.STYLE, "foreground:$Label.disabledForeground;");
            
            emptyPanel.add(emptyIcon);
            emptyPanel.add(emptyText);
            emptyPanel.add(emptyHint);
            
            gridPanel.add(emptyPanel, "span,grow");
        } else {
            for (FavoriteMovieResponse favorite : favoriteList) {
                gridPanel.add(createFavoriteCard(favorite));
            }
        }

        gridPanel.revalidate();
        gridPanel.repaint();
    }

    private JPanel createFavoriteCard(FavoriteMovieResponse favorite) {
        MovieResponse movie = favorite.getMovie();
        
        JPanel card = new JPanel(new MigLayout("wrap,fillx,insets 12", "[fill]"));
        card.putClientProperty(FlatClientProperties.STYLE, "" +
                "arc:16;" +
                "[light]background:tint($Panel.background,8%);" +
                "[dark]background:tint($Panel.background,4%);");

        // Placeholder image
        JLabel imgLabel = new JLabel("", SwingConstants.CENTER);
        imgLabel.setPreferredSize(new Dimension(200, 280));
        imgLabel.putClientProperty(FlatClientProperties.STYLE, "" +
                "arc:12;" +
                "[light]background:tint($Panel.background,15%);" +
                "[dark]background:tint($Panel.background,10%);");
        imgLabel.setOpaque(true);
        
        // Load image async
        loadImageAsync(movie, imgLabel);

        JLabel title = new JLabel("<html>" + movie.getTitle() + "</html>");
        title.putClientProperty(FlatClientProperties.STYLE, "font:bold;");

        JLabel addedDate = new JLabel("Thêm vào: " + favorite.getCreatedAt().format(DATE_FORMATTER));
        addedDate.putClientProperty(FlatClientProperties.STYLE, "font:-1;foreground:$Label.disabledForeground;");

        JLabel genre = new JLabel(movie.getGenre() != null ? movie.getGenre().getName() : "N/A");
        genre.putClientProperty(FlatClientProperties.STYLE, "foreground:$Label.disabledForeground;");

        JPanel buttonPanel = new JPanel(new MigLayout("fillx,insets 0", "[grow][grow]", "[]"));
        buttonPanel.putClientProperty(FlatClientProperties.STYLE, "background:null;");

        JButton btnDetail = new JButton("Chi tiết");
        btnDetail.putClientProperty(FlatClientProperties.STYLE, "arc:12;");
        btnDetail.addActionListener(e -> {
            FormMovieDetail detailForm = new FormMovieDetail(movie);
            FormManager.showForm(detailForm);
        });

        JButton btnRemove = new JButton("💔");
        btnRemove.putClientProperty(FlatClientProperties.STYLE, "arc:12;");
        btnRemove.setToolTipText("Xóa khỏi yêu thích");
        btnRemove.addActionListener(e -> removeFavorite(favorite.getId(), movie));

        buttonPanel.add(btnDetail, "growx");
        buttonPanel.add(btnRemove, "w 50!");

        card.add(imgLabel, "growx");
        card.add(title, "growx");
        card.add(addedDate, "growx");
        card.add(genre);
        card.add(buttonPanel, "growx");

        card.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        return card;
    }

    private void loadImageAsync(MovieResponse movie, JLabel imgLabel) {
        // Check cache first
        if (imageCache.containsKey(movie.getId())) {
            imgLabel.setIcon(imageCache.get(movie.getId()));
            imgLabel.setText("");
            return;
        }

        // Show loading text
        imgLabel.setText("⏳");
        imgLabel.putClientProperty(FlatClientProperties.STYLE, 
                imgLabel.getClientProperty(FlatClientProperties.STYLE) + "font:+10;");

        // Load in background
        new SwingWorker<ImageIcon, Void>() {
            @Override
            protected ImageIcon doInBackground() {
                try {
                    if (movie.getImageUrl() == null || movie.getImageUrl().isEmpty()) {
                        return null;
                    }

                    URL url = new URL(movie.getImageUrl());
                    java.net.URLConnection conn = url.openConnection();
                    conn.setConnectTimeout(5000);
                    conn.setReadTimeout(5000);
                    conn.setRequestProperty("User-Agent", "Mozilla/5.0");
                    
                    BufferedImage img = ImageIO.read(conn.getInputStream());
                    
                    if (img != null) {
                        Image scaled = img.getScaledInstance(200, 280, Image.SCALE_SMOOTH);
                        return new ImageIcon(scaled);
                    }
                } catch (Exception e) {
                    System.err.println("Error loading image: " + e.getMessage());
                }
                return null;
            }

            @Override
            protected void done() {
                try {
                    ImageIcon icon = get();
                    if (icon != null) {
                        imgLabel.setIcon(icon);
                        imgLabel.setText("");
                        imageCache.put(movie.getId(), icon);
                    } else {
                        imgLabel.setText("📽️");
                        imgLabel.putClientProperty(FlatClientProperties.STYLE, 
                                imgLabel.getClientProperty(FlatClientProperties.STYLE) + "font:+20;");
                    }
                } catch (Exception e) {
                    imgLabel.setText("❌");
                }
            }
        }.execute();
    }

    private void removeFavorite(Long favoriteId, MovieResponse movie) {
        int confirm = JOptionPane.showConfirmDialog(
                this,
                "Bạn có chắc muốn xóa \"" + movie.getTitle() + "\" khỏi danh sách yêu thích?",
                "Xác nhận",
                JOptionPane.YES_NO_OPTION
        );

        if (confirm == JOptionPane.YES_OPTION) {
            new SwingWorker<ApiResponse<Void>, Void>() {
                @Override
                protected ApiResponse<Void> doInBackground() {
                    return FavoriteApi.removeFavorite(getCurrentUserId(), movie.getId());
                }

                @Override
                protected void done() {
                    try {
                        ApiResponse<Void> response = get();
                        if (response != null && response.getCode() == 200) {
                            Toast.show(FormFavoriteMovies.this, Toast.Type.SUCCESS, "Đã xóa khỏi yêu thích");
                            loadFavorites(); // Reload
                        } else {
                            Toast.show(FormFavoriteMovies.this, Toast.Type.ERROR, 
                                    "Lỗi: " + (response != null ? response.getMessage() : "Unknown"));
                        }
                    } catch (Exception e) {
                        Toast.show(FormFavoriteMovies.this, Toast.Type.ERROR, "Lỗi: " + e.getMessage());
                        e.printStackTrace();
                    }
                }
            }.execute();
        }
    }

    private void updateHeader() {
        removeAll();
        add(createHeader());
        add(scrollPane, "grow");
        add(createPagination(), "growx");
        revalidate();
        repaint();
    }

    private void updatePagination() {
        // Will be updated in updateHeader()
    }

    private void showError(String message) {
        gridPanel.removeAll();
        JLabel error = new JLabel(message, SwingConstants.CENTER);
        error.putClientProperty(FlatClientProperties.STYLE, "font:+2;foreground:#ef4444;");
        gridPanel.add(error, "span,grow");
        gridPanel.revalidate();
        gridPanel.repaint();
        Toast.show(this, Toast.Type.ERROR, message);
    }
}
