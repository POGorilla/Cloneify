/** Clasa pentru endpoint-uri REST de generare a playlisturilor cu AI (Gemini)
 * si returnarea rezultatelor catre frontend
 * @author Mirica Alin-Marian
 * @version 5 Ianuarie 2026
 */

package com.proiect.spotifyclone.controller;

import com.proiect.spotifyclone.dto.ai.AiPlaylistRequest;
import com.proiect.spotifyclone.dto.ai.AiPlaylistResponse;
import com.proiect.spotifyclone.service.AiService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/ai")
@Validated
public class AiController {

    private final AiService aiService;

    public AiController(AiService aiService) {
        this.aiService = aiService;
    }

    @PostMapping("/playlist")
    public AiPlaylistResponse generate(@RequestParam(defaultValue="15") @Min(1) @Max(30) int limit,
                                       @Valid @RequestBody AiPlaylistRequest body) {
        try {
            return aiService.generatePlaylist(body.prompt(), limit);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "AI returned invalid JSON. Try again.");
        }
    }
}
