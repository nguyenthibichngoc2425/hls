package raven.modal.demo.model;

public class Movie {
    private Integer id;
    private String title;
    private String description;
    private String imageUrl;
    private String videoUrl;
    private Integer duration; // phút (thời lượng phim thực tế)
    private Integer processingMinutes; // số phút xử lý video cho backend (0 = không xử lý)
    private Integer releaseYear;
    private String genre;
    private String status; // published, draft, processing

    public Movie() {
    }

    public Movie(Integer id, String title, String description, String imageUrl, String videoUrl,
                 Integer duration, Integer processingMinutes, Integer releaseYear, String genre, String status) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.imageUrl = imageUrl;
        this.videoUrl = videoUrl;
        this.duration = duration;
        this.processingMinutes = processingMinutes;
        this.releaseYear = releaseYear;
        this.genre = genre;
        this.status = status;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getVideoUrl() {
        return videoUrl;
    }

    public void setVideoUrl(String videoUrl) {
        this.videoUrl = videoUrl;
    }

    public Integer getDuration() {
        return duration;
    }

    public void setDuration(Integer duration) {
        this.duration = duration;
    }

    public Integer getProcessingMinutes() {
        return processingMinutes;
    }

    public void setProcessingMinutes(Integer processingMinutes) {
        this.processingMinutes = processingMinutes;
    }

    public Integer getReleaseYear() {
        return releaseYear;
    }

    public void setReleaseYear(Integer releaseYear) {
        this.releaseYear = releaseYear;
    }

    public String getGenre() {
        return genre;
    }

    public void setGenre(String genre) {
        this.genre = genre;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getDurationDisplay() {
        if (duration == null) return "N/A";
        int hours = duration / 60;
        int mins = duration % 60;
        if (hours > 0) {
            return hours + "h " + mins + "m";
        }
        return mins + "m";
    }
}
