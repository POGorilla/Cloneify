/** Record pentru schema intermediara produsa de AI (nume + lista de sugestii title/artist)
 * inainte de rezolvarea in Deezer
 * @author Mirica Alin-Marian
 * @version 1 Ianuarie 2026
 */

package com.proiect.spotifyclone.dto.ai;

import java.util.List;

public record AiPlaylistSuggestion(String name, List<AiSongSuggestion> tracks) {}
