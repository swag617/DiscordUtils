package com.swag.discordutils.webhook;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks per-webhook-name send outcomes so the dashboard (and admins in general) can see
 * whether a configured webhook is actually working, without needing to watch the console.
 */
public class WebhookStats {

    public record Entry(Instant lastSentAt, boolean lastSuccess, String lastError, int totalSent) {}

    private final ConcurrentHashMap<String, Entry> entries = new ConcurrentHashMap<>();

    public void recordSuccess(String name) {
        if (name == null) return;
        entries.merge(name, new Entry(Instant.now(), true, null, 1),
                (old, fresh) -> new Entry(fresh.lastSentAt(), true, null, old.totalSent() + 1));
    }

    public void recordFailure(String name, String error) {
        if (name == null) return;
        entries.merge(name, new Entry(Instant.now(), false, error, 1),
                (old, fresh) -> new Entry(fresh.lastSentAt(), false, error, old.totalSent() + 1));
    }

    /** Read-only snapshot of every webhook name that has attempted at least one send. */
    public Map<String, Entry> snapshot() {
        return Map.copyOf(entries);
    }
}
