package com.swag.discordutils.web;

import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.io.InputStream;

/**
 * Serves static files from /web/ in the plugin jar.
 * Paths arrive already stripped of the /swagapi/discordutils/ prefix by IWebService.
 * Routes: GET / or GET /index.html -> index.html
 *         GET /app.js              -> app.js
 *         GET /style.css           -> style.css
 */
public class WebStaticHandler {

    public void handle(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();

        if (path.equals("/") || path.isEmpty() || path.equals("/index.html")) {
            path = "index.html";
        } else if (path.startsWith("/")) {
            path = path.substring(1);
        }

        String resourcePath = "web/" + path;
        String contentType = guessContentType(path);

        InputStream stream = getClass().getClassLoader().getResourceAsStream(resourcePath);
        if (stream == null) {
            WebResponseUtil.sendPlain(exchange, 404, "Not Found: " + path);
            return;
        }

        byte[] data;
        try (stream) {
            data = stream.readAllBytes();
        }

        exchange.getResponseHeaders().set("Content-Type", contentType);
        exchange.sendResponseHeaders(200, data.length);
        try (var os = exchange.getResponseBody()) {
            os.write(data);
        }
    }

    private String guessContentType(String path) {
        if (path.endsWith(".html")) return "text/html; charset=UTF-8";
        if (path.endsWith(".js"))   return "application/javascript; charset=UTF-8";
        if (path.endsWith(".css"))  return "text/css; charset=UTF-8";
        if (path.endsWith(".json")) return "application/json";
        if (path.endsWith(".png"))  return "image/png";
        if (path.endsWith(".ico"))  return "image/x-icon";
        return "application/octet-stream";
    }
}
