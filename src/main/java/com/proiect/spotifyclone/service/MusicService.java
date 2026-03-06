/** Clasa pentru integrarea cu Deezer;
 * cautare tracks, preluare detalii track
 * si playlist tracks, mapare JSON -> DTO
 * @author Mirica Alin-Marian
 * @version 24 Decembrie 2025
 */

package com.proiect.spotifyclone.service;

import com.proiect.spotifyclone.dto.TrackDTO;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;

@Service
public class MusicService {
    RestClient client = RestClient.create();

    // Cauta track-uri in Deezer dupa query (si le mapeaza in TrackDTO)
    public List<TrackDTO> searchTracks(String query) {
        List<TrackDTO> tracks = new ArrayList<>();
        String json =  client.get()
                .uri("https://api.deezer.com/search/track?q={q}&limit={l}", query, 10)
                .retrieve()
                .body(String.class);

        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(json);
            JsonNode data = root.get("data");

            if (data == null || !data.isArray()) return tracks;

            for (JsonNode item : data) {
                long id = item.get("id").asLong();
                String title = item.get("title").asText();
                String artist = item.get("artist").get("name").asText();
                String cover = item.get("album").get("cover").asText();
                String preview = item.get("preview").asText();

                tracks.add(new TrackDTO(id, title, artist, cover, preview));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return tracks;
    }

    // Ia un track Deezer dupa id (folosit la playByDeezerId)
    public TrackDTO getTrackById(long deezerId) throws Exception {
        String json = client.get()
                .uri("https://api.deezer.com/track/{id}", deezerId)
                .retrieve()
                .body(String.class);

        ObjectMapper mapper = new ObjectMapper();
        JsonNode item = mapper.readTree(json);

        long id = item.get("id").asLong();
        String title = item.get("title").asText();
        String artist = item.get("artist").get("name").asText();
        String cover = item.get("album").get("cover").asText();
        String preview = item.get("preview").asText();

        return new TrackDTO(id, title, artist, cover, preview);
    }

    // Ia track-urile unui playlist Deezer (Discover) cu limit
    public List<TrackDTO> getPlaylistTracks(long playlistId, int limit) {
        List<TrackDTO> tracks = new ArrayList<>();

        String json = client.get()
                .uri("https://api.deezer.com/playlist/{id}/tracks?limit={limit}", playlistId, limit)
                .retrieve()
                .body(String.class);

        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(json);
            JsonNode data = root.get("data");

            if (data == null || !data.isArray()) return tracks;

            for (JsonNode item : data) {
                long id = item.get("id").asLong();
                String title = item.get("title").asText();
                String artist = item.get("artist").get("name").asText();
                String cover = item.get("album").get("cover").asText();
                String preview = item.get("preview").asText();

                tracks.add(new TrackDTO(id, title, artist, cover, preview));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return tracks;
    }

    // Ia cover-ul unui playlist
    public String getPlaylistCover(long playlistId) {
        try {
            String json = client.get()
                    .uri("https://api.deezer.com/playlist/{id}", playlistId)
                    .retrieve()
                    .body(String.class);

            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(json);

            String cover = root.path("picture_medium").asText();
            if (cover == null || cover.isBlank())
                cover = root.path("picture").asText();

            return cover;
        } catch (Exception e) {
            return "";
        }
    }
}
