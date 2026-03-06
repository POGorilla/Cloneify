/** Interfata repository pentru operatii CRUD pe PlaylistTrack
 * si listarea pieselor ordonate dupa pozitia lor din playlist
 * @author Mirica Alin-Marian
 * @version 27 Decembrie 2025
 */

package com.proiect.spotifyclone.repository;

import com.proiect.spotifyclone.model.PlaylistTrack;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PlaylistTrackRepository extends JpaRepository<PlaylistTrack, Long> {
    List<PlaylistTrack> findByPlaylistIdOrderByPositionAsc(Long playlistId);
    Optional<PlaylistTrack> findByIdAndPlaylistId(Long trackId, Long playlistId);

    @Transactional
    void deleteByPlaylistId(Long playlistId);
}
