/** Clasa pentru construirea URL Gravatar din email
 * si returnarea unui AvatarDTO cu fallback
 * @author Mirica Alin-Marian
 * @version 30 Decembrie 2025
 */

package com.proiect.spotifyclone.service;

import org.springframework.stereotype.Service;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Locale;

@Service
public class GravatarService {
    // Returneaza URL complet Gravatar pentru un email (cu fallback size/default)
    public String urlForEmail(String email, int size, String def) {
        String normalized = (email == null ? "" : email.trim().toLowerCase(Locale.ROOT));
        String hash = md5Hex(normalized);

        int s = Math.min(Math.max(size, 16), 512);
        String d = (def == null || def.isBlank()) ? "identicon" : def;

        return "https://www.gravatar.com/avatar/" + hash + "?s=" + s + "&d=" + d + "&r=g";
    }

    // Face MD5 pe string (folosit de Gravatar)
    private String md5Hex(String s) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(s.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException("MD5 failed", e);
        }
    }
}
