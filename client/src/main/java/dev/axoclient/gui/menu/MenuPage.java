package dev.axoclient.gui.menu;

import dev.axoclient.gui.theme.Palette;
import net.minecraft.client.gui.GuiGraphics;

/**
 * One page of the client menu (Modules, Cosmetics, ...). The frame draws the
 * sidebar and header; a page owns everything below the header.
 */
public interface MenuPage {
    /** Sidebar label and the count shown beside it ("" for none). */
    String navLabel();

    String navCount();

    /** Title as two parts: the first is drawn in the accent, italic. */
    String titleAccent();

    String titleRest();

    String subtitle();

    /** Placeholder for the header search box, or null for no search box. */
    default String searchPlaceholder() {
        return null;
    }

    void render(GuiGraphics g, Palette p, Hits hits, int x, int y, int w, int h, int mx, int my, String query);

    /** Mouse drag with the left button held (sliders, the player preview). */
    default void drag(double mx, double my) {
    }

    default void release() {
    }

    default boolean scroll(double mx, double my, double amount) {
        return false;
    }

    /** True while the page wants every key itself (renaming, binding a key). */
    default boolean capturingKeys() {
        return false;
    }

    default boolean key(int glfwKey) {
        return false;
    }

    default boolean typed(String text) {
        return false;
    }

    /** Called when the page is shown, e.g. to leave a sub-view. */
    default void shown() {
    }
}
