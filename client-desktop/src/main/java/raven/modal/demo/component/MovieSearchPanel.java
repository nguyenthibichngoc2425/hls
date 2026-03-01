package raven.modal.demo.component;

import com.formdev.flatlaf.FlatClientProperties;
import com.formdev.flatlaf.extras.FlatSVGIcon;
import net.miginfocom.swing.MigLayout;
import raven.modal.ModalDialog;
import raven.modal.demo.api.MovieApi;
import raven.modal.demo.dto.response.ApiResponse;
import raven.modal.demo.dto.response.MovieResponse;
import raven.modal.demo.forms.FormMovieDetail;
import raven.modal.demo.system.FormManager;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.PlainDocument;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

public class MovieSearchPanel extends JPanel {

    private final int SEARCH_MAX_LENGTH = 100;
    private JTextField textSearch;
    private JPanel panelResult;
    private List<MovieResultItem> movieItems = new ArrayList<>();

    public MovieSearchPanel() {
        init();
    }

    private void init() {
        setLayout(new MigLayout("fillx,insets 0,wrap", "[fill,600]"));
        
        textSearch = new JTextField();
        textSearch.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Tìm kiếm phim...");
        textSearch.putClientProperty(FlatClientProperties.TEXT_FIELD_LEADING_ICON, 
            new FlatSVGIcon("raven/modal/demo/icons/search.svg", 0.4f));
        textSearch.putClientProperty(FlatClientProperties.STYLE, "" +
                "border:3,3,3,3;" +
                "background:null;" +
                "showClearButton:true;");
        
        add(textSearch, "gap 17 17 0 0");
        add(new JSeparator(), "height 2!");
        
        panelResult = new JPanel(new MigLayout("fillx,wrap,insets 10", "[fill]"));
        panelResult.putClientProperty(FlatClientProperties.STYLE, "background:null;");
        
        JScrollPane scrollPane = new JScrollPane(panelResult);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.setPreferredSize(new Dimension(600, 400));
        scrollPane.getVerticalScrollBar().putClientProperty(FlatClientProperties.STYLE, "" +
                "trackArc:$ScrollBar.thumbArc;" +
                "thumbInsets:0,3,0,3;" +
                "trackInsets:0,3,0,3;" +
                "width:12;");
        
        add(scrollPane);
        
        installSearchField();
        showEmptyState();
    }

