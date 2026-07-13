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
import java.util.logging.Logger;

/**
 * Posts messages to a raw Discord webhook URL via plain HTTP — no bot, no JDA, no
 * gateway connection. A webhook URL already encodes its destination channel, so
 * unlike {@code DiscordBot}'s JDA-based sends there is no guild/channel lookup here.
 *
 * <p>JSON is hand-built (matching the dependency-free approach already used in
 * SwagAC's DiscordNotifier) rather than pulling in a JSON library for a handful of
 * flat fields.</p>
 */
public class WebhookSender {

    private final Logger logger;
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    public WebhookSender(Logger logger) {
        this.logger = logger;
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
        private final List<Field> fields = new ArrayList<>();
        private byte[] imageBytes;
        private String imageFilename;
        private boolean imageIsThumbnail;

        public Embed title(String v)             { this.title = v; return this; }
        public Embed description(String v)        { this.description = v; return this; }
        public Embed color(int v)                  { this.color = v; return this; }
        public Embed thumbnailUrl(String v)        { this.thumbnailUrl = v; return this; }
        public Embed timestamp(Instant v)           { this.timestamp = v; return this; }
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

    /** Sends a plain-text message (no embed) via webhook. */
    public void sendContent(String webhookUrl, String content) {
        if (webhookUrl == null || webhookUrl.isEmpty()) return;
        String json = "{\"content\":\"" + escapeJson(content) + "\"}";
        post(webhookUrl, json);
    }

    /** Sends an embed via webhook, uploading its image as multipart if one was attached. */
    public void send(String webhookUrl, Embed embed) {
        if (webhookUrl == null || webhookUrl.isEmpty()) return;

        String payloadJson = "{\"embeds\":[" + buildEmbedJson(embed) + "]}";
        if (embed.imageBytes != null && embed.imageBytes.length > 0) {
            postMultipart(webhookUrl, payloadJson, embed.imageBytes, embed.imageFilename);
        } else {
            post(webhookUrl, payloadJson);
        }
    }

    // -------------------------------------------------------------------------
    // JSON building
    // -------------------------------------------------------------------------

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
    // HTTP
    // -------------------------------------------------------------------------

    private void post(String webhookUrl, String json) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(webhookUrl))
                .timeout(Duration.ofSeconds(10))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

        sendAsync(request);
    }

    private void postMultipart(String webhookUrl, String payloadJson, byte[] fileBytes, String filename) {
        String boundary = "----DiscordUtilsWebhook" + UUID.randomUUID();
        byte[] body = buildMultipartBody(boundary, payloadJson, fileBytes, filename);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(webhookUrl))
                .timeout(Duration.ofSeconds(10))
                .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                .POST(HttpRequest.BodyPublishers.ofByteArray(body))
                .build();

        sendAsync(request);
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

    private void sendAsync(HttpRequest request) {
        httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .whenComplete((response, ex) -> {
                    if (ex != null) {
                        logger.warning("Discord webhook request failed: " + ex.getMessage());
                    } else if (response.statusCode() < 200 || response.statusCode() >= 300) {
                        logger.warning("Discord webhook returned HTTP " + response.statusCode() + ": " + response.body());
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
