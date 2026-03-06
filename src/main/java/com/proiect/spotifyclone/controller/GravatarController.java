/** Clasa pentru endpoint-uri REST care returneaza avatarul utilizatorului (Gravatar)
 * in functie de email si parametrii de afisare
 * @author Mirica Alin-Marian
 * @version 29 Decembrie 2025
 */

package com.proiect.spotifyclone.controller;

import com.proiect.spotifyclone.dto.AvatarDTO;
import com.proiect.spotifyclone.model.User;
import com.proiect.spotifyclone.repository.UserRepository;
import com.proiect.spotifyclone.service.GravatarService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/users")
public class GravatarController {

    private final UserRepository userRepo;
    private final GravatarService gravatar;

    public GravatarController(UserRepository userRepo, GravatarService gravatar) {
        this.userRepo = userRepo;
        this.gravatar = gravatar;
    }

    @GetMapping("/me/avatar")
    public AvatarDTO myAvatar(@RequestHeader("X-User-Id") Long userId,
                              @RequestParam(defaultValue = "64") int size,
                              @RequestParam(defaultValue = "identicon") String def) {

        User u = userRepo.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        String url = gravatar.urlForEmail(u.getEmail(), size, def);
        return new AvatarDTO(url);
    }
}
