package raven.modal.demo.forms;

import com.formdev.flatlaf.FlatClientProperties;
import net.miginfocom.swing.MigLayout;
import raven.modal.Toast;
import raven.modal.demo.api.FavoriteApi;
import raven.modal.demo.api.ReviewApi;
import raven.modal.demo.api.WatchHistoryApi;
import raven.modal.demo.dto.request.MovieReviewRequest;
import raven.modal.demo.dto.response.ApiResponse;
import raven.modal.demo.dto.response.MovieRatingSummaryResponse;
import raven.modal.demo.dto.response.MovieResponse;
import raven.modal.demo.dto.response.MovieReviewResponse;
import raven.modal.demo.menu.MyDrawerBuilder;
import raven.modal.demo.system.Form;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.net.URL;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

public class FormMovieDetail extends Form {
    
    private final MovieResponse movie;
    private JPanel contentContainer;
    private JLabel imageLabel;
    private JButton favoriteButton;
    private boolean isFavorited = false;
    private JLabel ratingSummaryLabel;
    private JSpinner ratingSpinner;
    private JTextArea reviewCommentArea;
    private JButton submitReviewButton;
    private JPanel reviewsListPanel;
    
    private Long getCurrentUserId() {
        var user = MyDrawerBuilder.getInstance().getUser();
        return user != null ? user.getUserId() : 1L;
    }
    
    public FormMovieDetail(MovieResponse movie) {
        this.movie = movie;
        init();
        // Lưu lịch sử xem khi mở chi tiết phim
        saveWatchHistory();
        // Kiểm tra trạng thái yêu thích
        checkFavoriteStatus();
        // Load review và thống kê rating
        loadReviewsAndSummary();
    }
    
