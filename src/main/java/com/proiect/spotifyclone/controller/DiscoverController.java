/** Clasa pentru endpoint-uri REST de 'discover';
 * preia playlisturi si piese recomandate (de pe Deezer) pentru pagina Home (Discover)
 * @author Mirica Alin-Marian
 * @version 29 Decembrie 2025
 */

package com.proiect.spotifyclone.controller;

import com.proiect.spotifyclone.dto.DiscoverPlaylistDTO;
import com.proiect.spotifyclone.dto.TrackDTO;
import com.proiect.spotifyclone.service.MusicService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/discover")
public class DiscoverController {

    private final MusicService musicService;

    public DiscoverController(MusicService musicService) {
        this.musicService = musicService;
    }

    @GetMapping
    public List<DiscoverPlaylistDTO> discover() {
        return List.of(
                new DiscoverPlaylistDTO(14512114763L, "Rock", musicService.getPlaylistCover(14512114763L)),
                new DiscoverPlaylistDTO(2098157264L, "Pop", musicService.getPlaylistCover(2098157264L)),
                new DiscoverPlaylistDTO(14478937083L, "Jazz",  musicService.getPlaylistCover(14478937083L)),
                new DiscoverPlaylistDTO(1677006641L, "Hip-Hop",  musicService.getPlaylistCover(1677006641L)),
                new DiscoverPlaylistDTO(1999466402L, "R&B",   musicService.getPlaylistCover(1999466402L)),
                new DiscoverPlaylistDTO(2143562442L, "Electronic",   musicService.getPlaylistCover(2143562442L)),
                new DiscoverPlaylistDTO(4096400722L, "K-Pop",   musicService.getPlaylistCover(4096400722L)),
                new DiscoverPlaylistDTO(1130102843L, "Country",   musicService.getPlaylistCover(1130102843L)),
                new DiscoverPlaylistDTO(1273315391L, "Reggaeton",   musicService.getPlaylistCover(1273315391L)),
                new DiscoverPlaylistDTO(3272614282L, "Rap",   musicService.getPlaylistCover(3272614282L)),
                new DiscoverPlaylistDTO(5714797982L, "Alternative",   musicService.getPlaylistCover(5714797982L)),
                new DiscoverPlaylistDTO(1767932902L, "Blues",   musicService.getPlaylistCover(1767932902L)),
                new DiscoverPlaylistDTO(14431630043L, "Romanian Trap",   musicService.getPlaylistCover(14431630043L)),
                new DiscoverPlaylistDTO(2265794682L, "House",   musicService.getPlaylistCover(2265794682L)),
                new DiscoverPlaylistDTO(14529953643L, "DNB",   musicService.getPlaylistCover(14529953643L)),
                new DiscoverPlaylistDTO(2655390504L, "Metal", musicService.getPlaylistCover(2655390504L))
        );
    }

    @GetMapping("/{playlistId}/tracks")
    public List<TrackDTO> tracks(@PathVariable long playlistId,
                                 @RequestParam(defaultValue = "1000")
                                 @Min(1) @Max(1000) int limit) {
        return musicService.getPlaylistTracks(playlistId, limit);
    }
}