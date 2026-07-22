package dev.axoclient.gui.notify;

import dev.axoclient.gui.render.GuiRender;
import dev.axoclient.gui.theme.GuiTheme;
import dev.axoclient.gui.theme.Themes;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;

/**
 * Animated toast queue, rendered top-right on the HUD. Each toast slides in,
 * holds, then slides out; expired toasts are dropped. Static so any module
 * can raise one with a single call.
 */
public final class Notifications {
    private static final List<Notification> ACTIVE = new ArrayList<>();
    private static final int WIDTH = 150;
    private static final int HEIGHT = 22;
    private static final int MARGIN = 6;
    private static final int GAP = 5;
    private static final long SLIDE_MS = 220;

    private Notifications() {
    }

    public static void push(String message, Notification.Type type) {
        if (!dev.axoclient.core.ModuleManager.get().config().getModuleBool("gui", "notifications", true)) {
            return;
        }
        ACTIVE.add(new Notification(message, type, 2600));
        if (ACTIVE.size() > 6) {
            ACTIVE.remove(0);
        }
    }

    public static void info(String message) {
        push(message, Notification.Type.INFO);
    }

    public static void success(String message) {
        push(message, Notification.Type.SUCCESS);
    }

    public static void render(GuiGraphics g, int screenWidth) {
        ACTIVE.removeIf(Notification::expired);
        if (ACTIVE.isEmpty()) {
            return;
        }
        GuiTheme theme = Themes.current();
        int y = MARGIN;
        for (Notification n : ACTIVE) {
            float slide = slideFactor(n);
            int hidden = (int) ((WIDTH + MARGIN) * (1.0f - slide));
            int x = screenWidth - WIDTH - MARGIN + hidden;

            GuiRender.rect(g, x, y, WIDTH, HEIGHT, theme.panel);
            GuiRender.rect(g, x, y, 3, HEIGHT, accentFor(n.type, theme));
            GuiRender.outline(g, x, y, WIDTH, HEIGHT, theme.outline);
            GuiRender.text(g, trim(n.message), x + 9, y + (HEIGHT - GuiRender.lineHeight()) / 2, theme.text);
            y += HEIGHT + GAP;
        }
    }

    /** 0..1 in/out slide envelope based on the toast's age. */
    private static float slideFactor(Notification n) {
        long age = n.age();
        long remaining = n.durationMs - age;
        if (age < SLIDE_MS) {
            return ease(age / (float) SLIDE_MS);
        }
        if (remaining < SLIDE_MS) {
            return ease(Math.max(0, remaining) / (float) SLIDE_MS);
        }
        return 1.0f;
    }

    private static float ease(float t) {
        t = Math.max(0f, Math.min(1f, t));
        return 1.0f - (1.0f - t) * (1.0f - t); // ease-out quad
    }

    private static int accentFor(Notification.Type type, GuiTheme theme) {
        return switch (type) {
            case SUCCESS -> 0xFF4ADE80;
            case WARN -> 0xFFFBBF24;
            case INFO -> theme.accent;
        };
    }

    private static String trim(String s) {
        int max = WIDTH - 16;
        if (GuiRender.textWidth(s) <= max) {
            return s;
        }
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            if (GuiRender.textWidth(b.toString() + s.charAt(i) + "…") > max) {
                break;
            }
            b.append(s.charAt(i));
        }
        return b + "…";
    }
}
