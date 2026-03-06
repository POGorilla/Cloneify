/** Record pentru o piesa dintr-un playlist
 * (id intern, deezerId, titlu, artist, cover, preview, pozitie in playlist)
 * @author Mirica Alin-Marian
 * @version 27 Decembrie 2025
 */

package com.proiect.spotifyclone.dto;

public record PlaylistTrackDTO(Long id, long deezerId, String title,
                               String artist, String coverUrl, String previewUrl,
                               int position) {}
