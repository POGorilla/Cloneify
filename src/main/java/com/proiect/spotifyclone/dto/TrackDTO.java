/** Clasa pentru reprezentarea unui track (din Deezer API)
 * (id, titlu, artist, coverUrl si previewUrl)
 * @author Mirica Alin-Marian
 * @version 24 Decembrie 2025
 */

package com.proiect.spotifyclone.dto;

public class TrackDTO {
    private long id;
    private String title;
    private String artist;
    private String coverUrl;
    private String previewUrl;

    public TrackDTO(long id, String title, String artist, String coverUrl, String previewUrl) {
        this.id = id;
        this.title = title;
        this.artist = artist;
        this.coverUrl = coverUrl;
        this.previewUrl = previewUrl;
    }

    public long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getArtist() {
        return artist;
    }

    public String getCoverUrl() {
        return coverUrl;
    }

    public String getPreviewUrl() {
        return previewUrl;
    }

    public void setId(long id) {
        this.id = id;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setArtist(String artist) {
        this.artist = artist;
    }

    public void setCoverUrl(String coverUrl) {
        this.coverUrl = coverUrl;
    }

    public void setPreviewUrl(String previewUrl) {
        this.previewUrl = previewUrl;
    }
}