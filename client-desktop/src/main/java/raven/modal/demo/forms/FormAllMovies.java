package raven.modal.demo.forms;

import com.formdev.flatlaf.FlatClientProperties;
import net.miginfocom.swing.MigLayout;
import raven.modal.ModalDialog;
import raven.modal.Toast;
import raven.modal.component.SimpleModalBorder;
import raven.modal.demo.api.GenreApi;
import raven.modal.demo.api.MovieApi;
import raven.modal.demo.dto.response.ApiResponse;
import raven.modal.demo.dto.response.GenreResponse;
import raven.modal.demo.dto.response.MovieResponse;
import raven.modal.demo.system.Form;
import raven.modal.demo.system.FormManager;
import raven.modal.demo.utils.SystemForm;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@SystemForm(name = "All Movies", description = "Browse all movies with async image loading")
public class FormAllMovies extends Form {

    private JPanel gridPanel;
    private JPanel genresPanel;
    private JScrollPane scrollPane;
    private JComboBox<String> cmbSort;
    private List<MovieResponse> movies = new ArrayList<>();
    private List<GenreResponse> genres = new ArrayList<>();
    private Long selectedGenreId = null;
    private Map<Long, ImageIcon> imageCache = new HashMap<>();
    private Map<Long, JPanel> movieCardCache = new HashMap<>();
    private Map<Long, JButton> genreButtons = new HashMap<>();
    private JButton btnAllGenres;

    public FormAllMovies() {
        init();
    }

    private void init() {
        setLayout(new MigLayout("wrap,fillx,insets 15", "[fill]", "[]10[]10[fill]"));

        // Header with search and sort
        add(createHeader());

        // Genre chips
        genresPanel = new JPanel(new MigLayout("insets 0,gap 8", "[]", "[]"));
        genresPanel.putClientProperty(FlatClientProperties.STYLE, "background:null;");
        add(genresPanel, "growx");
        
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
    }

    @Override
    public void formInit() {
        loadGenres();
        loadMovies();
    }

    @Override
    public void formRefresh() {
        loadGenres();
        loadMovies();
    }