    private void init() {
        setLayout(new BorderLayout());

        contentContainer = new JPanel(new MigLayout("wrap,fill,insets 18 20 18 20,hidemode 3", "[fill]", "[]16[]16[grow,fill]16[]"));
        contentContainer.setOpaque(false);

        JScrollPane pageScrollPane = new JScrollPane(contentContainer);
        pageScrollPane.setBorder(BorderFactory.createEmptyBorder());
        pageScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        pageScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        pageScrollPane.getVerticalScrollBar().setUnitIncrement(16);
        add(pageScrollPane, BorderLayout.CENTER);
        
        // Title
        JLabel titleLabel = new JLabel(movie.getTitle());
        titleLabel.setFont(new Font("Arial", Font.BOLD, 28));
        titleLabel.putClientProperty(FlatClientProperties.STYLE, "foreground:$Component.accentColor");
        contentContainer.add(titleLabel, "wrap");
        
        // Main content panel with image and details
        JPanel contentPanel = new JPanel(new MigLayout("insets 14,fillx", "[300!]20[grow,fill]", "[top]"));
        contentPanel.putClientProperty(FlatClientProperties.STYLE,
            "arc:16;border:1,1,1,1,$Component.borderColor;background:lighten($Panel.background,2%)");
        
        // Left side - Movie poster
        JPanel imagePanel = new JPanel(new BorderLayout());
        imagePanel.setOpaque(false);
        imageLabel = new JLabel("⏳ Đang tải ảnh...", SwingConstants.CENTER);
        imageLabel.setPreferredSize(new Dimension(300, 420));
        imageLabel.putClientProperty(FlatClientProperties.STYLE,
            "arc:12;border:1,1,1,1,$Component.borderColor;background:$Panel.background");
        imagePanel.add(imageLabel, BorderLayout.CENTER);
        contentPanel.add(imagePanel);
        
        // Load image asynchronously
        loadMovieImage();
        
        // Right side - Movie details
        JPanel detailsPanel = new JPanel(new MigLayout("wrap 2,fillx,insets 2", "[130!,right]12[grow,fill]", "[]8[]"));
        detailsPanel.setOpaque(false);

        
        // Status
        String statusText = switch (movie.getStatus()) {
            case "PUBLISHED" -> "Đã xuất bản";
            case "DRAF" -> "Không hoạt động";
            case "PROCESSING" -> "Chưa sẵn sàng";
            case "FAILED" -> "Xử lý thất bại";
            default -> movie.getStatus();
        };
        addDetailRow(detailsPanel, "Trạng thái:", statusText);
        
        // Release year
        if (movie.getReleaseYear() != null) {
            addDetailRow(detailsPanel, "Năm phát hành:", String.valueOf(movie.getReleaseYear()));
        }
        
        // Duration
        if (movie.getDuration() != null) {
            addDetailRow(detailsPanel, "Thời lượng:", formatDuration(movie.getDuration()));
        }
        
        // Genres
        if (movie.getGenre() != null) {
            addDetailRow(detailsPanel, "Thể loại:", movie.getGenre().getName());
        }
        
        // Created date
        if (movie.getCreatedAt() != null) {
            addDetailRow(detailsPanel, "Ngày tạo:", movie.getCreatedAt().toString());
        }
        
        // Description (full width)
        if (movie.getDescription() != null && !movie.getDescription().isEmpty()) {
            detailsPanel.add(new JLabel("Mô tả:"), "aligny top");
            JTextArea descriptionArea = new JTextArea(movie.getDescription());
            descriptionArea.setEditable(false);
            descriptionArea.setLineWrap(true);
            descriptionArea.setWrapStyleWord(true);
            descriptionArea.setRows(5);
            descriptionArea.putClientProperty(FlatClientProperties.STYLE, 
                "border:1,1,1,1,$Component.borderColor,1,10");
            JScrollPane scrollPane = new JScrollPane(descriptionArea);
            scrollPane.setPreferredSize(new Dimension(400, 120));
            detailsPanel.add(scrollPane, "growx");
        }
        
        // Video qualities (processing status)
        if (movie.getVideoQualities() != null && !movie.getVideoQualities().isEmpty()) {
            String qualitiesText = movie.getVideoQualities().stream()
                    .map(vq -> vq.getQuality() + " (" + vq.getResolution() + ")")
                    .collect(Collectors.joining(", "));
            addDetailRow(detailsPanel, "Chất lượng:", qualitiesText);
        }
        
        contentPanel.add(detailsPanel);
        contentContainer.add(contentPanel, "growx");

        contentContainer.add(createReviewPanel(), "grow,pushy");
        
        // Action buttons (fixed at bottom, outside scroll area)
        JPanel buttonPanel = new JPanel(new MigLayout("insets 8 20 12 20", "[grow,right][]10[]"));
        buttonPanel.setOpaque(false);
        
        // Favorite button
        favoriteButton = new JButton(isFavorited ? "💖 Đã yêu thích" : "🤍 Yêu thích");
        favoriteButton.putClientProperty(FlatClientProperties.STYLE, 
            "borderWidth:1;focusWidth:1;arc:10");
        favoriteButton.addActionListener(e -> toggleFavorite());
        buttonPanel.add(favoriteButton);
        
        // Watch button
        JButton watchButton = new JButton("▶ Xem phim");
        watchButton.putClientProperty(FlatClientProperties.STYLE, 
            "borderWidth:0;focusWidth:1;arc:10;background:$Component.accentColor;foreground:#fff");
        
        // Check if movie is ready to watch
        boolean isReady = movie.getStatus().equals("PUBLISHED") && 
                         movie.getVideoQualities() != null &&
                         !movie.getVideoQualities().isEmpty();
        
        if (!isReady) {
            watchButton.setEnabled(false);
            watchButton.setText("⏳ Đang xử lý video");
        }
        
        watchButton.addActionListener(e -> {
            System.out.println("Watch button clicked for movie: " + movie.getTitle());
            try {
                VideoPlayerForm playerForm = new VideoPlayerForm();
                playerForm.setMovie(movie);
                raven.modal.demo.system.FormManager.showForm(playerForm);
            } catch (Exception ex) {
                System.err.println("Error opening video player: " + ex.getMessage());
                ex.printStackTrace();
                JOptionPane.showMessageDialog(this, 
                    "Lỗi mở video player: " + ex.getMessage(), 
                    "Lỗi", 
                    JOptionPane.ERROR_MESSAGE);
            }
        });
        
        buttonPanel.add(watchButton);
        add(buttonPanel, BorderLayout.SOUTH);
    }

