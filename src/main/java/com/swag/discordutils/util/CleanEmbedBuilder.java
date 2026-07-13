package com.swag.discordutils.util;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;

import java.awt.Color;
import java.time.temporal.TemporalAccessor;

/**
 * Fluent embed builder that automatically strips all Minecraft color codes,
 * MiniMessage tags, and hex colors from every text field before embedding.
 * Use this everywhere instead of new EmbedBuilder() — callers never need to
 * manually call FormattingUtil.stripFormatting().
 *
 * Uses composition rather than inheritance because several EmbedBuilder methods
 * are final in JDA 5 and cannot be overridden.
 *
 * URL fields (thumbnail, image, author URL) are intentionally NOT stripped.
 */
public class CleanEmbedBuilder {

    private final EmbedBuilder inner = new EmbedBuilder();

    private static String clean(String s) {
        return FormattingUtil.stripFormatting(s);
    }

    // ── Text fields (stripped) ───────────────────────────────────────────────

    public CleanEmbedBuilder setTitle(String title) {
        inner.setTitle(clean(title));
        return this;
    }

    public CleanEmbedBuilder setTitle(String title, String url) {
        inner.setTitle(clean(title), url);
        return this;
    }

    public CleanEmbedBuilder setDescription(CharSequence description) {
        inner.setDescription(description == null ? null : clean(description.toString()));
        return this;
    }

    public CleanEmbedBuilder appendDescription(CharSequence description) {
        inner.appendDescription(description == null ? null : clean(description.toString()));
        return this;
    }

    public CleanEmbedBuilder addField(String name, String value, boolean inline) {
        inner.addField(clean(name), clean(value), inline);
        return this;
    }

    public CleanEmbedBuilder addField(MessageEmbed.Field field) {
        if (field == null) return this;
        inner.addField(clean(field.getName()), clean(field.getValue()), field.isInline());
        return this;
    }

    public CleanEmbedBuilder setAuthor(String name) {
        inner.setAuthor(clean(name));
        return this;
    }

    public CleanEmbedBuilder setAuthor(String name, String url) {
        inner.setAuthor(clean(name), url);
        return this;
    }

    public CleanEmbedBuilder setAuthor(String name, String url, String iconUrl) {
        inner.setAuthor(clean(name), url, iconUrl);
        return this;
    }

    public CleanEmbedBuilder setFooter(String text) {
        inner.setFooter(clean(text));
        return this;
    }

    public CleanEmbedBuilder setFooter(String text, String iconUrl) {
        inner.setFooter(clean(text), iconUrl);
        return this;
    }

    // ── Non-text fields (pass through unchanged) ─────────────────────────────

    public CleanEmbedBuilder setColor(Color color) {
        inner.setColor(color);
        return this;
    }

    public CleanEmbedBuilder setColor(int color) {
        inner.setColor(color);
        return this;
    }

    public CleanEmbedBuilder setTimestamp(TemporalAccessor temporal) {
        inner.setTimestamp(temporal);
        return this;
    }

    public CleanEmbedBuilder setThumbnail(String url) {
        inner.setThumbnail(url);
        return this;
    }

    public CleanEmbedBuilder setImage(String url) {
        inner.setImage(url);
        return this;
    }

    public CleanEmbedBuilder setUrl(String url) {
        inner.setUrl(url);
        return this;
    }

    public MessageEmbed build() {
        return inner.build();
    }
}
