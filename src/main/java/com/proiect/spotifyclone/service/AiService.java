/** Clasa pentru integrarea cu Gemini;
 * genereaza sugestii, valideaza/parseaza JSON
 * si rezolva piesele in Deezer folosind MusicService
 * @author Mirica Alin-Marian
 * @version 1 Ianuarie 2026
 */

package com.proiect.spotifyclone.service;

import org.springframework.beans.factory.annotation.Value;
import com.google.genai.Client;
import com.google.genai.types.GenerateContentConfig;
import com.google.genai.types.GenerateContentResponse;
import com.proiect.spotifyclone.dto.TrackDTO;
import com.proiect.spotifyclone.dto.ai.AiPlaylistResponse;
import com.proiect.spotifyclone.dto.ai.AiPlaylistSuggestion;
import com.proiect.spotifyclone.dto.ai.AiSongSuggestion;
import jakarta.annotation.PreDestroy;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;

@Service
public class AiService {
    private final ObjectMapper mapper = new ObjectMapper();

    // Client Gemini (Google GenAI)
    private final Client client;
    private final MusicService musicService;
    String model = "gemini-2.5-flash";

    public AiService(MusicService musicService, @Value("${gemini.api-key}") String apiKey) {
        this.client = Client.builder().apiKey(apiKey).build();
        this.musicService = musicService;
    }

    // Genereaza playlist AI (Gemini) si rezolva fiecare track prin search Deezer (searchTracks)
    public AiPlaylistResponse generatePlaylist(String userPrompt, int limit) throws Exception {
        int n = Math.max(1, Math.min(limit, 30));

        String prompt = """
            You are a music playlist generator.
            Return ONLY valid JSON (no markdown, no code fences).
            Schema:
            {"name":"string","tracks":[{"title":"string","artist":"string"}]}
            Rules:
            - tracks length must be exactly %d
            - avoid duplicates
            - do not include quotes inside values
            User request: %s
            """.formatted(n, userPrompt);

        GenerateContentConfig cfg = GenerateContentConfig.builder()
                .temperature(0.2F)
                .candidateCount(1)
                .responseMimeType("application/json")
                .maxOutputTokens(1500)
                .build();

        String raw = client.models.generateContent(model, prompt, cfg).text();
        AiPlaylistSuggestion suggestion = parseOrRepair(raw, n);

        List<TrackDTO> resolved = new ArrayList<>();
        for (AiSongSuggestion s : suggestion.tracks()) {
            if (s == null) continue;
            String q = ((s.title() == null) ? "" : s.title()) + " " + ((s.artist() == null) ? "" : s.artist());
            q = q.trim();
            if (q.isBlank()) continue;

            List<TrackDTO> matches = musicService.searchTracks(q);
            if (!matches.isEmpty()) resolved.add(matches.get(0));
        }

        String name = (suggestion.name() == null || suggestion.name().isBlank()) ? "AI Playlist" : suggestion.name();
        return new AiPlaylistResponse(name, resolved);
    }

    // Incearca sa parseze JSON; daca e stricat, aplica “repair” ca sa nu crape app
    private AiPlaylistSuggestion parseOrRepair(String raw, int n) throws Exception {
        String json = extractFirstJsonObject(raw);

        try {
            return mapper.readValue(json, AiPlaylistSuggestion.class);
        } catch (Exception firstEx) {
            String repairPrompt = """
                You are a JSON repair tool.
                Fix the JSON so it is VALID and matches exactly this schema:
                {"name":"string","tracks":[{"title":"string","artist":"string"}]}
                Rules:
                - tracks length must be exactly %d
                - Return ONLY the repaired JSON, nothing else.
                Broken input:
                %s
                """.formatted(n, json);

            GenerateContentConfig repairCfg = GenerateContentConfig.builder()
                    .temperature(0.0F)
                    .candidateCount(1)
                    .responseMimeType("application/json")
                    .maxOutputTokens(1200)
                    .build();

            String repairedRaw = client.models.generateContent(model, repairPrompt, repairCfg).text();
            String repairedJson = extractFirstJsonObject(repairedRaw);

            return mapper.readValue(repairedJson, AiPlaylistSuggestion.class);
        }
    }

    // Extrage primul obiect JSON valid dintr-un text (daca ai-ul intoarce text extra)
    private static String extractFirstJsonObject(String s) {
        if (s == null) return "{}";
        String t = s.trim();

        if (t.startsWith("```")) {
            int firstNl = t.indexOf('\n');
            int lastFence = t.lastIndexOf("```");
            if (firstNl > 0 && lastFence > firstNl) {
                t = t.substring(firstNl + 1, lastFence).trim();
            }
        }

        int start = t.indexOf('{');
        if (start < 0) return "{}";

        int depth = 0;
        boolean inString = false;
        boolean esc = false;

        for (int i = start; i < t.length(); i++) {
            char c = t.charAt(i);

            if (inString) {
                if (esc) { esc = false; continue; }
                if (c == '\\') { esc = true; continue; }
                if (c == '"') inString = false;
                continue;
            } else {
                if (c == '"') { inString = true; continue; }
                if (c == '{') depth++;
                if (c == '}') {
                    depth--;
                    if (depth == 0) {
                        return t.substring(start, i + 1).trim();
                    }
                }
            }
        }

        return t.substring(start).trim();
    }

    @PreDestroy
    public void close() {
        try { client.close(); } catch (Exception ignored) {}
    }
}
