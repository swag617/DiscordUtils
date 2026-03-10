package com.swag.discordutils.link;

import com.sun.net.httpserver.HttpServer;
import com.swag.discordutils.DiscordUtils;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

public class LinkHttpServer {

    private final DiscordUtils plugin;
    private HttpServer server;

    public LinkHttpServer(DiscordUtils plugin) {
        this.plugin = plugin;
    }

    public void start() throws IOException {
        int port = plugin.getConfig().getInt("link.port", 4567);
        server = HttpServer.create(new InetSocketAddress(port), 0);

        server.createContext("/link", exchange -> {
            try {
                Map<String, String> params = parseQuery(exchange.getRequestURI().getQuery());
                String state = params.get("state");
                String code  = params.get("code");

                boolean success = false;
                if (state != null && code != null) {
                    success = plugin.getLinkManager().completeLinkOAuth2(state, code);
                }

                String html = success
                        ? "<html><body style='font-family:sans-serif;text-align:center;padding:60px;background:#2b2d31;color:#dbdee1'>"
                          + "<h1 style='color:#57f287'>&#9989; Account Linked!</h1>"
                          + "<p>Your Minecraft account has been successfully linked to your Discord account.</p>"
                          + "<p style='color:#949ba4'>You can close this tab and return to Minecraft.</p>"
                          + "</body></html>"
                        : "<html><body style='font-family:sans-serif;text-align:center;padding:60px;background:#2b2d31;color:#dbdee1'>"
                          + "<h1 style='color:#ed4245'>&#10060; Link Failed</h1>"
                          + "<p>The link code is invalid or has expired (codes expire after 10 minutes).</p>"
                          + "<p style='color:#949ba4'>Run <code>/discordlink</code> in Minecraft to get a new code.</p>"
                          + "</body></html>";

                byte[] response = html.getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
                exchange.sendResponseHeaders(200, response.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(response);
                }
            } catch (Exception e) {
                plugin.getLogger().warning("Link HTTP server error: " + e.getMessage());
            } finally {
                exchange.close();
            }
        });

        server.setExecutor(null);
        server.start();
        plugin.getLogger().info("Discord link server started on port " + port + ".");
    }

    public void stop() {
        if (server != null) {
            server.stop(0);
            plugin.getLogger().info("Discord link server stopped.");
        }
    }

    private Map<String, String> parseQuery(String query) {
        if (query == null || query.isEmpty()) return Map.of();
        return Arrays.stream(query.split("&"))
                .map(kv -> kv.split("=", 2))
                .filter(kv -> kv.length == 2)
                .collect(Collectors.toMap(kv -> kv[0], kv -> decode(kv[1])));
    }

    private String decode(String s) {
        try {
            return java.net.URLDecoder.decode(s, StandardCharsets.UTF_8);
        } catch (Exception e) {
            return s;
        }
    }
}
