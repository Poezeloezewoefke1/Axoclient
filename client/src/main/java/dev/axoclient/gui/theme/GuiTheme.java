package dev.axoclient.gui.theme;

/**
 * Immutable colour + layout set for the Axo GUI. Colours are packed
 * ARGB ints (0xAARRGGBB). Built from config by {@link Themes} so the
 * accent picker, light/dark toggle, and GUI scale all flow from one place.
 */
public final class GuiTheme {
    public final int background;
    public final int panel;
    public final int panelHeader;
    public final int accent;
    public final int text;
    public final int textDim;
    public final int moduleOn;
    public final int moduleOff;
    public final int hover;
    public final int outline;
    public final boolean dark;

    public GuiTheme(
        int background,
        int panel,
        int panelHeader,
        int accent,
        int text,
        int textDim,
        int moduleOn,
        int moduleOff,
        int hover,
        int outline,
        boolean dark
    ) {
        this.background = background;
        this.panel = panel;
        this.panelHeader = panelHeader;
        this.accent = accent;
        this.text = text;
        this.textDim = textDim;
        this.moduleOn = moduleOn;
        this.moduleOff = moduleOff;
        this.hover = hover;
        this.outline = outline;
        this.dark = dark;
    }

    /** Dark preset tinted by the chosen accent. */
    public static GuiTheme dark(int accent) {
        return new GuiTheme(
            0xC00A0A0F,
            0xF0141420,
            0xFF1A1A28,
            accent,
            0xFFEEF1F6,
            0xFF8B90A0,
            accent,
            0xFF33334A,
            0x22FFFFFF,
            0xFF26263A,
            true
        );
    }

    /** Light preset tinted by the chosen accent. */
    public static GuiTheme light(int accent) {
        return new GuiTheme(
            0xC0E8ECF2,
            0xF0FFFFFF,
            0xFFF0F3F8,
            accent,
            0xFF14161C,
            0xFF5A6072,
            accent,
            0xFFC5CBD8,
            0x18000000,
            0xFFD5DAE6,
            false
        );
    }
}
