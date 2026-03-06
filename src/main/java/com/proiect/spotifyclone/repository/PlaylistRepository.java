/** Interfata repository pentru operatii CRUD pe Playlist si cautari filtrate dupa user
 * @author Mirica Alin-Marian
 * @version 27 Decembrie 2025
 */

package com.proiect.spotifyclone.repository;

import com.proiect.spotifyclone.model.Playlist;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PlaylistRepository extends JpaRepository<Playlist, Long> {
    List<Playlist> findByOwnerId(Long ownerId);
    Optional<Playlist> findByIdAndOwnerId(Long id, Long ownerId);
}