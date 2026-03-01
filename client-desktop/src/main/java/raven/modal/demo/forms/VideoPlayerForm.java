package raven.modal.demo.forms;

import raven.modal.demo.dto.response.MovieResponse;
import raven.modal.demo.dto.response.VideoQualityResponse;
import raven.modal.demo.menu.MyDrawerBuilder;
import raven.modal.demo.system.Form;
import javafx.application.Platform;
import javafx.concurrent.Worker;
import javafx.embed.swing.JFXPanel;
import javafx.scene.Scene;
import javafx.scene.paint.Color;
import javafx.scene.web.PromptData;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

public class VideoPlayerForm extends Form {

    private final JFXPanel jfxPanel = new JFXPanel();

    private String movieTitle = "";
    private List<QualityOption> qualities = new ArrayList<>();
    
    public static class QualityOption {
        public String label;
        public String url;
        
        public QualityOption(String label, String url) {
            this.label = label;
            this.url = url;
        }
    }

    private WebView webView;
    private WebEngine engine;

    private boolean isFullscreen = false;
    private JWindow fullScreenWindow;
    private JFXPanel fullScreenJFXPanel;
    private WebView fullScreenWebView;
    private WebEngine fullScreenEngine;

    public VideoPlayerForm() {
        init();
    }

    private void init() {
        setLayout(new BorderLayout());
        setBackground(java.awt.Color.BLACK);
        add(jfxPanel, BorderLayout.CENTER);
        Platform.setImplicitExit(false);
        Platform.runLater(this::initFX);
    }

    private void initFX() {
        webView = new WebView();
        webView.setContextMenuEnabled(false);
        Scene scene = new Scene(webView, Color.BLACK);
        jfxPanel.setScene(scene);
        engine = webView.getEngine();
        setupEngine(engine, false);
    }

    @Override
    public void formOpen() {
        super.formOpen();
        Platform.runLater(() -> {
            if (engine != null) {
                loadCustomPlayer(engine);
            }
        });
    }

