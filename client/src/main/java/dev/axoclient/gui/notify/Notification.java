package dev.axoclient.gui.notify;

/** A single transient toast. Colour comes from its type at render time. */
public final class Notification {
    public enum Type {
        INFO,
        SUCCESS,
        WARN
    }

    public final String message;
    public final Type type;
    public final long createdMs;
    public final long durationMs;

    public Notification(String message, Type type, long durationMs) {
        this.message = message;
        this.type = type;
        this.durationMs = durationMs;
        this.createdMs = System.currentTimeMillis();
    }

    public long age() {
        return System.currentTimeMillis() - createdMs;
    }

    public boolean expired() {
        return age() > durationMs;
    }
}
