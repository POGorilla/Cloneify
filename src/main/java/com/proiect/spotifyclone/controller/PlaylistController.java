/** Clasa pentru endpoint-uri REST de playlist;
 * CRUD playlisturi, adaugare/stergere piese si listarea pieselor din playlist
 * @author Mirica Alin-Marian
 * @version 27 Decembrie 2025
 */

package com.proiect.spotifyclone.controller;

import com.proiect.spotifyclone.dto.AddTrackRequest;
import com.proiect.spotifyclone.dto.DiscoverPlaylistDTO;
import com.proiect.spotifyclone.dto.TrackDTO;
import com.proiect.spotifyclone.model.Playlist;
import com.proiect.spotifyclone.model.PlaylistTrack;
import com.proiect.spotifyclone.model.User;
import com.proiect.spotifyclone.repository.PlaylistRepository;
import com.proiect.spotifyclone.repository.PlaylistTrackRepository;
import com.proiect.spotifyclone.repository.UserRepository;
import com.proiect.spotifyclone.service.MusicService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/playlists")
@Validated
public class PlaylistController {
    private final PlaylistRepository playlists;
    private final PlaylistTrackRepository tracks;
    private final UserRepository userRepo;
    private final MusicService musicService;

    public PlaylistController(PlaylistRepository playlists, PlaylistTrackRepository tracks,  UserRepository userRepo,  MusicService musicService) {
        this.playlists = playlists;
        this.tracks = tracks;
        this.userRepo = userRepo;
        this.musicService = musicService;
    }

    @GetMapping
    public List<Playlist> all(@RequestHeader("X-User-Id") Long userId) {
        return playlists.findByOwnerId(userId);
    }

    @PostMapping
    public Playlist create(@RequestHeader("X-User-Id") Long userId,
                           @RequestParam
                           @NotBlank(message = "Playlist name is required")
                           @Size(min = 1, max = 40, message = "Playlist name must have between 1-40 characters.")
                           String name) {
        Playlist p = new Playlist();
        p.setName(name);

        User u = userRepo.findById(userId).orElseThrow();
        p.setOwner(u);

        return playlists.save(p);
    }

    @GetMapping("/{id}/tracks")
    public List<PlaylistTrack> listTracks(@RequestHeader("X-User-Id") Long userId,
                                          @PathVariable Long id) {
        playlists.findByIdAndOwnerId(id, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Playlist not found"));
        return tracks.findByPlaylistIdOrderByPositionAsc(id);
    }

    @PostMapping("/{id}/tracks")
    public PlaylistTrack addTrack(@RequestHeader("X-User-Id") Long userId,
                                  @PathVariable Long id,
                                  @Valid @RequestBody AddTrackRequest req) {
        Playlist playlist = playlists.findByIdAndOwnerId(id, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Playlist not found"));
        PlaylistTrack t = new PlaylistTrack();

        t.setPlaylist(playlist);
        t.setDeezerId(req.deezerId());
        t.setTitle(req.title());
        t.setArtist(req.artist());
        t.setCoverUrl(req.coverUrl());
        t.setPreviewUrl(req.previewUrl());

        int pos = tracks.findByPlaylistIdOrderByPositionAsc(id).size() + 1;
        t.setPosition(pos);

        return tracks.save(t);
    }

    @DeleteMapping("/{id}")
    public void delete(@RequestHeader("X-User-Id") Long userId,
                       @PathVariable Long id) {
        Playlist p = playlists.findByIdAndOwnerId(id, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Playlist not found"));

        tracks.deleteByPlaylistId(id);
        playlists.delete(p);
    }

    @DeleteMapping("/{id}/tracks/{trackId}")
    public void removeTrack(@RequestHeader("X-User-Id") Long userId,
                            @PathVariable Long id,
                            @PathVariable Long trackId) {
        playlists.findByIdAndOwnerId(id, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Playlist not found"));
        PlaylistTrack tr = tracks.findByIdAndPlaylistId(trackId, id).orElseThrow();
        tracks.delete(tr);
    }

    @PutMapping("/{id}")
    public Playlist rename(@RequestHeader("X-User-Id") Long userId,
                           @PathVariable Long id,
                           @RequestParam
                               @NotBlank(message = "Playlist name is required")
                               @Size(min = 1, max = 40, message = "Playlist name must be between 1-40 characters.")
                               String name) {
        Playlist p = playlists.findByIdAndOwnerId(id, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Playlist not found"));
        p.setName(name);
        return playlists.save(p);
    }

    @GetMapping("/preview/{deezerId}")
    public ResponseEntity<Void> previewRedirect(@PathVariable long deezerId) throws Exception {
        TrackDTO dto = musicService.getTrackById(deezerId);

        return ResponseEntity.status(302)
                .location(java.net.URI.create(dto.getPreviewUrl()))
                .build();
    }
}
