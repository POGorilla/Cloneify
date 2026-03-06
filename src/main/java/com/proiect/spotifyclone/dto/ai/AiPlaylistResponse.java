/** Record pentru raspunsul AI;
 * numele playlistului si lista de track-uri rezolvate (TrackDTO)
 * @author Mirica Alin-Marian
 * @version 1 Ianuarie 2026
 */

package com.proiect.spotifyclone.dto.ai;

import com.proiect.spotifyclone.dto.TrackDTO;
import java.util.List;

public record AiPlaylistResponse(String name, List<TrackDTO> tracks) {}

