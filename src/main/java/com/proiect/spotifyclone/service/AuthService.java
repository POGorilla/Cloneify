/** Clasa pentru logica de autentificare si inregistrare;
 * validari, persistenta user si verificare email
 * @author Mirica Alin-Marian
 * @version 2 Decembrie 2025
 */


package com.proiect.spotifyclone.service;

import com.proiect.spotifyclone.model.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.proiect.spotifyclone.repository.UserRepository;

import java.util.Optional;

@Service
public class AuthService {
    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EmailVerificationService verificationService;

    public Optional<User> authenticateUser(String email, String password) {
        return userRepository.findByEmail(email)
                .filter(u -> u.getPassword().equals(password));
    }

    // inregistrare user cu validari / verificari
    public void registerUser(String email, String password) throws Exception {
        if (email == null || email.trim().isEmpty()) throw new Exception("Email cannot be empty!");
        if (!email.contains("@")) throw new Exception("Email must contain '@'!");
        if (email.indexOf("@") != email.lastIndexOf("@")) throw new Exception("Email cannot contain more than one '@'!");

        int atIndex = email.indexOf("@");
        String domainFull = email.substring(atIndex + 1);

        if (!domainFull.contains(".")) {
            throw new Exception("Email must have a valid extension (e.g., .com, .ro)!");
        }

        String localPart = email.substring(0, atIndex);
        int lastDotIndex = domainFull.lastIndexOf(".");
        String domainBody = domainFull.substring(0, lastDotIndex);
        String extension = domainFull.substring(lastDotIndex + 1);

        if (localPart.isEmpty()) throw new Exception("The part before @ is empty!");
        if (domainBody.isEmpty()) throw new Exception("The domain (between @ and the dot) is empty!");
        if (extension.isEmpty()) throw new Exception("The extension (after the dot) is empty!");

        valideazaBucataStandard(localPart, "the part before @");
        valideazaBucataStandard(domainBody, "the domain (before the extension)");
        valideazaExtensie(extension);
        valideazaParola(password, email);

        Optional<User> existingUser = userRepository.findByEmail(email);
        if (existingUser.isPresent()) {
            throw new Exception("This email is already in use!");
        }
        if (userRepository.findByEmail(email).isPresent()) {
            throw new Exception("This email is already in use!");
        }

        verificationService.sendVerificationCode(email);
    }

    // validare portiune de email
    private void valideazaBucataStandard(String text, String zona) throws Exception {
        if (text.startsWith(".") || text.startsWith("_"))
            throw new Exception("Error in " + zona + ": It cannot start with . or _");

        if (text.endsWith(".") || text.endsWith("_"))
            throw new Exception("Error in " + zona + ": It cannot end with . or _");

        for (char c : text.toCharArray()) {
            boolean isLitera = Character.isLetter(c);
            boolean isCifra = Character.isDigit(c);
            boolean isSpecialPermis = (c == '.' || c == '_');

            if (!isLitera && !isCifra && !isSpecialPermis) {
                throw new Exception("Error in " + zona + ": The character '" + c + "' is not allowed!");
            }
        }

        if (text.contains("..") || text.contains("__") || text.contains("._") || text.contains("_.") ) {
            throw new Exception("Error in " + zona + ": Consecutive special characters are not allowed!");
        }
    }

    // validare ce este dupa @ din email
    private void valideazaExtensie(String text) throws Exception {
        if (text.length() < 2) {
            throw new Exception("The email extension is too short (e.g., .com, .ro)!");
        }

        for (char c : text.toCharArray()) {
            boolean isLitera = Character.isLetter(c);
            boolean isCifra = Character.isDigit(c);

            if (!isLitera && !isCifra) {
                throw new Exception("The email extension ('" + text + "') cannot contain special characters!");
            }
        }
    }

    // validare parola
    private void valideazaParola(String password, String email) throws Exception {
        if (password == null || password.trim().isEmpty()) throw new Exception("Password cannot be empty!");
        if (password.length() < 8) throw new Exception("Password must be at least 8 characters long!");

        boolean areLiteraMare = false;
        boolean areCaracterSpecial = false;

        for (char c : password.toCharArray()) {
            if (Character.isUpperCase(c)) areLiteraMare = true;
            if (!Character.isLetterOrDigit(c)) areCaracterSpecial = true;
        }

        if (!areLiteraMare) throw new Exception("Password must contain an UPPERCASE letter!");
        if (!areCaracterSpecial) throw new Exception("Password must contain a special character!");

        String passLower = password.toLowerCase();
        String emailLower = email.toLowerCase();
        String userPart = emailLower.split("@")[0];

        if (userPart.length() > 2 && passLower.contains(userPart)) {
            throw new Exception("Password cannot contain the username part from the email!");
        }
    }

    // finalizeaza inregistrarea (prin verificarea codului de pe email)
    public void finalizeRegistration(String email, String password, String code) throws Exception {
        boolean isCorrect = verificationService.verifyCode(email, code);

        if (!isCorrect) {
            throw new Exception("Verification code is incorrect!");
        }

        User newUser = new User(email, password);
        userRepository.save(newUser);
    }
}
