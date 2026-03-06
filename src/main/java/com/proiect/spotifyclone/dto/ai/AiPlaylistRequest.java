/** Record pentru cererea de generare playlist AI (prompt)
 * @author Mirica Alin-Marian
 * @version 1 Ianuarie 2026
 */

package com.proiect.spotifyclone.dto.ai;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AiPlaylistRequest(
        @NotBlank(message = "Prompt is required.")
        @Size(min = 5, max = 200, message = "Prompt must be between 5 and 200 characters.")
        String prompt) {}