package dev.axoclient.gui.theme;

import java.util.List;

/**
 * One of the menu's colour themes: the full set of surfaces the client menu
 * is painted with, from the window backdrop down to muted helper text.
 *
 * The fifteen presets and their colours come straight from the Axo Client GUI
 * design (twelve dark, three light). Pure data with no Minecraft types, so the
 * table is unit-tested.
 */
public record Palette(
    String id,
    String name,
    int accent,
    int ground,
    int panel,
    int side,
    int card,
    int raised,
    int text,
    int sub,
    int muted,
    boolean light
) {
    public static final List<Palette> ALL = List.of(
        of("midnight", "Midnight", 0x8B7CF6, 0x05060B, 0x0A0C18, 0x0D1020, 0x111527, 0x181D35, 0xE8EAF3, 0xA3A9C4, 0x858CAB, false),
        of("ocean", "Deep Ocean", 0x5B9BFF, 0x030612, 0x07102A, 0x0A1535, 0x0E1B42, 0x152452, 0xE3ECFF, 0xA0B3DA, 0x8599C2, false),
        of("aqua", "Aqua", 0x3CC3D6, 0x03090B, 0x071216, 0x0A181D, 0x0D1F25, 0x142B32, 0xE2F3F5, 0x98BBC1, 0x7FA5AB, false),
        of("forest", "Forest", 0x4FBF7A, 0x040906, 0x08120C, 0x0B1810, 0x0F2016, 0x162B1E, 0xE3F2E8, 0x9DBEA8, 0x84A790, false),
        of("ember", "Ember", 0xF08A4B, 0x0B0605, 0x140C09, 0x1A100C, 0x21140F, 0x2C1C15, 0xF5E9E2, 0xC8AC9C, 0xAB9081, false),
        of("nether", "Nether", 0xFF7A3D, 0x0C0303, 0x170606, 0x1F0909, 0x290C0C, 0x361212, 0xFBE6DD, 0xCDA092, 0xB2867A, false),
        of("crimson", "Crimson", 0xEF5357, 0x0A0405, 0x14080A, 0x1B0B0E, 0x240F13, 0x30161B, 0xF7E4E5, 0xC7A0A3, 0xAE878A, false),
        of("rose", "Rose", 0xEE6E9F, 0x0A0508, 0x140A10, 0x1A0D15, 0x22111B, 0x2D1825, 0xF6E6EE, 0xC6A3B5, 0xAC8A9B, false),
        of("gold", "Royal Gold", 0xE8B64C, 0x0A0804, 0x141009, 0x1A150B, 0x221B0F, 0x2D2415, 0xF6EFDF, 0xC6B58F, 0xAA9978, false),
        of("end", "The End", 0xD8E37A, 0x06050C, 0x0E0B1A, 0x130F23, 0x1A142E, 0x231C3C, 0xEEEAF8, 0xADA6C8, 0x928AAF, false),
        of("mono", "Mono", 0xD4D6DE, 0x070708, 0x0E0E10, 0x131316, 0x19191D, 0x232328, 0xEDEDF0, 0xA8AAB3, 0x8E909A, false),
        of("mint", "Mint", 0x5EEAD4, 0x040A0A, 0x091413, 0x0C1A19, 0x102221, 0x172D2B, 0xE2F5F2, 0x9CC0BA, 0x83A8A2, false),
        of("snow", "Snow", 0x5B4FD6, 0xD7DCE6, 0xF6F7FA, 0xEDEFF4, 0xFFFFFF, 0xEBEDF3, 0x151827, 0x4D5470, 0x5A617C, true),
        of("sakura", "Sakura", 0xB8336A, 0xE8D5DC, 0xFBF5F7, 0xF5E9EE, 0xFFFFFF, 0xF4E7EC, 0x2A1520, 0x694858, 0x765564, true),
        of("sand", "Desert", 0xB5651D, 0xE6DCC8, 0xFAF6EE, 0xF2EBDD, 0xFFFFFF, 0xF1E9DA, 0x2B2116, 0x66553F, 0x73614A, true)
    );

    private static Palette of(
        String id, String name, int accent, int ground, int panel, int side, int card, int raised,
        int text, int sub, int muted, boolean light
    ) {
        return new Palette(
            id, name, opaque(accent), opaque(ground), opaque(panel), opaque(side), opaque(card),
            opaque(raised), opaque(text), opaque(sub), opaque(muted), light
        );
    }

    private static int opaque(int rgb) {
        return 0xFF000000 | rgb;
    }

    /** The preset with this id, or the first (Midnight) when unknown. */
    public static Palette byId(String id) {
        for (Palette palette : ALL) {
            if (palette.id.equals(id)) {
                return palette;
            }
        }
        return ALL.get(0);
    }

    /** Text and icons drawn on top of a solid accent fill. */
    public int onAccent() {
        return light ? 0xFFFFFFFF : 0xFF0A0C18;
    }

    /** Hairline borders around cards and panels. */
    public int border() {
        return light ? 0x1A141428 : 0x12FFFFFF;
    }

    /** Faint placeholder bars, as in the theme previews. */
    public int line() {
        return light ? 0x26141428 : 0x24FFFFFF;
    }

    /** The accent at low strength: selected sidebar row, icon tiles, badges. */
    public int accentSoft() {
        int alpha = light ? 0x24 : 0x2E;
        return (alpha << 24) | (accent & 0x00FFFFFF);
    }

    /** Track colour of a switched-off toggle. */
    public int off() {
        return light ? 0x2E141428 : 0x29FFFFFF;
    }
}
