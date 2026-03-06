/** Record pentru payloadul de adaugare a unei melodii intr-un playlist (datele trackului sunt salvate in DB)
 * @author Mirica Alin-Marian
 * @version 27 Decembrie 2025
 */

package com.proiect.spotifyclone.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record AddTrackRequest (
        @NotNull @Positive Long deezerId,
        @NotBlank @Size(min = 1, max = 120) String title,
        @NotBlank @Size(min = 1, max = 120) String artist,
        @Size(max = 500) String coverUrl,
        @NotBlank @Size(min = 1, max = 500) String previewUrl
) {}