    private void loadGenres() {
        genresPanel.removeAll();
        genresPanel.add(new JLabel("Đang tải thể loại..."));
        genresPanel.revalidate();
        genresPanel.repaint();

        new SwingWorker<ApiResponse<List<GenreResponse>>, Void>() {
            @Override
            protected ApiResponse<List<GenreResponse>> doInBackground() {
                return GenreApi.getAllGenres();
            }

            @Override
            protected void done() {
                try {
                    ApiResponse<List<GenreResponse>> response = get();
                    if (response != null && response.getCode() == 200) {
                        genres = response.getResult();
                        createGenreChips();
                    } else {
                        genresPanel.removeAll();
                        genresPanel.add(new JLabel("Lỗi tải thể loại"));
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                genresPanel.revalidate();
                genresPanel.repaint();
            }
        }.execute();
    }

    private void createGenreChips() {
        genresPanel.removeAll();
        genreButtons.clear();

        // "All" chip
        btnAllGenres = new JButton("Tất cả");
        styleChip(btnAllGenres, selectedGenreId == null);
        btnAllGenres.addActionListener(e -> {
            if (selectedGenreId != null) {
                selectedGenreId = null;
                updateGenreChipStyles();
                filterMovies();
            }
        });
        genresPanel.add(btnAllGenres);

        for (GenreResponse genre : genres) {
            JButton btn = new JButton(genre.getName());
            styleChip(btn, selectedGenreId != null && selectedGenreId.equals(genre.getId()));
            btn.addActionListener(e -> {
                if (selectedGenreId == null || !selectedGenreId.equals(genre.getId())) {
                    selectedGenreId = genre.getId();
                    updateGenreChipStyles();
                    filterMovies();
                }
            });
            genreButtons.put(genre.getId(), btn);
            genresPanel.add(btn);
        }
        genresPanel.revalidate();
        genresPanel.repaint();
    }

    private void updateGenreChipStyles() {
        styleChip(btnAllGenres, selectedGenreId == null);
        for (Map.Entry<Long, JButton> entry : genreButtons.entrySet()) {
            styleChip(entry.getValue(), selectedGenreId != null && selectedGenreId.equals(entry.getKey()));
        }
    }

    private void styleChip(JButton btn, boolean isActive) {
        if (isActive) {
            btn.putClientProperty(FlatClientProperties.STYLE, "" +
                    "arc:999;" +
                    "margin:4,12,4,12;" +
                    "borderWidth:2;" +
                    "borderColor:#2196F3;" +
                    "focusWidth:0;" +
                    "innerFocusWidth:0;");
        } else {
            btn.putClientProperty(FlatClientProperties.STYLE, "" +
                    "arc:999;" +
                    "margin:4,12,4,12;" +
                    "focusWidth:0;" +
                    "innerFocusWidth:0;");
        }
    }

    private JPanel createHeader() {
        JPanel panel = new JPanel(new MigLayout("fillx,insets 0", "[grow]10[]", "[]"));
        panel.putClientProperty(FlatClientProperties.STYLE, "background:null;");

        JLabel title = new JLabel("Tất cả phim");
        title.putClientProperty(FlatClientProperties.STYLE, "font:bold +5;");

        cmbSort = new JComboBox<>(new String[]{"Mới nhất", "Tên A-Z", "Năm phát hành"});
        cmbSort.putClientProperty(FlatClientProperties.STYLE, "arc:12;");
        cmbSort.addActionListener(e -> sortMovies());

        panel.add(title, "split 2,aligny center");
        panel.add(new JLabel("(" + movies.size() + " phim)"), "aligny center");
        panel.add(cmbSort, "w 150!");

        return panel;
    }

    private void loadMovies() {
        // Clear grid
        gridPanel.removeAll();
        movieCardCache.clear(); // Clear cache when reloading all movies
        
        // Show loading
        JLabel loading = new JLabel("Đang tải phim...", SwingConstants.CENTER);
        loading.putClientProperty(FlatClientProperties.STYLE, "font:+2;");
        gridPanel.add(loading, "span,grow");
        gridPanel.revalidate();
        gridPanel.repaint();

        // Load in background
        new SwingWorker<ApiResponse<List<MovieResponse>>, Void>() {
            @Override
            protected ApiResponse<List<MovieResponse>> doInBackground() {
                return MovieApi.getAllMovies();
            }

            @Override
            protected void done() {
                try {
                    ApiResponse<List<MovieResponse>> response = get();
                    if (response != null && response.getCode() == 200) {
                        movies = response.getResult();
                        // Pre-create cards in background or just clear loading and let displayMovies handle it
                        // For now, just clear loading
                        gridPanel.removeAll();
                        displayMovies();
                        updateHeader();
                    } else {
                        showError("Lỗi tải phim: " + (response != null ? response.getMessage() : "Unknown"));
                    }
                } catch (Exception e) {
                    showError("Lỗi: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        }.execute();
    }

    private void displayMovies() {
        gridPanel.removeAll();

        List<MovieResponse> filtered = new ArrayList<>();

        for (MovieResponse m : movies) {
            boolean matchGenre = selectedGenreId == null || (m.getGenre() != null && m.getGenre().getId().equals(selectedGenreId));

            if (matchGenre) {
                filtered.add(m);
            }
        }

        // Sort
        int sortIndex = cmbSort.getSelectedIndex();
        filtered.sort((m1, m2) -> {
            switch (sortIndex) {
                case 0: // Mới nhất (ID desc)
                    return Long.compare(m2.getId(), m1.getId());
                case 1: // Tên A-Z
                    return m1.getTitle().compareToIgnoreCase(m2.getTitle());
                case 2: // Năm phát hành (Year desc)
                    int y1 = m1.getReleaseYear() != null ? m1.getReleaseYear() : 0;
                    int y2 = m2.getReleaseYear() != null ? m2.getReleaseYear() : 0;
                    return Integer.compare(y2, y1);
                default:
                    return 0;
            }
        });

        if (filtered.isEmpty()) {
            JLabel empty = new JLabel("Không tìm thấy phim nào", SwingConstants.CENTER);
            empty.putClientProperty(FlatClientProperties.STYLE, "font:+2;foreground:$Label.disabledForeground;");
            gridPanel.add(empty, "span,grow");
        } else {
            for (MovieResponse movie : filtered) {
                gridPanel.add(createMovieCard(movie));
            }
        }

        gridPanel.revalidate();
        gridPanel.repaint();
    }

    private JPanel createMovieCard(MovieResponse movie) {
        if (movieCardCache.containsKey(movie.getId())) {
            return movieCardCache.get(movie.getId());
        }

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

        JLabel genre = new JLabel(movie.getGenre() != null ? movie.getGenre().getName() : "N/A");
        genre.putClientProperty(FlatClientProperties.STYLE, "foreground:$Label.disabledForeground;");

        JLabel year = new JLabel("" + movie.getReleaseYear());
        year.putClientProperty(FlatClientProperties.STYLE, "foreground:$Label.disabledForeground;");

        JLabel status = new JLabel(movie.getStatusDisplay());
        status.setOpaque(true);
        status.putClientProperty(FlatClientProperties.STYLE, "" +
                "arc:999;border:4,8,4,8;" +
                "[light]background:tint($Panel.background,15%);" +
                "[dark]background:tint($Panel.background,10%);");

        JButton btnDetail = new JButton("Chi tiết");
        btnDetail.putClientProperty(FlatClientProperties.STYLE, "arc:12;");
        btnDetail.addActionListener(e -> {
            FormMovieDetail detailForm = new FormMovieDetail(movie);
            FormManager.showForm(detailForm);
        });

        card.add(imgLabel, "growx");
        card.add(title, "growx");
        card.add(genre, "split 2");
        card.add(year);
        card.add(status, "growx");
        card.add(btnDetail, "growx");

        card.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        movieCardCache.put(movie.getId(), card);
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
                        System.err.println("Movie " + movie.getId() + " has no image URL");
                        return null;
                    }

                    System.out.println("Loading image for movie " + movie.getId() + ": " + movie.getImageUrl());
                    
                    URL url = new URL(movie.getImageUrl());
                    java.net.URLConnection conn = url.openConnection();
                    conn.setConnectTimeout(5000);
                    conn.setReadTimeout(5000);
                    conn.setRequestProperty("User-Agent", "Mozilla/5.0");
                    
                    BufferedImage img = ImageIO.read(conn.getInputStream());
                    
                    if (img != null) {
                        System.out.println("Successfully loaded image for movie " + movie.getId());
                        // Resize to fit
                        Image scaled = img.getScaledInstance(200, 280, Image.SCALE_SMOOTH);
                        return new ImageIcon(scaled);
                    } else {
                        System.err.println("ImageIO.read returned null for movie " + movie.getId());
                    }
                } catch (Exception e) {
                    System.err.println("Error loading image for movie " + movie.getId() + ": " + e.getClass().getSimpleName() + " - " + e.getMessage());
                    e.printStackTrace();
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
                    System.err.println("Error in done() for movie " + movie.getId() + ": " + e.getMessage());
                }
            }
        }.execute();
    }

    private void filterMovies() {
        displayMovies();
    }

    private void sortMovies() {
        // TODO: Implement sorting
        displayMovies();
    }

    private void updateHeader() {
        // Rebuild header to update count
        removeAll();
        add(createHeader());
        add(genresPanel, "growx");
        add(scrollPane, "grow");
        revalidate();
        repaint();
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