    private JPanel createReviewPanel() {
        JPanel reviewPanel = new JPanel(new MigLayout("wrap,fill,insets 14", "[grow,fill]", "[]12[]12[grow,fill]"));
        reviewPanel.putClientProperty(FlatClientProperties.STYLE,
            "arc:16;border:1,1,1,1,$Component.borderColor;background:lighten($Panel.background,3%)");

        JPanel headerPanel = new JPanel(new MigLayout("insets 0,fillx", "[grow]", "[]"));
        headerPanel.setOpaque(false);

        JLabel sectionTitle = new JLabel("Đánh giá & bình luận");
        sectionTitle.setFont(sectionTitle.getFont().deriveFont(Font.BOLD, 18f));
        headerPanel.add(sectionTitle, "split 2");

        ratingSummaryLabel = new JLabel(buildRatingSummaryText(movie.getAverageRating(), movie.getRatingCount()));
        ratingSummaryLabel.putClientProperty(FlatClientProperties.STYLE,
            "foreground:$Component.accentColor;font:bold +2");
        headerPanel.add(ratingSummaryLabel, "gapleft push");
        reviewPanel.add(headerPanel, "growx");

        JPanel inputCard = new JPanel(new MigLayout("wrap 2,fillx,insets 12", "[110!,right]12[grow,fill]", "[]10[]10[]"));
        inputCard.putClientProperty(FlatClientProperties.STYLE,
            "arc:12;border:1,1,1,1,$Component.borderColor;background:$Panel.background");

        inputCard.add(new JLabel("Chấm điểm:"));

        ratingSpinner = new JSpinner(new SpinnerNumberModel(5, 1, 5, 1));
        ratingSpinner.setPreferredSize(new Dimension(84, 30));
        inputCard.add(ratingSpinner, "left,w 90!");

        inputCard.add(new JLabel("Bình luận:"), "top");
        reviewCommentArea = new JTextArea(4, 40);
        reviewCommentArea.setLineWrap(true);
        reviewCommentArea.setWrapStyleWord(true);
        reviewCommentArea.putClientProperty(FlatClientProperties.STYLE,
            "border:6,8,6,8,$Component.borderColor,1,8");
        JScrollPane reviewScrollPane = new JScrollPane(reviewCommentArea);
        reviewScrollPane.setPreferredSize(new Dimension(0, 110));
        inputCard.add(reviewScrollPane, "growx");

        submitReviewButton = new JButton("Gửi đánh giá");
        submitReviewButton.putClientProperty(FlatClientProperties.STYLE,
                "borderWidth:0;focusWidth:1;arc:10;background:$Component.accentColor;foreground:#fff");
        submitReviewButton.addActionListener(e -> submitReview());
        inputCard.add(submitReviewButton, "skip 1,growx,h 34!");

        reviewPanel.add(inputCard, "growx");

        JPanel listCard = new JPanel(new MigLayout("wrap,fill,insets 12", "[grow,fill]", "[][grow,fill]"));
        listCard.putClientProperty(FlatClientProperties.STYLE,
            "arc:12;border:1,1,1,1,$Component.borderColor;background:$Panel.background");
        JLabel listTitle = new JLabel("Bình luận gần đây");
        listTitle.setFont(listTitle.getFont().deriveFont(Font.BOLD, 14f));
        listCard.add(listTitle, "growx");

        reviewsListPanel = new JPanel();
        reviewsListPanel.setLayout(new BoxLayout(reviewsListPanel, BoxLayout.Y_AXIS));
        reviewsListPanel.setOpaque(false);
        JScrollPane listScrollPane = new JScrollPane(reviewsListPanel);
        listScrollPane.getVerticalScrollBar().setUnitIncrement(14);
        listScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        listScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        listScrollPane.setPreferredSize(new Dimension(0, 320));
        listScrollPane.setMinimumSize(new Dimension(0, 220));
        listCard.add(listScrollPane, "grow,pushy,hmin 240");

        reviewPanel.add(listCard, "grow,pushy");

        return reviewPanel;
    }
    
