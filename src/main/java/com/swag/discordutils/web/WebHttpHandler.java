package com.swag.discordutils.web;

import com.swag.discordutils.DiscordUtils;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;

/**
 * Single entry point registered with SwagAPI's IWebService.
 * Dispatches /api/... to WebApiHandler and everything else to WebStaticHandler.
 * Authentication is handled entirely by SwagAPI before this handler ever runs.
 *
 * <p>This is a separate, admin-only hook from {@link com.swag.discordutils.link.LinkHttpServer}
 * (the standalone OAuth callback server) — the dashboard should sit behind SwagAPI's
 * panel-login gate, unlike the public OAuth redirect target.</p>
 */
public class WebHttpHandler implements HttpHandler {

    private final WebApiHandler apiHandler;
    private final WebStaticHandler staticHandler;

    public WebHttpHandler(DiscordUtils plugin) {
        this.apiHandler = new WebApiHandler(plugin);
        this.staticHandler = new WebStaticHandler();
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        if (path.startsWith("/api/")) {
            apiHandler.handle(exchange);
        } else {
            staticHandler.handle(exchange);
        }
    }
}
