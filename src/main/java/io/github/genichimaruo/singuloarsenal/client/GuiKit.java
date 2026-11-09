package io.github.genichimaruo.singuloarsenal.client;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

/**
 * The parts of Singulo's machine GUI this addon needs, with the same geometry and the same standard skin (light from the upper left,
 * steps of one pixel; pressable things raised, containers sunk). Singulo keeps its own classes package-private, and this addon does not
 * compile against it, so they are reproduced here rather than linked.
 */
final class GuiKit {
    static final int HEADER = 19, POWER_Y = 4, CHIP_WIDTH = 19;
    static final int OK = 0xFF5BD07E, IDLE = 0xFFA3AEB8, WARN = 0xFFF0B450, BAD = 0xFFF06A6A, GLOW = 0xFF78D2F0;
    static final int TEXT = 0xFF33404A, DIM = 0xFF6E7A85, FILL = 0xFF5A646E, FACE2 = 0xFFDCE2E7, ACCENT = 0xFF4AAED2;
    private static final int SHADOW = 0x66000000, SHADOW_SOFT = 0x2E000000;
    private static final int OUTLINE = 0xFF56626C, HI = 0xFFFFFFFF, FACE = 0xFFEBEFF2, LO = 0xFFB1BAC3;
    private static final int PLATE = 0xFFF6F8F9, PLATE2 = 0xFFE5EAEE, SEAM = 0xFFC4CAD0;
    private static final int DARK = 0xFF2B333A, SHADE = 0xFF4E5862, CHIP = 0xFF2A3137, ARROW = 0xFFC3CAD1;
    private static final int BUTTON = 0xFFF3F6F8, BUTTON2 = 0xFFD8DEE3, BUTTON_LO = 0xFF9DA8B2;

    enum Press { NORMAL, HOVER, PRESSED, DISABLED }
    enum Status { RUN, IDLE, WARN, NO_POWER }

    private GuiKit() {}

