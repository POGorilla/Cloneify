/** Clasa pentru endpoint-uri REST legate de muzica;
 * cautare track-uri, detalii track si acces la preview
 * (Deezer ofera doar preview de 30s, nu si acces la toata melodia)
 * @author Mirica Alin-Marian
 * @version 27 Decembrie 2025
 */

package com.proiect.spotifyclone.controller;

import com.proiect.spotifyclone.dto.TrackDTO;
import com.proiect.spotifyclone.service.MusicService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/music")
public class MusicController {
    private final MusicService musicService;

    public MusicController(MusicService musicService) {
        this.musicService = musicService;
    }

    @GetMapping("/search")
    public List<TrackDTO> search(@RequestParam String query) {
        return musicService.searchTracks(query);
    }

    @GetMapping("/track/{deezerId}")
    public TrackDTO trackById(@PathVariable long deezerId) throws Exception {
        return musicService.getTrackById(deezerId);
    }

    @GetMapping("/preview/{deezerId}")
    public ResponseEntity<Void> previewRedirect(@PathVariable long deezerId) throws Exception {
        TrackDTO dto = musicService.getTrackById(deezerId);

        if (dto.getPreviewUrl() == null || dto.getPreviewUrl().isBlank()) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.status(302)
                .location(URI.create(dto.getPreviewUrl()))
                .build();
    }
}
