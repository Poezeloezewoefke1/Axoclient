package dev.axoclient.gui.theme;

import dev.axoclient.core.AxoConfig;
import dev.axoclient.core.ModuleManager;

/**
 * Current-theme provider. The player picks one of the {@link Palette} presets
 * on the menu's Themes page; it is stored in the "gui" config section, and
 * every Axo screen — the menu, the HUD editor, notifications — derives its
 * colours from it through {@link #current()}.
 */
public final class Themes {
    public static final String SECTION = "gui";

    private Themes() {
    }

    private static AxoConfig config() {
        return ModuleManager.get().config();
    }

    public static Palette palette() {
        return Palette.byId(config().getModuleString(SECTION, "palette", Palette.ALL.get(0).id()));
    }

    public static void setPalette(Palette palette) {
        config().setModuleString(SECTION, "palette", palette.id());
    }

    /**
     * The flat colour set older screens draw with, mapped from the palette:
     * panels and text come across directly, "off" rows use the raised
     * surface, and the outline is the card colour nudged toward the text so
     * it reads as an edge on both dark and light themes.
     */
    public static GuiTheme current() {
        Palette p = palette();
        return new GuiTheme(
            (0xC0 << 24) | (p.ground() & 0x00FFFFFF),
            p.panel(),
            p.side(),
            p.accent(),
            p.text(),
            p.sub(),
            p.accent(),
            p.raised(),
            p.light() ? 0x14000000 : 0x14FFFFFF,
            blend(p.card(), p.text(), 0.14f),
            !p.light()
        );
    }

    private static int blend(int from, int to, float t) {
        int r = Math.round(((from >> 16) & 0xFF) + (((to >> 16) & 0xFF) - ((from >> 16) & 0xFF)) * t);
        int g = Math.round(((from >> 8) & 0xFF) + (((to >> 8) & 0xFF) - ((from >> 8) & 0xFF)) * t);
        int b = Math.round((from & 0xFF) + ((to & 0xFF) - (from & 0xFF)) * t);
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }
}
