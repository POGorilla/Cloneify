/** Clasa utilitara pentru stocarea temporara a datelor de sesiune
 * @author Mirica Alin-Marian
 * @version 27 Decembrie 2025
 */

package com.proiect.spotifyclone.util;

public final class AppSession {
    private AppSession() {}

    public static Long userId = null;
    public static String email = null;

    public static boolean isLoggedIn() {
        return userId != null;
    }

    public static void clear() {
        userId = null;
        email = null;
    }
}