    private void installSearchField() {
        textSearch.setDocument(new PlainDocument() {
            @Override
            public void insertString(int offs, String str, AttributeSet a) throws BadLocationException {
                if (getLength() + str.length() <= SEARCH_MAX_LENGTH) {
                    super.insertString(offs, str, a);
                }
            }
        });
        
        textSearch.getDocument().addDocumentListener(new DocumentListener() {
            private String text;
            private javax.swing.Timer searchTimer;

            @Override
            public void insertUpdate(DocumentEvent e) {
                scheduleSearch();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                scheduleSearch();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                scheduleSearch();
            }

            private void scheduleSearch() {
                if (searchTimer != null) {
                    searchTimer.stop();
                }
                searchTimer = new javax.swing.Timer(300, e -> search());
                searchTimer.setRepeats(false);
                searchTimer.start();
            }

            private void search() {
                String st = textSearch.getText().trim();
                if (!st.equals(text)) {
                    text = st;
                    if (st.isEmpty()) {
                        showEmptyState();
                    } else {
                        searchMovies(st);
                    }
                }
            }
        });

        textSearch.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                switch (e.getKeyCode()) {
                    case KeyEvent.VK_UP:
                        moveSelection(true);
                        break;
                    case KeyEvent.VK_DOWN:
                        moveSelection(false);
                        break;
                    case KeyEvent.VK_ENTER:
                        openSelectedMovie();
                        break;
                }
            }
        });
    }

    private void searchMovies(String keyword) {
        panelResult.removeAll();
        movieItems.clear();
        
        JLabel loading = new JLabel("Đang tìm kiếm...", SwingConstants.CENTER);
        loading.putClientProperty(FlatClientProperties.STYLE, "foreground:$Label.disabledForeground;");
        panelResult.add(loading, "growx");
        panelResult.revalidate();
        panelResult.repaint();

        new SwingWorker<ApiResponse<List<MovieResponse>>, Void>() {
            @Override
            protected ApiResponse<List<MovieResponse>> doInBackground() {
                return MovieApi.searchMovies(keyword);
            }

            @Override
            protected void done() {
                try {
                    ApiResponse<List<MovieResponse>> response = get();
                    panelResult.removeAll();
                    movieItems.clear();

                    if (response != null && response.getCode() == 200 && response.getResult() != null) {
                        List<MovieResponse> movies = response.getResult();
                        if (movies.isEmpty()) {
                            showNoResults(keyword);
                        } else {
                            for (MovieResponse movie : movies) {
                                MovieResultItem item = new MovieResultItem(movie);
                                movieItems.add(item);
                                panelResult.add(item, "growx");
                            }
                            if (!movieItems.isEmpty()) {
                                setSelected(0);
                            }
                        }
                    } else {
                        showError("Lỗi tìm kiếm: " + (response != null ? response.getMessage() : "Unknown"));
                    }
                } catch (Exception e) {
                    showError("Lỗi: " + e.getMessage());
                    e.printStackTrace();
                }
                panelResult.revalidate();
                panelResult.repaint();
            }
        }.execute();
    }

    private void showEmptyState() {
        panelResult.removeAll();
        movieItems.clear();
        JLabel label = new JLabel("Nhập tên phim để tìm kiếm...", SwingConstants.CENTER);
        label.putClientProperty(FlatClientProperties.STYLE, "foreground:$Label.disabledForeground;font:+2;");
        panelResult.add(label, "growx");
        panelResult.revalidate();
        panelResult.repaint();
    }

    private void showNoResults(String keyword) {
        JLabel label = new JLabel("Không tìm thấy phim \"" + keyword + "\"", SwingConstants.CENTER);
        label.putClientProperty(FlatClientProperties.STYLE, "foreground:$Label.disabledForeground;font:+2;");
        panelResult.add(label, "growx");
    }

    private void showError(String message) {
        JLabel label = new JLabel(message, SwingConstants.CENTER);
        label.putClientProperty(FlatClientProperties.STYLE, "foreground:#ef4444;font:+1;");
        panelResult.add(label, "growx");
    }

    private void setSelected(int index) {
        for (int i = 0; i < movieItems.size(); i++) {
            movieItems.get(i).setSelected(index == i);
        }
    }

    private int getSelectedIndex() {
        for (int i = 0; i < movieItems.size(); i++) {
            if (movieItems.get(i).isSelected()) {
                return i;
            }
        }
        return -1;
    }

    private void moveSelection(boolean up) {
        if (movieItems.isEmpty()) return;
        int index = getSelectedIndex();
        int size = movieItems.size();
        if (index == -1) {
            index = up ? size - 1 : 0;
        } else {
            index = up ? (index == 0 ? size - 1 : index - 1) : (index == size - 1 ? 0 : index + 1);
        }
        setSelected(index);
    }

    private void openSelectedMovie() {
        int index = getSelectedIndex();
        if (index != -1) {
            movieItems.get(index).openMovie();
        }
    }

    public void searchGrabFocus() {
        textSearch.requestFocus();
    }

    public void clearSearch() {
        textSearch.setText("");
        showEmptyState();
    }

    // Inner class for movie result item
    private class MovieResultItem extends JPanel {
        private final MovieResponse movie;
        private boolean selected = false;

        public MovieResultItem(MovieResponse movie) {
            this.movie = movie;
            init();
        }

        private void init() {
            setLayout(new MigLayout("fillx,insets 8", "[]12[grow]", "[]"));
            putClientProperty(FlatClientProperties.STYLE, "" +
                    "arc:12;" +
                    "[light]background:tint($Panel.background,5%);" +
                    "[dark]background:tint($Panel.background,3%);");

            // Icon
            JLabel icon = new JLabel("🎬");
            icon.putClientProperty(FlatClientProperties.STYLE, "font:+8;");

            // Info panel
            JPanel infoPanel = new JPanel(new MigLayout("wrap,fillx,insets 0", "[fill]"));
            infoPanel.putClientProperty(FlatClientProperties.STYLE, "background:null;");

            JLabel titleLabel = new JLabel(movie.getTitle());
            titleLabel.putClientProperty(FlatClientProperties.STYLE, "font:bold;");

            String info = movie.getReleaseYear() != null ? movie.getReleaseYear() + "" : "N/A";
            if (movie.getGenre() != null) {
                info += " • " + movie.getGenre().getName();
            }
            JLabel infoLabel = new JLabel(info);
            infoLabel.putClientProperty(FlatClientProperties.STYLE, "foreground:$Label.disabledForeground;font:-1;");

            infoPanel.add(titleLabel);
            infoPanel.add(infoLabel);

            add(icon);
            add(infoPanel, "growx");

            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    openMovie();
                }

                @Override
                public void mouseEntered(MouseEvent e) {
                    int index = movieItems.indexOf(MovieResultItem.this);
                    if (index != -1) {
                        MovieSearchPanel.this.setSelected(index);
                    }
                }
            });
        }

        public void setSelected(boolean selected) {
            this.selected = selected;
            if (selected) {
                putClientProperty(FlatClientProperties.STYLE, "" +
                        "arc:12;" +
                        "[light]background:tint($Component.accentColor,80%);" +
                        "[dark]background:tint($Component.accentColor,20%);");
            } else {
                putClientProperty(FlatClientProperties.STYLE, "" +
                        "arc:12;" +
                        "[light]background:tint($Panel.background,5%);" +
                        "[dark]background:tint($Panel.background,3%);");
            }
            repaint();
        }

        public boolean isSelected() {
            return selected;
        }

        public void openMovie() {
            ModalDialog.closeModal("movieSearch");
            FormMovieDetail detailForm = new FormMovieDetail(movie);
            FormManager.showForm(detailForm);
        }
    }
}
