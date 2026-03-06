/** Record pentru cererea de confirmare a contului (email + cod de verificare)
 * @author Mirica Alin-Marian
 * @version 27 Decembrie 2025
 */

package com.proiect.spotifyclone.dto;

public record RegisterConfirmRequest(String email, String password, String code) {}
