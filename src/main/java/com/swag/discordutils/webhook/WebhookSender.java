package com.swag.discordutils.webhook;

import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.logging.Logger;

/**
 * Posts messages to a raw Discord webhook URL via plain HTTP — no bot, no JDA, no
 * gateway connection. A webhook URL already encodes its destination channel, so
 * unlike {@code DiscordBot}'s JDA-based sends there is no guild/channel lookup here.
 *
 * <p>JSON is hand-built (matching the dependency-free approach already used in
 * SwagAC's DiscordNotifier) rather than pulling in a JSON library for a handful of
 * flat fields.</p>
 *
 * <p>Every actual send (except {@link #sendTestWithCallback}) is queued per-webhook-URL
 * and drained at a fixed safe rate — Discord enforces roughly 5 requests / 2 seconds per
 * webhook, and with multiple plugins now able to share a webhook name via
 * {@code discordutils:notify}, a burst from any one of them could otherwise start
 * silently dropping messages for all of them.</p>
 */
public class WebhookSender {

    private static final long MIN_INTERVAL_MS = 500L;

    private final Logger logger;
    private final WebhookStats stats = new WebhookStats();
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    private final ConcurrentHashMap<String, ConcurrentLinkedQueue<Runnable>> queues = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Long> lastDispatchMs = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "DiscordUtils-WebhookQueue");
        t.setDaemon(true);
        return t;
    });

    public WebhookSender(Logger logger) {
        this.logger = logger;
        scheduler.scheduleAtFixedRate(this::drainQueues, 100, 100, TimeUnit.MILLISECONDS);
    }

    public WebhookStats getStats() {
        return stats;
    }

    /** Stops the queue-draining scheduler. Call from the owning plugin's onDisable(). */
    public void shutdown() {
        scheduler.shutdownNow();
    }

    /** A single embed field: name, value, and whether it displays inline. */
    public record Field(String name, String value, boolean inline) {}

    /** Fluent embed spec — mirrors {@code CleanEmbedBuilder}'s shape for a familiar feel. */
    public static final class Embed {
        private String title;
        private String description;
        private Integer color;
        private String thumbnailUrl;
        private Instant timestamp;
        private String username;
        private String avatarUrl;
        private final List<Field> fields = new ArrayList<>();
        private byte[] imageBytes;
        private String imageFilename;
        private boolean imageIsThumbnail;

        public Embed title(String v)             { this.title = v; return this; }
        public Embed description(String v)        { this.description = v; return this; }
        public Embed color(int v)                  { this.color = v; return this; }
        public Embed thumbnailUrl(String v)        { this.thumbnailUrl = v; return this; }
        public Embed timestamp(Instant v)           { this.timestamp = v; return this; }
        /** Overrides the webhook's default display name for just this message. */
        public Embed username(String v)             { this.username = v; return this; }
        /** Overrides the webhook's default avatar for just this message. */
        public Embed avatarUrl(String v)             { this.avatarUrl = v; return this; }
        public Embed field(String name, String value, boolean inline) {
            fields.add(new Field(name, value, inline));
            return this;
        }
        /** Attaches an image (e.g. an item tooltip render) as the embed's main image, sent as multipart. */
        public Embed image(byte[] bytes, String filename) {
            this.imageBytes = bytes;
            this.imageFilename = filename;
            this.imageIsThumbnail = false;
            return this;
        }
        /** Attaches an image (e.g. an auction badge) as the embed's thumbnail, sent as multipart. */
        public Embed thumbnailImage(byte[] bytes, String filename) {
            this.imageBytes = bytes;
            this.imageFilename = filename;
            this.imageIsThumbnail = true;
            return this;
        }
    }

    /** Sends a plain-text message (no embed), with no identity override or stats tracking. */
    public void sendContent(String webhookUrl, String content) {
        sendContent(webhookUrl, content, null, null, null);
    }

    /**
     * Sends a plain-text message (no embed).
     *
     * @param username   overrides the webhook's default name for this message, or null
     * @param avatarUrl  overrides the webhook's default avatar for this message, or null
     * @param statsName  attributes the outcome to this name in {@link #getStats()}, or null to skip tracking
     */
    public void sendContent(String webhookUrl, String content, String username, String avatarUrl, String statsName) {
        if (webhookUrl == null || webhookUrl.isEmpty()) return;

        StringBuilder json = new StringBuilder("{");
        boolean any = false;
        if (username != null && !username.isEmpty()) {
            json.append("\"username\":\"").append(escapeJson(username)).append("\"");
            any = true;
        }
        if (avatarUrl != null && !avatarUrl.isEmpty()) {
            if (any) json.append(",");
            json.append("\"avatar_url\":\"").append(escapeJson(avatarUrl)).append("\"");
            any = true;
        }
        if (any) json.append(",");
        json.append("\"content\":\"").append(escapeJson(content)).append("\"");
        json.append("}");

        post(webhookUrl, json.toString(), statsName);
    }

    /** Sends an embed via webhook, with no stats tracking. */
    public void send(String webhookUrl, Embed embed) {
        send(webhookUrl, embed, null);
    }

    /**
     * Sends an embed via webhook, uploading its image as multipart if one was attached.
     *
     * @param statsName attributes the outcome to this name in {@link #getStats()}, or null to skip tracking
     */
    public void send(String webhookUrl, Embed embed, String statsName) {
        if (webhookUrl == null || webhookUrl.isEmpty()) return;

        String payloadJson = buildTopLevelJson(embed);
        if (embed.imageBytes != null && embed.imageBytes.length > 0) {
            postMultipart(webhookUrl, payloadJson, embed.imageBytes, embed.imageFilename, statsName);
        } else {
            post(webhookUrl, payloadJson, statsName);
        }
    }

    /**
     * Sends a one-off confirmation embed immediately, bypassing the rate-limit queue (this
     * is a deliberate, rare admin action — e.g. {@code /discordwebhook set} or the
     * dashboard's "Test" button — not bulk traffic) and reports success/failure via callback
     * once the HTTP round-trip completes.
     */
    public void sendTestWithCallback(String webhookUrl, Consumer<Boolean> onResult) {
        if (webhookUrl == null || webhookUrl.isEmpty()) {
            onResult.accept(false);
            return;
        }
        String json = "{\"embeds\":[{\"title\":\"\\u2705 DiscordUtils Test\","
                + "\"description\":\"This webhook is configured correctly.\",\"color\":5793266}]}";
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(webhookUrl))
                .timeout(Duration.ofSeconds(10))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

        httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .whenComplete((response, ex) -> {
                    boolean success = ex == null && response.statusCode() >= 200 && response.statusCode() < 300;
                    onResult.accept(success);
                });
    }

    // -------------------------------------------------------------------------
    // JSON building
    // -------------------------------------------------------------------------

    private String buildTopLevelJson(Embed embed) {
        StringBuilder json = new StringBuilder("{");
        boolean any = false;
        if (embed.username != null && !embed.username.isEmpty()) {
            json.append("\"username\":\"").append(escapeJson(embed.username)).append("\"");
            any = true;
        }
        if (embed.avatarUrl != null && !embed.avatarUrl.isEmpty()) {
            if (any) json.append(",");
            json.append("\"avatar_url\":\"").append(escapeJson(embed.avatarUrl)).append("\"");
            any = true;
        }
        if (any) json.append(",");
        json.append("\"embeds\":[").append(buildEmbedJson(embed)).append("]");
        json.append("}");
        return json.toString();
    }

    private String buildEmbedJson(Embed embed) {
        StringBuilder json = new StringBuilder("{");
        boolean any = false;

        if (embed.title != null && !embed.title.isEmpty()) {
            json.append("\"title\":\"").append(escapeJson(embed.title)).append("\"");
            any = true;
        }
        if (embed.description != null && !embed.description.isEmpty()) {
            if (any) json.append(",");
            json.append("\"description\":\"").append(escapeJson(embed.description)).append("\"");
            any = true;
        }
        if (embed.color != null) {
            if (any) json.append(",");
            json.append("\"color\":").append(embed.color);
            any = true;
        }
        if (embed.timestamp != null) {
            if (any) json.append(",");
            json.append("\"timestamp\":\"").append(embed.timestamp.toString()).append("\"");
            any = true;
        }
        if (embed.thumbnailUrl != null && !embed.thumbnailUrl.isEmpty()) {
            if (any) json.append(",");
            json.append("\"thumbnail\":{\"url\":\"").append(escapeJson(embed.thumbnailUrl)).append("\"}");
            any = true;
        }
        if (!embed.fields.isEmpty()) {
            if (any) json.append(",");
            json.append("\"fields\":[");
            for (int i = 0; i < embed.fields.size(); i++) {
                Field f = embed.fields.get(i);
                if (i > 0) json.append(",");
                json.append("{\"name\":\"").append(escapeJson(f.name())).append("\",")
                        .append("\"value\":\"").append(escapeJson(f.value())).append("\",")
                        .append("\"inline\":").append(f.inline()).append("}");
            }
            json.append("]");
            any = true;
        }
        if (embed.imageFilename != null && !embed.imageFilename.isEmpty()) {
            if (any) json.append(",");
            String target = embed.imageIsThumbnail ? "thumbnail" : "image";
            json.append("\"").append(target).append("\":{\"url\":\"attachment://").append(embed.imageFilename).append("\"}");
        }
        json.append("}");
        return json.toString();
    }

    // -------------------------------------------------------------------------
    // HTTP + rate-limit queue
    // -------------------------------------------------------------------------

    private void post(String webhookUrl, String json, String statsName) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(webhookUrl))
                .timeout(Duration.ofSeconds(10))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

        enqueue(webhookUrl, () -> sendAsync(request, statsName));
    }

    private void postMultipart(String webhookUrl, String payloadJson, byte[] fileBytes, String filename, String statsName) {
        String boundary = "----DiscordUtilsWebhook" + UUID.randomUUID();
        byte[] body = buildMultipartBody(boundary, payloadJson, fileBytes, filename);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(webhookUrl))
                .timeout(Duration.ofSeconds(10))
                .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                .POST(HttpRequest.BodyPublishers.ofByteArray(body))
                .build();

        enqueue(webhookUrl, () -> sendAsync(request, statsName));
    }

    private void enqueue(String webhookUrl, Runnable task) {
        queues.computeIfAbsent(webhookUrl, k -> new ConcurrentLinkedQueue<>()).add(task);
    }

    /** Ticks every 100ms; pops and fires at most one queued send per webhook URL per {@link #MIN_INTERVAL_MS}. */
    private void drainQueues() {
        long now = System.currentTimeMillis();
        for (var entry : queues.entrySet()) {
            String url = entry.getKey();
            ConcurrentLinkedQueue<Runnable> queue = entry.getValue();
            if (queue.isEmpty()) continue;

            long last = lastDispatchMs.getOrDefault(url, 0L);
            if (now - last < MIN_INTERVAL_MS) continue;

            Runnable task = queue.poll();
            if (task != null) {
                lastDispatchMs.put(url, now);
                task.run();
            }
        }
    }

    private byte[] buildMultipartBody(String boundary, String payloadJson, byte[] fileBytes, String filename) {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            String crlf = "\r\n";

            out.write(("--" + boundary + crlf).getBytes(StandardCharsets.UTF_8));
            out.write(("Content-Disposition: form-data; name=\"payload_json\"" + crlf).getBytes(StandardCharsets.UTF_8));
            out.write(("Content-Type: application/json" + crlf + crlf).getBytes(StandardCharsets.UTF_8));
            out.write(payloadJson.getBytes(StandardCharsets.UTF_8));
            out.write(crlf.getBytes(StandardCharsets.UTF_8));

            out.write(("--" + boundary + crlf).getBytes(StandardCharsets.UTF_8));
            out.write(("Content-Disposition: form-data; name=\"files[0]\"; filename=\"" + filename + "\"" + crlf)
                    .getBytes(StandardCharsets.UTF_8));
            out.write(("Content-Type: image/png" + crlf + crlf).getBytes(StandardCharsets.UTF_8));
            out.write(fileBytes);
            out.write(crlf.getBytes(StandardCharsets.UTF_8));

            out.write(("--" + boundary + "--" + crlf).getBytes(StandardCharsets.UTF_8));
            return out.toByteArray();
        } catch (Exception e) {
            logger.warning("Failed to build multipart webhook body: " + e.getMessage());
            return new byte[0];
        }
    }

    private void sendAsync(HttpRequest request, String statsName) {
        httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .whenComplete((response, ex) -> {
                    if (ex != null) {
                        logger.warning("Discord webhook request failed: " + ex.getMessage());
                        stats.recordFailure(statsName, ex.getMessage());
                    } else if (response.statusCode() < 200 || response.statusCode() >= 300) {
                        logger.warning("Discord webhook returned HTTP " + response.statusCode() + ": " + response.body());
                        stats.recordFailure(statsName, "HTTP " + response.statusCode());
                    } else {
                        stats.recordSuccess(statsName);
                    }
                });
    }

    private String escapeJson(String input) {
        if (input == null) return "";
        return input
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}