    private void addDetailRow(JPanel panel, String label, String value) {
        JLabel labelComponent = new JLabel(label);
        labelComponent.setFont(labelComponent.getFont().deriveFont(Font.BOLD));
        panel.add(labelComponent, "aligny top");
        
        JLabel valueComponent = new JLabel("<html><div style=\"line-height:1.4;\">" + value + "</div></html>");
        panel.add(valueComponent, "growx");
    }
    
    private String formatDuration(Integer minutes) {
        if (minutes == null || minutes == 0) {
            return "N/A";
        }
        int hours = minutes / 60;
        int mins = minutes % 60;
        if (hours > 0) {
            return hours + " giờ " + mins + " phút";
        } else {
            return mins + " phút";
        }
    }
    
    private void loadMovieImage() {
        if (movie.getImageUrl() == null || movie.getImageUrl().isEmpty()) {
            imageLabel.setText("📽️ Không có ảnh");
            return;
        }
        
        SwingWorker<ImageIcon, Void> worker = new SwingWorker<>() {
            @Override
            protected ImageIcon doInBackground() throws Exception {
                System.out.println("Loading movie detail image: " + movie.getImageUrl());
                
                URL url = new URL(movie.getImageUrl());
                java.net.URLConnection conn = url.openConnection();
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(5000);
                conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36");
                
                BufferedImage img = ImageIO.read(conn.getInputStream());
                
                if (img == null) {
                    System.err.println("ImageIO.read returned null for movie " + movie.getId());
                    return null;
                }
                
                System.out.println("Successfully loaded image for movie detail: " + movie.getId());
                
                // Scale image to fit 300x420 maintaining aspect ratio
                int targetWidth = 300;
                int targetHeight = 420;
                
                double aspectRatio = (double) img.getWidth() / img.getHeight();
                int scaledWidth = targetWidth;
                int scaledHeight = (int) (scaledWidth / aspectRatio);
                
                if (scaledHeight > targetHeight) {
                    scaledHeight = targetHeight;
                    scaledWidth = (int) (scaledHeight * aspectRatio);
                }
                
                Image scaledImage = img.getScaledInstance(scaledWidth, scaledHeight, Image.SCALE_SMOOTH);
                return new ImageIcon(scaledImage);
            }
            
            @Override
            protected void done() {
                try {
                    ImageIcon icon = get();
                    if (icon != null) {
                        imageLabel.setIcon(icon);
                        imageLabel.setText(null);
                    } else {
                        imageLabel.setText("📽️ Lỗi tải ảnh");
                    }
                } catch (Exception e) {
                    System.err.println("Error loading image for movie " + movie.getId() + ": " + e.getMessage());
                    e.printStackTrace();
                    imageLabel.setText("❌ Lỗi tải ảnh");
                }
            }
        };
        
        worker.execute();
    }
    
