package com.swag.discordutils.web;

import com.swag.discordutils.DiscordUtils;
import com.swag.discordutils.webhook.WebhookStats;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * JSON REST backend for the dashboard — same hand-rolled routing shape as SwagAC's
 * WebApiHandler (path/method if-chain, JsonUtil for responses, try/catch wrapping the
 * whole dispatch).
 */
public class WebApiHandler {

    private final DiscordUtils plugin;

    public WebApiHandler(DiscordUtils plugin) {
        this.plugin = plugin;
    }

    public void handle(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        String method = exchange.getRequestMethod();

        try {
            if (path.equals("/api/webhooks") && method.equals("GET")) {
                handleList(exchange);
                return;
            }
            if (path.equals("/api/webhooks") && method.equals("POST")) {
                handleSet(exchange);
                return;
            }
            if (path.equals("/api/webhooks/test") && method.equals("POST")) {
                handleTest(exchange);
                return;
            }

            WebResponseUtil.sendJson(exchange, 404, JsonUtil.obj("error", "Not found"));
        } catch (Exception e) {
            plugin.getLogger().warning("Dashboard API error on " + path + ": " + e.getMessage());
            WebResponseUtil.sendJson(exchange, 500, JsonUtil.obj("error", "Internal error: " + e.getMessage()));
        }
    }

    private void handleList(HttpExchange exchange) throws IOException {
        var section = plugin.getConfig().getConfigurationSection("webhooks");
        Map<String, WebhookStats.Entry> stats = plugin.getDiscordBot().getWebhookSender().getStats().snapshot();

        // Union of configured names and anything that's ever recorded a stat (covers
        // ad-hoc discordutils:notify senders that used a raw webhookUrl under a name
        // never added to webhooks.* here).
        TreeSet<String> names = new TreeSet<>();
        if (section != null) names.addAll(section.getKeys(false));
        names.addAll(stats.keySet());

        List<String> rows = new ArrayList<>();
        for (String name : names) {
            String url = section != null ? section.getString(name, "") : "";
            WebhookStats.Entry entry = stats.get(name);

            List<Object> kv = new ArrayList<>(List.of(
                    "name", name,
                    "urlMasked", mask(url),
                    "configured", !url.isEmpty()
            ));
            if (entry != null) {
                kv.add("lastSentAt");
                kv.add(DateTimeFormatter.ISO_INSTANT.format(entry.lastSentAt()));
                kv.add("lastSuccess");
                kv.add(entry.lastSuccess());
                kv.add("lastError");
                kv.add(entry.lastError());
                kv.add("totalSent");
                kv.add(entry.totalSent());
            }
            rows.add(JsonUtil.obj(kv.toArray()));
        }

        WebResponseUtil.sendJson(exchange, 200, JsonUtil.obj("webhooks", JsonUtil.raw(JsonUtil.arr(rows))));
    }

    private void handleSet(HttpExchange exchange) throws IOException {
        Map<String, String> form = parseForm(exchange);
        String name = form.getOrDefault("name", "").trim().toLowerCase();
        String url = form.getOrDefault("url", "").trim();

        if (name.isEmpty() || url.isEmpty()) {
            WebResponseUtil.sendJson(exchange, 400, JsonUtil.obj("error", "Missing name or url"));
            return;
        }

        plugin.getConfig().set("webhooks." + name, url);
        plugin.saveConfig();

        boolean success = awaitTest(url);
        WebResponseUtil.sendJson(exchange, 200, JsonUtil.obj("saved", true, "testSuccess", success));
    }

    private void handleTest(HttpExchange exchange) throws IOException {
        Map<String, String> form = parseForm(exchange);
        String name = form.getOrDefault("name", "").trim().toLowerCase();
        String url = plugin.getConfig().getString("webhooks." + name, "");

        if (url.isEmpty()) {
            WebResponseUtil.sendJson(exchange, 404, JsonUtil.obj("error", "No webhook named " + name));
            return;
        }

        boolean success = awaitTest(url);
        WebResponseUtil.sendJson(exchange, 200, JsonUtil.obj("testSuccess", success));
    }

    /** Blocks briefly (max 8s) so the HTTP response can report the real test outcome. */
    private boolean awaitTest(String url) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        plugin.getDiscordBot().getWebhookSender().sendTestWithCallback(url, future::complete);
        try {
            return future.get(8, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            return false;
        } catch (Exception e) {
            plugin.getLogger().warning("Webhook test wait interrupted: " + e.getMessage());
            return false;
        }
    }

    private String mask(String url) {
        if (url == null || url.isEmpty()) return "";
        if (url.length() <= 10) return "***";
        return "..." + url.substring(url.length() - 6);
    }

    private static Map<String, String> parseForm(HttpExchange exchange) throws IOException {
        byte[] bodyBytes = exchange.getRequestBody().readAllBytes();
        String body = new String(bodyBytes, StandardCharsets.UTF_8);
        Map<String, String> out = new LinkedHashMap<>();
        for (String pair : body.split("&")) {
            if (pair.isEmpty()) continue;
            String[] kv = pair.split("=", 2);
            String key = URLDecoder.decode(kv[0], StandardCharsets.UTF_8);
            String val = kv.length > 1 ? URLDecoder.decode(kv[1], StandardCharsets.UTF_8) : "";
            out.put(key, val);
        }
        return out;
    }
}
