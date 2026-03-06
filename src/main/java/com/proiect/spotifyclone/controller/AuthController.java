/** Clasa pentru endpoint-uri REST de autentificare, inregistrare si confirmare email
 * @author Mirica Alin-Marian
 * @version 2 Decembrie 2025
 */

package com.proiect.spotifyclone.controller;

import com.proiect.spotifyclone.dto.AuthRequest;
import com.proiect.spotifyclone.dto.LoginResponse;
import com.proiect.spotifyclone.dto.RegisterConfirmRequest;
import com.proiect.spotifyclone.service.AuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody AuthRequest req) {
        return authService.authenticateUser(req.email(), req.password())
                .map(u -> ResponseEntity.ok(new LoginResponse(u.getId(), u.getEmail())))
                .orElseGet(() -> ResponseEntity.status(401).build());
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody AuthRequest req) {
        try {
            authService.registerUser(req.email(), req.password());
            return ResponseEntity.ok("CODE_SENT");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/register/confirm")
    public ResponseEntity<?> confirm(@RequestBody RegisterConfirmRequest req) {
        try {
            authService.finalizeRegistration(req.email(), req.password(), req.code());
            return ResponseEntity.ok("REGISTERED");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}