    public void setMovie(MovieResponse movie) {
        this.movieTitle = movie.getTitle();
        this.qualities = new ArrayList<>();
        
        // Get user email for tracking
        String userEmail = "Khách";
        try {
            var user = MyDrawerBuilder.getInstance().getUser();
            if (user != null && user.getMail() != null && !user.getMail().isEmpty()) {
                userEmail = URLEncoder.encode(user.getMail(), "UTF-8");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        if (movie.getVideoQualities() != null && !movie.getVideoQualities().isEmpty()) {
            for (VideoQualityResponse vq : movie.getVideoQualities()) {
                String quality = vq.getQuality();
                String url = "http://localhost:8080/api/hls/" + movie.getId() + "/" + quality + "/playlist.m3u8?userEmail=" + userEmail;
                qualities.add(new QualityOption(quality, url));
            }
        }
        
        if (qualities.isEmpty() && movie.getMasterPlaylistPath() != null) {
            String masterUrl = "http://localhost:8080/api/hls/" + movie.getId() + "/master.m3u8?userEmail=" + userEmail;
            qualities.add(new QualityOption("Auto", masterUrl));
        }
    }

    @Override
    public void removeNotify() {
        if (isFullscreen) exitFullscreen();
        stopVideo();
        super.removeNotify();
    }

    private void stopVideo() {
        Platform.runLater(() -> {
            if (engine != null) {
                engine.load(null);
            }
        });
    }

    private void setupEngine(WebEngine eng, boolean isFullScreenEngine) {
        eng.setPromptHandler((PromptData param) -> {
            String command = param.getMessage();
            if ("toggleFullscreen".equals(command)) {
                SwingUtilities.invokeLater(() -> {
                    if (isFullscreen) exitFullscreen();
                    else enterFullscreen();
                });
                return "OK";
            }
            return null;
        });
    }

    private void enterFullscreen() {
        Window windowAncestor = SwingUtilities.getWindowAncestor(this);
        Frame parentFrame = (windowAncestor instanceof Frame) ? (Frame) windowAncestor : null;
        isFullscreen = true;

        Platform.runLater(() -> {
            double currentTime = 0;
            boolean isPaused = true;
            String currentQualityUrl = "";

            try {
                Object timeObj = engine.executeScript("video.currentTime");
                Object pausedObj = engine.executeScript("video.paused");
                Object srcObj = engine.executeScript("currentSrc");

                if (timeObj instanceof Number) currentTime = ((Number) timeObj).doubleValue();
                if (pausedObj instanceof Boolean) isPaused = (Boolean) pausedObj;
                if (srcObj instanceof String) currentQualityUrl = (String) srcObj;

                engine.executeScript("video.pause()");
            } catch (Exception e) { e.printStackTrace(); }

            final double startAt = currentTime;
            final boolean shouldPlay = !isPaused;
            final String startUrl = currentQualityUrl;

            SwingUtilities.invokeLater(() -> {
                fullScreenJFXPanel = new JFXPanel();
                fullScreenWindow = new JWindow(parentFrame);
                fullScreenWindow.setBackground(java.awt.Color.BLACK);
                fullScreenWindow.setLayout(new BorderLayout());
                fullScreenWindow.add(fullScreenJFXPanel, BorderLayout.CENTER);
                fullScreenWindow.setAlwaysOnTop(true);
                Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
                fullScreenWindow.setSize(screenSize);
                fullScreenWindow.setLocation(0, 0);

                fullScreenWindow.addWindowListener(new WindowAdapter() {
                    @Override
                    public void windowClosed(WindowEvent e) {
                        if(isFullscreen) exitFullscreen();
                    }
                });

                Platform.runLater(() -> {
                    fullScreenWebView = new WebView();
                    fullScreenWebView.setContextMenuEnabled(false);
                    fullScreenJFXPanel.setScene(new Scene(fullScreenWebView, Color.BLACK));
                    fullScreenEngine = fullScreenWebView.getEngine();
                    setupEngine(fullScreenEngine, true);
                    loadCustomPlayer(fullScreenEngine);

                    fullScreenEngine.getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
                        if (newState == Worker.State.SUCCEEDED) {
                            new Thread(() -> {
                                try {
                                    Thread.sleep(500);
                                    Platform.runLater(() -> {
                                        if (fullScreenEngine == null) return;
                                        if (!startUrl.isEmpty()) {
                                            fullScreenEngine.executeScript("changeQualityByUrl('" + startUrl + "')");
                                        }
                                        fullScreenEngine.executeScript("video.currentTime = " + startAt);
                                        if (shouldPlay) {
                                            fullScreenEngine.executeScript("video.addEventListener('seeked', function() { video.play(); }, {once:true});");
                                        }
                                    });
                                } catch (InterruptedException ignored) {}
                            }).start();
                        }
                    });
                });
                fullScreenWindow.setVisible(true);
                fullScreenWindow.toFront();
            });
        });
    }

    private void exitFullscreen() {
        if (fullScreenWindow == null || fullScreenEngine == null) {
            isFullscreen = false;
            return;
        }

        Platform.runLater(() -> {
            double currentTime = 0;
            boolean isPaused = true;
            String currentQualityUrl = "";

            try {
                Object timeObj = fullScreenEngine.executeScript("video.currentTime");
                Object pausedObj = fullScreenEngine.executeScript("video.paused");
                Object srcObj = fullScreenEngine.executeScript("currentSrc");

                if (timeObj instanceof Number) currentTime = ((Number) timeObj).doubleValue();
                if (pausedObj instanceof Boolean) isPaused = (Boolean) pausedObj;
                if (srcObj instanceof String) currentQualityUrl = (String) srcObj;
            } catch (Exception e) { e.printStackTrace(); }

            final double syncTime = currentTime;
            final boolean shouldPlay = !isPaused;
            final String syncUrl = currentQualityUrl;

            SwingUtilities.invokeLater(() -> {
                if (fullScreenWindow != null) {
                    fullScreenWindow.dispose();
                    fullScreenWindow = null;
                }
                isFullscreen = false;
            });

            Platform.runLater(() -> {
                if (fullScreenEngine != null) {
                    fullScreenEngine.load(null);
                    fullScreenEngine = null;
                }
                fullScreenWebView = null;
                fullScreenJFXPanel = null;

                if (engine != null) {
                    try {
                        if (!syncUrl.isEmpty()) {
                            engine.executeScript("changeQualityByUrl('" + syncUrl + "')");
                        }
                        engine.executeScript("video.currentTime = " + syncTime);
                        if (shouldPlay) engine.executeScript("video.play()");
                    } catch (Exception ignored){}
                }
            });
        });
    }

