package com.swag.discordutils.util;

import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import javax.imageio.ImageIO;

/**
 * Generates colored badge PNG images for auction house Discord embeds.
 * Images are rendered once and cached as byte arrays for the lifetime of the plugin.
 *
 *  SOLD         – green  circle + white checkmark
 *  LISTED       – yellow circle + white star
 *  REMOVED      – red    circle + white X
 *  ADMIN REMOVED– red    circle + white warning triangle
 */
public class AuctionBadgeRenderer {

    private static final int SIZE = 200;

    private static byte[] soldCache;
    private static byte[] listedCache;
    private static byte[] removedCache;
    private static byte[] adminRemovedCache;

    public static synchronized byte[] getSold() throws IOException {
        if (soldCache == null) soldCache = renderCheckmark(new Color(0x57, 0xF2, 0x87));
        return soldCache;
    }

    public static synchronized byte[] getListed() throws IOException {
        if (listedCache == null) listedCache = renderStar(new Color(0xFF, 0xD7, 0x00));
        return listedCache;
    }

    public static synchronized byte[] getRemoved() throws IOException {
        if (removedCache == null) removedCache = renderCross(new Color(0xED, 0x42, 0x45));
        return removedCache;
    }

    public static synchronized byte[] getAdminRemoved() throws IOException {
        if (adminRemovedCache == null) adminRemovedCache = renderWarning(new Color(0xED, 0x42, 0x45));
        return adminRemovedCache;
    }

    // -------------------------------------------------------------------------
    // Base canvas
    // -------------------------------------------------------------------------

    private static BufferedImage createBase(Color accent) {
        BufferedImage img = new BufferedImage(SIZE, SIZE, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = setup(img.createGraphics());

        g.setComposite(AlphaComposite.Clear);
        g.fillRect(0, 0, SIZE, SIZE);
        g.setComposite(AlphaComposite.SrcOver);

        g.setColor(new Color(0x2B, 0x2D, 0x31));
        g.fill(new RoundRectangle2D.Float(0, 0, SIZE, SIZE, 36, 36));

        g.setPaint(new GradientPaint(
                SIZE / 2f - 60, SIZE / 2f - 60, accent,
                SIZE / 2f + 60, SIZE / 2f + 60, accent.darker()
        ));
        g.fillOval(26, 26, SIZE - 52, SIZE - 52);

        g.dispose();
        return img;
    }

    // -------------------------------------------------------------------------
    // Symbol renderers
    // -------------------------------------------------------------------------

    /** Green circle + white checkmark */
    private static byte[] renderCheckmark(Color accent) throws IOException {
        BufferedImage img = createBase(accent);
        Graphics2D g = setup(img.createGraphics());

        int cx = SIZE / 2, cy = SIZE / 2;
        g.setColor(Color.WHITE);
        g.setStroke(new BasicStroke(15f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.drawPolyline(
                new int[]{cx - 34, cx - 8, cx + 38},
                new int[]{cy + 4,  cy + 30, cy - 30},
                3);

        g.dispose();
        return toBytes(img);
    }

    /** Red circle + white X */
    private static byte[] renderCross(Color accent) throws IOException {
        BufferedImage img = createBase(accent);
        Graphics2D g = setup(img.createGraphics());

        int cx = SIZE / 2, cy = SIZE / 2, r = 34;
        g.setColor(Color.WHITE);
        g.setStroke(new BasicStroke(15f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.drawLine(cx - r, cy - r, cx + r, cy + r);
        g.drawLine(cx + r, cy - r, cx - r, cy + r);

        g.dispose();
        return toBytes(img);
    }

    /** Yellow circle + white 5-pointed star */
    private static byte[] renderStar(Color accent) throws IOException {
        BufferedImage img = createBase(accent);
        Graphics2D g = setup(img.createGraphics());

        g.setColor(Color.WHITE);
        g.fillPolygon(buildStar(SIZE / 2, SIZE / 2, 40, 17, 5));

        g.dispose();
        return toBytes(img);
    }

    /** Red circle + white warning triangle with exclamation mark */
    private static byte[] renderWarning(Color accent) throws IOException {
        BufferedImage img = createBase(accent);
        Graphics2D g = setup(img.createGraphics());

        int cx = SIZE / 2, cy = SIZE / 2;
        g.setColor(Color.WHITE);

        g.setStroke(new BasicStroke(12f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.drawPolygon(
                new int[]{cx,      cx - 40, cx + 40},
                new int[]{cy - 36, cy + 28, cy + 28},
                3);

        g.fillRoundRect(cx - 5, cy - 15, 10, 22, 4, 4);
        g.fillOval(cx - 5, cy + 13, 10, 10);

        g.dispose();
        return toBytes(img);
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private static Graphics2D setup(Graphics2D g) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,      RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_RENDERING,         RenderingHints.VALUE_RENDER_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL,    RenderingHints.VALUE_STROKE_PURE);
        return g;
    }

    /** Builds a filled n-pointed star polygon centered at (cx, cy). */
    private static Polygon buildStar(int cx, int cy, int outerR, int innerR, int points) {
        Polygon p = new Polygon();
        double angle = -Math.PI / 2;
        double step  = Math.PI / points;
        for (int i = 0; i < points * 2; i++) {
            int r = (i % 2 == 0) ? outerR : innerR;
            p.addPoint(
                    cx + (int) (r * Math.cos(angle)),
                    cy + (int) (r * Math.sin(angle)));
            angle += step;
        }
        return p;
    }

    private static byte[] toBytes(BufferedImage img) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(img, "PNG", baos);
        return baos.toByteArray();
    }
}
