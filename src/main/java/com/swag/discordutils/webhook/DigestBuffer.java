package com.swag.discordutils.webhook;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Buffers text lines per webhook name for digest mode — instead of sending each
 * {@code discordutils:notify} message immediately, lines accumulate here until a
 * scheduled flush combines them into a single embed. Scoped to cross-plugin
 * notifications only; the built-in chat/join-leave/etc. categories already have their
 * own direct webhook-first paths and don't go through this.
 */
public class DigestBuffer {

    private record WebhookTarget(String webhookUrl, String username, String avatarUrl) {}

    public record FlushResult(String webhookUrl, String username, String avatarUrl, List<String> lines) {}

    private final Map<String, List<String>> lines = new ConcurrentHashMap<>();
    private final Map<String, WebhookTarget> targets = new ConcurrentHashMap<>();

    public void append(String webhookName, String webhookUrl, String line, String username, String avatarUrl) {
        lines.computeIfAbsent(webhookName, k -> new CopyOnWriteArrayList<>()).add(line);
        // Last-writer-wins for identity — fine in practice since a given webhook name is
        // almost always published to by one source plugin's notification category.
        targets.put(webhookName, new WebhookTarget(webhookUrl, username, avatarUrl));
    }

    /** Removes and returns everything buffered for this name, or null if nothing was buffered. */
    public FlushResult flush(String webhookName) {
        List<String> flushedLines = lines.remove(webhookName);
        WebhookTarget target = targets.remove(webhookName);
        if (flushedLines == null || flushedLines.isEmpty() || target == null) return null;
        return new FlushResult(target.webhookUrl(), target.username(), target.avatarUrl(), flushedLines);
    }

    /** Snapshot of webhook names that currently have at least one buffered line. */
    public Set<String> bufferedWebhookNames() {
        return Set.copyOf(lines.keySet());
    }
}
