package fw.session;

import java.util.*;

public class Session {
    private final String id;
    private final Map<String, Object> attributes;
    private long createdAt;
    private long lastAccessTime;

    public Session(String id) {
        this.id = id;
        this.attributes = new HashMap<>();
        this.createdAt = System.currentTimeMillis();
        this.lastAccessTime = createdAt;
    }

    public String getId() {
        return id;
    }

    public void setAttribute(String key, Object value) {
        attributes.put(key, value);
    }

    public Object getAttribute(String key) {
        return attributes.get(key);
    }

    public void removeAttribute(String key) {
        attributes.remove(key);
    }

    public Map<String, Object> getAllAttributes() {
        return new HashMap<>(attributes);
    }

    public void updateLastAccessTime() {
        this.lastAccessTime = System.currentTimeMillis();
    }

    public boolean isExpired(long timeout) {
        return (System.currentTimeMillis() - lastAccessTime) > timeout;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public long getLastAccessTime() {
        return lastAccessTime;
    }
}