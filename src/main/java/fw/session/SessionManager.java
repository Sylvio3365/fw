package fw.session;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class SessionManager {
    private static SessionManager instance;
    private final Map<String, Session> sessions;
    private final Set<String> newSessions; // Tracker les nouvelles sessions
    private final long SESSION_TIMEOUT = 30 * 60 * 1000; // 30 minutes

    private SessionManager() {
        this.sessions = new ConcurrentHashMap<>();
        this.newSessions = Collections.newSetFromMap(new ConcurrentHashMap<>());
        startCleanupThread();
    }

    public static synchronized SessionManager getInstance() {
        if (instance == null) {
            instance = new SessionManager();
        }
        return instance;
    }

    public Session createSession(String sessionId) {
        Session session = new Session(sessionId);
        sessions.put(sessionId, session);
        newSessions.add(sessionId); // Marquer comme nouvelle
        return session;
    }

    public boolean isNewSession(String sessionId) {
        boolean isNew = newSessions.contains(sessionId);
        newSessions.remove(sessionId); // Retirer après vérification
        return isNew;
    }

    public Session getSession(String sessionId) {
        Session session = sessions.get(sessionId);
        if (session != null && !session.isExpired(SESSION_TIMEOUT)) {
            session.updateLastAccessTime();
            return session;
        }
        if (session != null) {
            sessions.remove(sessionId);
        }
        return null;
    }

    public void destroySession(String sessionId) {
        sessions.remove(sessionId);
    }

    private void startCleanupThread() {
        Thread cleanupThread = new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(5 * 60 * 1000); // Vérifier toutes les 5 minutes
                    sessions.entrySet().removeIf(entry ->
                        entry.getValue().isExpired(SESSION_TIMEOUT)
                    );
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        });
        cleanupThread.setDaemon(true);
        cleanupThread.start();
    }

    public boolean isSessionValid(String sessionId) {
        return getSession(sessionId) != null;
    }
}
