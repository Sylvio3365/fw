package fw.session;

import java.util.UUID;

public class SessionUtils {
    private static final String SESSION_COOKIE_NAME = "JSESSIONID";

    public static String generateSessionId() {
        return UUID.randomUUID().toString();
    }

    public static String getSessionCookieName() {
        return SESSION_COOKIE_NAME;
    }

    public static String createSessionIdFromRequest(jakarta.servlet.http.HttpServletRequest request) {
        String sessionId = null;

        // Chercher dans les cookies
        jakarta.servlet.http.Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (jakarta.servlet.http.Cookie cookie : cookies) {
                if (SESSION_COOKIE_NAME.equals(cookie.getName())) {
                    sessionId = cookie.getValue();
                    break;
                }
            }
        }

        // Si pas de session, en créer une nouvelle
        if (sessionId == null) {
            sessionId = generateSessionId();
        }

        return sessionId;
    }

    public static void setSessionCookie(jakarta.servlet.http.HttpServletResponse response, String sessionId, boolean isNew) {
        // Ne définir le cookie que pour les nouvelles sessions
        if (isNew) {
            jakarta.servlet.http.Cookie cookie = new jakarta.servlet.http.Cookie(SESSION_COOKIE_NAME, sessionId);
            cookie.setHttpOnly(true);
            cookie.setPath("/");
            cookie.setMaxAge(30 * 60); // 30 minutes
            response.addCookie(cookie);
        }
    }
}
