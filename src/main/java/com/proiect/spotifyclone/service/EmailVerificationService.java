/** Clasa pentru generarea si verificarea codurilor de confirmare pe email
 * si expedierea lor catre utilizator (prin SMTP)
 * @author Mirica Alin-Marian
 * @version 2 Decembrie 2025
 */

package com.proiect.spotifyclone.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

@Service
public class EmailVerificationService {
    @Autowired
    private EmailService emailService;

    // codurile pentru inregistrare
    private Map<String, String> codeStorage = new HashMap<>();
    private Map<String, LocalDateTime> rateLimier = new HashMap<>();

    // rate limiter pentru a nu supraincarca smtp-ul
    private final static int RATE_SECONDS = 60;

    // generare cod verificare + trimitere cod
    public void sendVerificationCode(String email) throws Exception {
        if (rateLimier.containsKey(email)) {
            LocalDateTime last = rateLimier.get(email);
            long secondsPassed = ChronoUnit.SECONDS.between(last, LocalDateTime.now());

            if (secondsPassed < RATE_SECONDS) {
                long secondsLeft = RATE_SECONDS - secondsPassed;
                throw new Exception("Prea multe cereri! Mai așteaptă " + secondsLeft + " secunde.");
            }
        }

        String code = String.format("%06d", new Random().nextInt(999999));
        codeStorage.put(email, code);
        rateLimier.put(email, LocalDateTime.now());
        emailService.sendEmail(email, "Codul tau Cloneify", "Codul de verificare este: " + code);
    }

    // verificare cod stocat cu cod introdus
    public boolean verifyCode(String email, String userEnteredCode) {
        String realCode = codeStorage.get(email);

        if (realCode != null && realCode.equals(userEnteredCode)) {
            codeStorage.remove(email);
            return true;
        }
        return false;
    }
}