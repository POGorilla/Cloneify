/** Interfata repository pentru operatii CRUD pe User si cautare dupa email
 * @author Mirica Alin-Marian
 * @version 2 Decembrie 2025
 */

package com.proiect.spotifyclone.repository;

import com.proiect.spotifyclone.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
}