    static boolean inside(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    static void frame(GuiGraphics g, int x, int y, int w, int h) {
        g.fill(x + 2, y + h, x + w + 1, y + h + 1, SHADOW);
        g.fill(x + w, y + 2, x + w + 1, y + h, SHADOW);
        g.fill(x + 3, y + h + 1, x + w + 1, y + h + 2, SHADOW_SOFT);
        g.fill(x + w + 1, y + 3, x + w + 2, y + h + 1, SHADOW_SOFT);
        g.fill(x + 1, y, x + w - 1, y + 1, OUTLINE);
        g.fill(x + 1, y + h - 1, x + w - 1, y + h, OUTLINE);
        g.fill(x, y + 1, x + 1, y + h - 1, OUTLINE);
        g.fill(x + w - 1, y + 1, x + w, y + h - 1, OUTLINE);
        g.fillGradient(x + 1, y + 1, x + w - 1, y + h - 1, FACE, FACE2);
        g.fill(x + 1, y + 1, x + w - 1, y + 2, HI);
        g.fill(x + 1, y + 1, x + 2, y + h - 1, HI);
        g.fill(x + 1, y + h - 2, x + w - 1, y + h - 1, LO);
        g.fill(x + w - 2, y + 2, x + w - 1, y + h - 1, LO);
    }

    static void header(GuiGraphics g, int x, int y, int w) {
        g.fillGradient(x + 3, y + 3, x + w - 3, y + 16, PLATE, PLATE2);
        g.fill(x + 3, y + 16, x + w - 3, y + 17, SEAM);
        g.fill(x + 3, y + 17, x + w - 3, y + 18, HI);
        g.fill(x + 5, y + 3, x + w - 5, y + 4, GLOW);
    }

    static void groove(GuiGraphics g, int x, int y, int w) {
        g.fill(x, y, x + w, y + 1, SEAM);
        g.fill(x, y + 1, x + w, y + 2, HI);
    }

    static void inset(GuiGraphics g, int x, int y, int w, int h, int fill) {
        g.fill(x, y, x + w, y + 1, DARK);
        g.fill(x, y, x + 1, y + h, DARK);
        g.fill(x + 1, y + h - 1, x + w, y + h, HI);
        g.fill(x + w - 1, y + 1, x + w, y + h, HI);
        g.fill(x + 1, y + 1, x + w - 1, y + h - 1, fill);
    }

    static void slot(GuiGraphics g, int x, int y) {
        inset(g, x - 1, y - 1, 18, 18, FILL);
        g.fill(x, y, x + 16, y + 1, SHADE);
        g.fill(x, y, x + 1, y + 16, SHADE);
    }

    /** A socket this gear does not have: filled in level with the panel and crossed out. */
    static void lockedSlot(GuiGraphics g, int x, int y) {
        inset(g, x - 1, y - 1, 18, 18, FACE2);
        for (int i = 0; i < 7; i++) {
            g.fill(x + 4 + i, y + 4 + i, x + 5 + i, y + 5 + i, LO);
            g.fill(x + 10 - i, y + 4 + i, x + 11 - i, y + 5 + i, LO);
        }
    }

    static void gauge(GuiGraphics g, int x, int y, int w, int h, double fraction, int color) {
        inset(g, x - 1, y - 1, w + 2, h + 2, FILL);
        int filled = (int) Math.round(h * Math.max(0, Math.min(1, fraction)));
        if (filled > 0) {
            g.fillGradient(x, y + h - filled, x + w, y + h, mix(color, 0xFFFFFFFF, 0.35F), mix(color, 0xFF000000, 0.2F));
            g.fill(x, y + h - filled, x + w, y + h - filled + 1, 0x8CFFFFFF);
            g.fill(x, y + h - filled + 1, x + 1, y + h, 0x59FFFFFF);
            if (w > 2) g.fill(x + w - 1, y + h - filled + 1, x + w, y + h, 0x40000000);
        }
        for (int i = 1; i < 4; i++) {
            int ty = y + Math.round(h * i / 4F);
            g.fill(x + w - 2, ty, x + w, ty + 1, 0x73FFFFFF);
        }
    }

    static void arrow(GuiGraphics g, int x, int y, double fraction) {
        arrowShape(g, x, y + 1, HI, 22);
        arrowShape(g, x, y, ARROW, 22);
        int filled = (int) Math.round(22 * Math.max(0, Math.min(1, fraction)));
        if (filled > 0) arrowShape(g, x, y, GLOW, filled);
    }

    private static void arrowShape(GuiGraphics g, int x, int y, int color, int limit) {
        g.fill(x, y + 6, x + Math.min(limit, 16), y + 10, color);
        for (int i = 0; i < 8 && 14 + i < limit; i++) g.fill(x + 14 + i, y + i, x + 15 + i, y + 16 - i, color);
    }

    /** The status window at the right end of the header: an LED and a glyph. */
    static void chip(GuiGraphics g, int right, int y, Status status, int color) {
        int left = right - CHIP_WIDTH;
        inset(g, left, y, CHIP_WIDTH, 11, CHIP);
        g.fill(left + 3, y + 4, left + 6, y + 7, color);
        g.fill(left + 3, y + 4, left + 4, y + 5, 0xFFFFFFFF);
        int gx = left + 9, gy = y + 2;
        switch (status) {
            case RUN -> { for (int i = 0; i < 4; i++) g.fill(gx + 1 + i, gy + i, gx + 2 + i, gy + 7 - i, color); }
            case IDLE -> { for (int i = 0; i <= 6; i += 3) g.fill(gx + i, gy + 5, gx + i + 1, gy + 6, color); }
            case WARN -> { g.fill(gx + 3, gy, gx + 4, gy + 4, color); g.fill(gx + 3, gy + 5, gx + 4, gy + 7, color); }
            case NO_POWER -> bolt(g, gx, gy - 1, color);
        }
    }

    static int button(GuiGraphics g, int x, int y, int w, int h, Press state) {
        int edge = state == Press.HOVER ? ACCENT : state == Press.DISABLED ? LO : OUTLINE;
        g.fill(x + 1, y, x + w - 1, y + 1, edge);
        g.fill(x + 1, y + h - 1, x + w - 1, y + h, edge);
        g.fill(x, y + 1, x + 1, y + h - 1, edge);
        g.fill(x + w - 1, y + 1, x + w, y + h - 1, edge);
        int ix = x + 1, iy = y + 1, iw = w - 2, ih = h - 2;
        if (state == Press.DISABLED) {
            g.fill(ix, iy, ix + iw, iy + ih, BUTTON2);
            return 0;
        }
        if (state == Press.PRESSED) {
            g.fill(ix, iy, ix + iw, iy + ih, BUTTON2);
            g.fill(ix, iy, ix + iw, iy + 1, BUTTON_LO);
            g.fill(ix, iy, ix + 1, iy + ih, BUTTON_LO);
            return 1;
        }
        g.fillGradient(ix, iy, ix + iw, iy + ih, state == Press.HOVER ? HI : BUTTON, BUTTON2);
        g.fill(ix, iy, ix + iw, iy + 1, HI);
        g.fill(ix, iy, ix + 1, iy + ih - 1, HI);
        g.fill(ix, iy + ih - 1, ix + iw, iy + ih, BUTTON_LO);
        g.fill(ix + iw - 1, iy + 1, ix + iw, iy + ih, BUTTON_LO);
        return 0;
    }

    static void labelButton(GuiGraphics g, Font font, int x, int y, int w, int h, Component label, Press state) {
        int d = button(g, x, y, w, h, state);
        g.drawString(font, label, x + (w - font.width(label)) / 2, y + (h - 8) / 2 + d, state == Press.DISABLED ? DIM : TEXT, false);
    }

    static void bolt(GuiGraphics g, int x, int y, int c) {
        g.fill(x + 4, y, x + 6, y + 2, c);
        g.fill(x + 3, y + 2, x + 5, y + 4, c);
        g.fill(x + 2, y + 4, x + 6, y + 5, c);
        g.fill(x + 4, y + 5, x + 6, y + 6, c);
        g.fill(x + 3, y + 6, x + 5, y + 8, c);
    }

    private static final String[] HOURGLASS = {"xxxxxxx", ".x...x.", "..x.x..", "...x...", "..x.x..", ".x...x.", "xxxxxxx"};

    static void hourglass(GuiGraphics g, int x, int y, int c) {
        for (int j = 0; j < HOURGLASS.length; j++) {
            for (int i = 0; i < HOURGLASS[j].length(); i++) {
                if (HOURGLASS[j].charAt(i) == 'x') g.fill(x + i, y + j, x + i + 1, y + j + 1, c);
            }
        }
    }

    /** Wrapped, and kept with its border inside small windows. */
    static void tooltip(GuiGraphics g, Font font, List<Component> text, int maxWidth, int mouseX, int mouseY) {
        List<FormattedCharSequence> lines = new ArrayList<>();
        for (Component line : text) lines.addAll(font.split(line, Math.min(maxWidth, g.guiWidth() - 8)));
        g.renderTooltip(font, lines, net.minecraft.client.gui.screens.inventory.tooltip.DefaultTooltipPositioner.INSTANCE, mouseX, mouseY);
    }

    private static int mix(int from, int to, float t) {
        int r = Math.round(((from >> 16) & 255) + (((to >> 16) & 255) - ((from >> 16) & 255)) * t);
        int g = Math.round(((from >> 8) & 255) + (((to >> 8) & 255) - ((from >> 8) & 255)) * t);
        int b = Math.round((from & 255) + ((to & 255) - (from & 255)) * t);
        return 0xFF000000 | r << 16 | g << 8 | b;
    }
}