    private void saveWatchHistory() {
        // Lưu lịch sử xem trong background
        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() {
                try {
                    WatchHistoryApi.addOrUpdateWatchHistory(getCurrentUserId(), movie.getId());
                    System.out.println("Watch history saved for movie: " + movie.getTitle());
                } catch (Exception e) {
                    System.err.println("Error saving watch history: " + e.getMessage());
                }
                return null;
            }
        }.execute();
    }
    
    private void checkFavoriteStatus() {
        new SwingWorker<Boolean, Void>() {
            @Override
            protected Boolean doInBackground() {
                try {
                    ApiResponse<Boolean> response = FavoriteApi.checkFavorite(getCurrentUserId(), movie.getId());
                    return response != null && response.getCode() == 200 && Boolean.TRUE.equals(response.getResult());
                } catch (Exception e) {
                    System.err.println("Error checking favorite status: " + e.getMessage());
                    return false;
                }
            }

            @Override
            protected void done() {
                try {
                    isFavorited = get();
                    if (favoriteButton != null) {
                        updateFavoriteButton();
                    }
                } catch (Exception e) {
                    System.err.println("Error updating favorite button: " + e.getMessage());
                }
            }
        }.execute();
    }
    
    private void toggleFavorite() {
        favoriteButton.setEnabled(false);
        
        new SwingWorker<ApiResponse<?>, Void>() {
            @Override
            protected ApiResponse<?> doInBackground() {
                if (isFavorited) {
                    return FavoriteApi.removeFavorite(getCurrentUserId(), movie.getId());
                } else {
                    return FavoriteApi.addFavorite(getCurrentUserId(), movie.getId());
                }
            }

            @Override
            protected void done() {
                try {
                    ApiResponse<?> response = get();
                    if (response != null && response.getCode() == 200) {
                        isFavorited = !isFavorited;
                        updateFavoriteButton();
                        Toast.show(FormMovieDetail.this, Toast.Type.SUCCESS, 
                                isFavorited ? "Đã thêm vào yêu thích" : "Đã xóa khỏi yêu thích");
                    } else {
                        Toast.show(FormMovieDetail.this, Toast.Type.ERROR, 
                                "Lỗi: " + (response != null ? response.getMessage() : "Unknown"));
                    }
                } catch (Exception e) {
                    Toast.show(FormMovieDetail.this, Toast.Type.ERROR, "Lỗi: " + e.getMessage());
                    e.printStackTrace();
                } finally {
                    favoriteButton.setEnabled(true);
                }
            }
        }.execute();
    }
    
    private void updateFavoriteButton() {
        favoriteButton.setText(isFavorited ? "💖 Đã yêu thích" : "🤍 Yêu thích");
    }

    private void loadReviewsAndSummary() {
        new SwingWorker<ReviewPageResult, Void>() {
            @Override
            protected ReviewPageResult doInBackground() {
                try {
                    ApiResponse<MovieRatingSummaryResponse> summaryResponse = ReviewApi.getMovieRatingSummary(movie.getId());
                    ApiResponse<List<MovieReviewResponse>> reviewsResponse = ReviewApi.getMovieReviews(movie.getId(), 0, 20);

                    MovieRatingSummaryResponse summary = summaryResponse != null && summaryResponse.getCode() == 200
                            ? summaryResponse.getResult()
                            : null;
                    List<MovieReviewResponse> reviews = reviewsResponse != null && reviewsResponse.getCode() == 200
                            ? reviewsResponse.getResult()
                            : java.util.Collections.emptyList();

                    return new ReviewPageResult(summary, reviews);
                } catch (Exception e) {
                    System.err.println("Error loading reviews: " + e.getMessage());
                    return new ReviewPageResult(null, java.util.Collections.emptyList());
                }
            }

            @Override
            protected void done() {
                try {
                    ReviewPageResult result = get();
                    if (result.summary != null) {
                        ratingSummaryLabel.setText(buildRatingSummaryText(
                                result.summary.getAverageRating(),
                                result.summary.getRatingCount()
                        ));
                    }
                    renderReviews(result.reviews);
                } catch (Exception e) {
                    System.err.println("Error rendering reviews: " + e.getMessage());
                }
            }
        }.execute();
    }

    private void submitReview() {
        submitReviewButton.setEnabled(false);

        new SwingWorker<ApiResponse<MovieReviewResponse>, Void>() {
            @Override
            protected ApiResponse<MovieReviewResponse> doInBackground() {
                MovieReviewRequest request = MovieReviewRequest.builder()
                        .userId(getCurrentUserId())
                        .rating((Integer) ratingSpinner.getValue())
                        .comment(reviewCommentArea.getText())
                        .build();
                return ReviewApi.upsertReview(movie.getId(), request);
            }

            @Override
            protected void done() {
                try {
                    ApiResponse<MovieReviewResponse> response = get();
                    if (response != null && response.getCode() == 200) {
                        Toast.show(FormMovieDetail.this, Toast.Type.SUCCESS, "Đã gửi đánh giá thành công");
                        reviewCommentArea.setText("");
                        loadReviewsAndSummary();
                    } else {
                        Toast.show(FormMovieDetail.this, Toast.Type.ERROR,
                                "Không thể gửi đánh giá: " + (response != null ? response.getMessage() : "Unknown"));
                    }
                } catch (Exception e) {
                    Toast.show(FormMovieDetail.this, Toast.Type.ERROR, "Lỗi gửi đánh giá: " + e.getMessage());
                } finally {
                    submitReviewButton.setEnabled(true);
                }
            }
        }.execute();
    }

    private void renderReviews(List<MovieReviewResponse> reviews) {
        reviewsListPanel.removeAll();

        if (reviews == null || reviews.isEmpty()) {
            JLabel emptyLabel = new JLabel("Chưa có bình luận nào.");
            emptyLabel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
            reviewsListPanel.add(emptyLabel);
        } else {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
            for (MovieReviewResponse review : reviews) {
                JPanel item = new JPanel(new MigLayout("wrap,fillx,insets 10", "[fill]", "[]6[]6[]"));
                item.putClientProperty(FlatClientProperties.STYLE,
                        "arc:10;border:1,1,1,1,$Component.borderColor;background:lighten($Panel.background,6%)");

                String author = review.getUserFullName() != null && !review.getUserFullName().isBlank()
                        ? review.getUserFullName()
                        : "Người dùng";
                JLabel userLabel = new JLabel(author + "  " + renderStars(review.getRating()));
                userLabel.setFont(userLabel.getFont().deriveFont(Font.BOLD));
                item.add(userLabel, "growx");

                String comment = review.getComment() == null || review.getComment().isBlank()
                        ? "(Không có bình luận)"
                        : review.getComment();
                JLabel commentLabel = new JLabel("<html>" + escapeHtml(comment).replace("\n", "<br>") + "</html>");
                item.add(commentLabel, "growx");

                String timeText = review.getUpdatedAt() != null ? review.getUpdatedAt().format(formatter) : "";
                JLabel timeLabel = new JLabel(timeText);
                timeLabel.putClientProperty(FlatClientProperties.STYLE, "foreground:shade($Label.foreground,35%)");
                item.add(timeLabel, "right");

                reviewsListPanel.add(item);
                reviewsListPanel.add(Box.createVerticalStrut(8));
            }
        }

        reviewsListPanel.revalidate();
        reviewsListPanel.repaint();
    }

    private String buildRatingSummaryText(java.math.BigDecimal averageRating, Long ratingCount) {
        long count = ratingCount != null ? ratingCount : 0;
        if (averageRating == null || count == 0) {
            return "Chưa có đánh giá";
        }
        return "⭐ " + averageRating + "/5 (" + count + " lượt)";
    }

    private String renderStars(Integer rating) {
        int value = rating != null ? Math.max(1, Math.min(5, rating)) : 1;
        return "★".repeat(value) + "☆".repeat(5 - value);
    }

    private String escapeHtml(String text) {
        return text
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    private static class ReviewPageResult {
        private final MovieRatingSummaryResponse summary;
        private final List<MovieReviewResponse> reviews;

        private ReviewPageResult(MovieRatingSummaryResponse summary, List<MovieReviewResponse> reviews) {
            this.summary = summary;
            this.reviews = reviews;
        }
    }
}
