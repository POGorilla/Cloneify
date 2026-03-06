/** Record pentru cererea de login (email + parola)
 * @author Mirica Alin-Marian
 * @version 27 Decembrie 2025
 */

package com.proiect.spotifyclone.dto;

public record AuthRequest(String email, String password) {}
