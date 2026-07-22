package dev.axoclient.gui;

import dev.axoclient.core.AxoConfig;
import dev.axoclient.core.ModuleManager;
import dev.axoclient.gui.render.GuiRender;
import dev.axoclient.gui.theme.Themes;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * In-game settings: theme + accent, and the in-game update behaviour. Every
 * control writes straight through to {@code config/axoclient.json} via the
 * same keys the rest of the client reads, so changes apply immediately and
 * persist. Simple toggle buttons that relabel themselves on click.
 */
public final class AxoSettingsScreen extends Screen {
    private static final int BTN_W = 240;
    private static final int BTN_H = 20;
    private static final int GAP = 24;

    private final Screen parent;

    public AxoSettingsScreen(Screen parent) {
        super(Component.literal("Axo Settings"));
        this.parent = parent;
    }

    private static AxoConfig config() {
        return ModuleManager.get().config();
    }

    @Override
    protected void init() {
        int cx = this.width / 2 - BTN_W / 2;
        int y = this.height / 4 + 8;

        Button theme = Button.builder(themeLabel(), b -> {
            Themes.setDark(!Themes.isDark());
            b.setMessage(themeLabel());
        }).bounds(cx, y, BTN_W, BTN_H).build();

        Button accent = Button.builder(accentLabel(), b -> {
            cycleAccent();
            b.setMessage(accentLabel());
        }).bounds(cx, y + GAP, BTN_W, BTN_H).build();

        Button check = Button.builder(boolLabel("Update check", "update", "check", true), b -> {
            toggle("update", "check", true);
            b.setMessage(boolLabel("Update check", "update", "check", true));
        }).bounds(cx, y + GAP * 2, BTN_W, BTN_H).build();

        Button download = Button.builder(boolLabel("Auto-download updates", "update", "download", true), b -> {
            toggle("update", "download", true);
            b.setMessage(boolLabel("Auto-download updates", "update", "download", true));
        }).bounds(cx, y + GAP * 3, BTN_W, BTN_H).build();

        Button notifications = Button.builder(boolLabel("Notifications", "gui", "notifications", true), b -> {
            toggle("gui", "notifications", true);
            b.setMessage(boolLabel("Notifications", "gui", "notifications", true));
        }).bounds(cx, y + GAP * 4, BTN_W, BTN_H).build();

        addRenderableWidget(theme);
        addRenderableWidget(accent);
        addRenderableWidget(check);
        addRenderableWidget(download);
        addRenderableWidget(notifications);
        addRenderableWidget(Button.builder(Component.literal("Done"), b -> this.onClose())
            .bounds(cx, y + GAP * 6, BTN_W, BTN_H).build());
    }

    private static Component themeLabel() {
        return Component.literal("Theme: " + (Themes.isDark() ? "Dark" : "Light"));
    }

    private static Component accentLabel() {
        int accent = Themes.accent();
        int index = 0;
        for (int i = 0; i < Themes.ACCENT_SWATCHES.length; i++) {
            if (Themes.ACCENT_SWATCHES[i] == accent) {
                index = i;
                break;
            }
        }
        return Component.literal("Accent: " + (index + 1) + " / " + Themes.ACCENT_SWATCHES.length);
    }

    private static void cycleAccent() {
        int accent = Themes.accent();
        int index = 0;
        for (int i = 0; i < Themes.ACCENT_SWATCHES.length; i++) {
            if (Themes.ACCENT_SWATCHES[i] == accent) {
                index = i;
                break;
            }
        }
        Themes.setAccent(Themes.ACCENT_SWATCHES[(index + 1) % Themes.ACCENT_SWATCHES.length]);
    }

    private static Component boolLabel(String name, String section, String key, boolean def) {
        return Component.literal(name + ": " + (config().getModuleBool(section, key, def) ? "ON" : "OFF"));
    }

    private static void toggle(String section, String key, boolean def) {
        config().setModuleBool(section, key, !config().getModuleBool(section, key, def));
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float delta) {
        g.fill(0, 0, this.width, this.height, 0xC00A0A12);
        super.render(g, mouseX, mouseY, delta);
        GuiRender.centered(g, "Settings", this.width / 2, this.height / 4 - 12, Themes.current().accent);
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(parent);
    }
}
