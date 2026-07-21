package dev.axoclient.gui.theme;

import dev.axoclient.core.AxoConfig;
import dev.axoclient.core.ModuleManager;

/**
 * Current-theme provider. Reads the accent colour, light/dark choice, and
 * GUI scale from the "gui" config section, so the accent picker and theme
 * toggle in the ClickGUI persist across restarts.
 */
public final class Themes {
    public static final String SECTION = "gui";
    public static final int DEFAULT_ACCENT = 0xFF38BDF8;

    /** A few ready-made accent swatches for the picker. */
    public static final int[] ACCENT_SWATCHES = {
        0xFF38BDF8, // axo blue
        0xFF8B5CF6, // violet
        0xFF4ADE80, // green
        0xFFF472B6, // pink
        0xFFFBBF24, // amber
        0xFFF87171, // red
        0xFF22D3EE, // cyan
        0xFFFFFFFF // white
    };

    private Themes() {
    }

    private static AxoConfig config() {
        return ModuleManager.get().config();
    }

    public static int accent() {
        return config().getModuleInt(SECTION, "accent", DEFAULT_ACCENT);
    }

    public static void setAccent(int argb) {
        config().setModuleInt(SECTION, "accent", argb);
    }

    public static boolean isDark() {
        return config().getModuleBool(SECTION, "dark", true);
    }

    public static void setDark(boolean dark) {
        config().setModuleBool(SECTION, "dark", dark);
    }

    /** GUI scale as a fraction (1.0 = 100%). Clamped to a sane range. */
    public static float scale() {
        int pct = config().getModuleInt(SECTION, "scale_pct", 100);
        pct = Math.max(60, Math.min(160, pct));
        return pct / 100.0f;
    }

    public static void setScalePercent(int pct) {
        config().setModuleInt(SECTION, "scale_pct", Math.max(60, Math.min(160, pct)));
    }

    /** The live theme built from current config. */
    public static GuiTheme current() {
        int accent = accent();
        return isDark() ? GuiTheme.dark(accent) : GuiTheme.light(accent);
    }
}
