/** Entitate JPA pentru o piesa salvata intr-un playlist
 * @author Mirica Alin-Marian
 * @version 27 Decembrie 2025
 */

package com.proiect.spotifyclone.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

@Entity
public class PlaylistTrack {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JsonIgnore
    private Playlist playlist;

    private long deezerId;

    @Column(nullable = false, length = 500)
    private String title;

    @Column(nullable = false, length = 300)
    private String artist;

    @Column(length = 1024)
    private String coverUrl;

    @Column(nullable = false, length = 1024)
    private String previewUrl;

    private int position;

    public Long getId() {
        return id;
    }

    public Playlist getPlaylist() {
        return playlist;
    }

    public long getDeezerId() {
        return deezerId;
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

    public int getPosition() {
        return position;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setPlaylist(Playlist playlist) {
        this.playlist = playlist;
    }

    public void setDeezerId(long deezerId) {
        this.deezerId = deezerId;
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

    public void setPosition(int position) {
        this.position = position;
    }
}