    private String buildQualitiesJson() {
        if (qualities.isEmpty()) return "[]";
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < qualities.size(); i++) {
            QualityOption q = qualities.get(i);
            sb.append(String.format("{\"quality\":\"%s\", \"url\":\"%s\"}", q.label, q.url));
            if (i < qualities.size() - 1) sb.append(",");
        }
        sb.append("]");
        return sb.toString();
    }

    private void loadCustomPlayer(WebEngine engine) {
        String qualitiesJson = buildQualitiesJson();

        String htmlContent = """
            <html>
            <head>
                <meta charset="UTF-8">
                <script src='https://cdn.jsdelivr.net/npm/hls.js@latest'></script>
                <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.4.0/css/all.min.css">
                <style>
                    body { margin: 0; background: #000; overflow: hidden; font-family: 'Segoe UI', sans-serif; user-select: none; }
                    #player-container { position: relative; width: 100%; height: 100vh; display: flex; justify-content: center; align-items: center; background: #000; }
                    video { width: 100%; height: 100%; object-fit: contain; }
                    
                    .title-overlay { position: absolute; top: 25px; left: 30px; color: white; font-size: 24px; font-weight: 600; text-shadow: 0 2px 4px rgba(0,0,0,0.8); opacity: 0.9; pointer-events: none; z-index: 10; transition: opacity 0.5s; }
                    .hide-controls .title-overlay { opacity: 0; }
                    
                    .controls-overlay { position: absolute; bottom: 0; left: 0; right: 0; background: linear-gradient(to top, rgba(0,0,0,0.9) 0%, rgba(0,0,0,0.5) 50%, transparent 100%); padding: 20px 30px 25px 30px; display: flex; flex-direction: column; gap: 10px; transition: opacity 0.3s ease-in-out; opacity: 1; z-index: 20; }
                    .hide-controls .controls-overlay { opacity: 0; pointer-events: none; }
                    
                    .progress-row { display: flex; align-items: center; gap: 15px; width: 100%; margin-bottom: 5px; }
                    .time-text { color: #e0e0e0; font-size: 13px; font-weight: 500; min-width: 60px; text-align: center; font-variant-numeric: tabular-nums; }
                    
                    input[type=range] { -webkit-appearance: none; width: 100%; background: transparent; cursor: pointer; height: 4px; border-radius: 2px; }
                    input[type=range]:focus { outline: none; }
                    input[type=range]::-webkit-slider-runnable-track { width: 100%; height: 4px; cursor: pointer; border-radius: 2px; border: none; }
                    input[type=range]::-webkit-slider-thumb { -webkit-appearance: none; height: 14px; width: 14px; border-radius: 50%; background: #fff; cursor: pointer; margin-top: -5px; box-shadow: 0 0 5px rgba(0,0,0,0.5); transform: scale(0); transition: transform 0.1s; }
                    .controls-overlay:hover input[type=range]::-webkit-slider-thumb { transform: scale(1); }
                    input[type=range]::-webkit-slider-thumb:hover { transform: scale(1.3); }
                    
                    .buttons-row { display: flex; align-items: center; justify-content: space-between; }
                    .left-controls, .right-controls { display: flex; align-items: center; gap: 20px; }
                    .btn { background: none; border: none; color: white; cursor: pointer; font-size: 18px; opacity: 0.85; transition: all 0.2s; padding: 5px; min-width: 30px; }
                    .btn:hover { opacity: 1; transform: scale(1.1); text-shadow: 0 0 10px rgba(255,255,255,0.5); }
                    .btn-play { font-size: 24px; width: 35px; }
                    
                    .volume-group { display: flex; align-items: center; gap: 10px; width: 130px; }
                    #volume-slider { height: 4px; }
                    
                    .settings-container { position: relative; }
                    .quality-menu { position: absolute; bottom: 40px; right: 0; background: rgba(20, 20, 20, 0.95); border-radius: 8px; padding: 5px 0; min-width: 120px; display: none; flex-direction: column; box-shadow: 0 4px 15px rgba(0,0,0,0.5); backdrop-filter: blur(5px); }
                    .quality-menu.show { display: flex; }
                    .quality-item { padding: 8px 15px; color: #ccc; font-size: 14px; cursor: pointer; transition: background 0.2s; text-align: left; }
                    .quality-item:hover { background: rgba(255,255,255,0.1); color: #fff; }
                    .quality-item.active { color: #3498db; font-weight: bold; }
                    .quality-item.active::before { content: '✓ '; }
                </style>
            </head>
            <body>
                <div id="player-container">
                    <div class="title-overlay">__MOVIE_TITLE__</div>
                    <video id="video" onclick="togglePlay()" autoplay></video>
                    <div class="controls-overlay" id="controls">
                        <div class="progress-row">
                            <span id="current-time" class="time-text">00:00:00</span>
                            <input type="range" id="seek-bar" class="slider-progress" value="0" min="0" step="0.1">
                            <span id="duration" class="time-text">00:00:00</span>
                        </div>
                        <div class="buttons-row">
                            <div class="left-controls">
                                <button class="btn btn-play" onclick="togglePlay()"><i id="play-icon" class="fas fa-play"></i></button>
                                <button class="btn" onclick="skip(-10)"><i class="fas fa-rotate-left"></i></button>
                                <button class="btn" onclick="skip(10)"><i class="fas fa-rotate-right"></i></button>
                                <div class="volume-group">
                                    <button class="btn" onclick="toggleMute()"><i id="vol-icon" class="fas fa-volume-high"></i></button>
                                    <input type="range" id="volume-slider" class="slider-vol" min="0" max="1" step="0.05" value="1">
                                </div>
                            </div>
                            <div class="right-controls">
                                <div class="settings-container">
                                    <button class="btn" onclick="toggleQualityMenu()"><i class="fas fa-cog"></i></button>
                                    <div id="quality-menu" class="quality-menu"></div>
                                </div>
                                <button class="btn" onclick="callJavaFullscreen()"><i class="fas fa-expand"></i></button>
                            </div>
                        </div>
                    </div>
                </div>

                <script>
                    var video = document.getElementById('video');
                    var qualities = __QUALITIES_JSON__;
                    var currentSrc = "";
                    var hls = null;
                    
                    var playIcon = document.getElementById('play-icon');
                    var seekBar = document.getElementById('seek-bar');
                    var volSlider = document.getElementById('volume-slider');
                    var container = document.getElementById('player-container');
                    var currentTimeLabel = document.getElementById('current-time');
                    var durationLabel = document.getElementById('duration');
                    var qualityMenu = document.getElementById('quality-menu');
                    var hideTimer;

                    if(qualities.length > 0) {
                        loadSource(qualities[0].url);
                        renderQualities();
                    }

                    function loadSource(url) {
                        currentSrc = url;
                        var currentTime = video.currentTime;
                        var isPlaying = !video.paused;

                        if (Hls.isSupported()) {
                            if(hls) { hls.destroy(); }
                            hls = new Hls();
                            hls.loadSource(url);
                            hls.attachMedia(video);
                            hls.on(Hls.Events.MANIFEST_PARSED, function() {
                               video.currentTime = currentTime;
                               if(isPlaying) video.play();
                            });
                        } else if (video.canPlayType('application/vnd.apple.mpegurl')) {
                            video.src = url;
                            video.currentTime = currentTime;
                            video.addEventListener('loadedmetadata', function() {
                               if(isPlaying) video.play();
                            }, {once:true});
                        }
                        updateActiveQualityUI(url);
                    }

                    function renderQualities() {
                        qualityMenu.innerHTML = '';
                        qualities.forEach(q => {
                            var div = document.createElement('div');
                            div.className = 'quality-item ' + (q.url === currentSrc ? 'active' : '');
                            div.innerText = q.quality;
                            div.onclick = function() { 
                                loadSource(q.url); 
                                toggleQualityMenu();
                            };
                            qualityMenu.appendChild(div);
                        });
                    }

                    function updateActiveQualityUI(url) {
                        var items = document.getElementsByClassName('quality-item');
                        for(var i=0; i<items.length; i++) {
                            items[i].className = 'quality-item';
                            if(qualities[i].url === url) items[i].className += ' active';
                        }
                    }
                    
                    function changeQualityByUrl(url) {
                        loadSource(url);
                    }

                    function toggleQualityMenu() {
                        if (qualityMenu.classList.contains('show')) {
                            qualityMenu.classList.remove('show');
                        } else {
                            qualityMenu.classList.add('show');
                        }
                    }

                    function togglePlay() {
                        if (video.paused) video.play();
                        else video.pause();
                    }

                    video.addEventListener('play', function() {
                        playIcon.className = 'fas fa-pause';
                        container.classList.remove('hide-controls');
                        scheduleHideControls();
                    });
                    
                    video.addEventListener('pause', function() {
                        playIcon.className = 'fas fa-play';
                        container.classList.remove('hide-controls');
                    });

                    video.addEventListener('timeupdate', function() {
                        if (!isNaN(video.duration)) {
                            seekBar.value = video.currentTime;
                            currentTimeLabel.innerText = formatTime(video.currentTime);
                            updateSliderBackground(seekBar, video.currentTime, video.duration);
                        }
                    });

                    video.addEventListener('loadedmetadata', function() {
                        seekBar.max = video.duration;
                        durationLabel.innerText = formatTime(video.duration);
                        updateSliderBackground(seekBar, 0, video.duration);
                        updateSliderBackground(volSlider, video.volume, 1);
                    });

                    seekBar.addEventListener('input', function() {
                        var val = this.value;
                        video.currentTime = val;
                        updateSliderBackground(this, val, this.max);
                    });
                    
                    volSlider.addEventListener('input', function() {
                        video.volume = this.value;
                        updateSliderBackground(this, this.value, this.max);
                        updateVolumeIcon();
                    });

                    function toggleMute() {
                        video.muted = !video.muted;
                        updateVolumeIcon();
                    }

                    function updateVolumeIcon() {
                        var icon = document.getElementById('vol-icon');
                        if (video.muted || video.volume === 0) icon.className = 'fas fa-volume-xmark';
                        else if (video.volume < 0.5) icon.className = 'fas fa-volume-low';
                        else icon.className = 'fas fa-volume-high';
                    }

                    function skip(val) { video.currentTime += val; }

                    function callJavaFullscreen() { prompt('toggleFullscreen'); }

                    function formatTime(seconds) {
                        if(isNaN(seconds)) return "00:00:00";
                        var date = new Date(0);
                        date.setSeconds(seconds);
                        return date.toISOString().substr(11, 8);
                    }

                    function updateSliderBackground(el, val, max) {
                        var percentage = (val / max) * 100;
                        el.style.background = `linear-gradient(to right, #fff ${percentage}%, rgba(255,255,255,0.3) ${percentage}%)`;
                    }

                    function scheduleHideControls() {
                        clearTimeout(hideTimer);
                        hideTimer = setTimeout(() => {
                            if(!video.paused) container.classList.add('hide-controls');
                            qualityMenu.classList.remove('show');
                        }, 3000);
                    }

                    container.addEventListener('mousemove', function() {
                        container.classList.remove('hide-controls');
                        scheduleHideControls();
                    });
                    
                    updateSliderBackground(volSlider, 1, 1);
                </script>
            </body>
            </html>
            """;

        String finalHtml = htmlContent
                .replace("__QUALITIES_JSON__", qualitiesJson)
                .replace("__MOVIE_TITLE__", movieTitle);

        engine.loadContent(finalHtml);
    }
}
