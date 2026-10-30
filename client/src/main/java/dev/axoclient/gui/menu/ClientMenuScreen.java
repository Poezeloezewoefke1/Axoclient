package dev.axoclient.gui.menu;

import dev.axoclient.AxoClient;
import dev.axoclient.gui.render.MenuBackground;
import dev.axoclient.gui.theme.Palette;
import dev.axoclient.gui.theme.Themes;
import dev.axoclient.input.Keybinds;
import java.util.List;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

/**
 * The Axo client menu, after the "Axo Client GUI" design: a sidebar with the
 * logo and five pages (Modules, Cosmetics, Waypoints, Themes, Settings), and a
 * main area with a title, a one-line summary and, on some pages, a search box.
 *
 * The design is laid out for a 1280x720 window. Minecraft GUI space is much
 * smaller (960x540 at GUI scale 2 on a 1080p screen) and its font is fixed,
 * so the window is sized to the screen with caps, and the sidebar and page
 * grids adapt to whatever width they get rather than assuming the design's.
 */
public final class ClientMenuScreen extends Screen {
    public static final int TAB_MODULES = 0;
    public static final int TAB_COSMETICS = 1;
    public static final int TAB_WAYPOINTS = 2;
    public static final int TAB_THEMES = 3;
    public static final int TAB_SETTINGS = 4;

    /** Window caps per Settings › Menu size (small, normal, large), in GUI pixels. */
    private static final int[][] SIZE_CAPS = {{660, 380}, {780, 440}, {920, 520}};
    private static final float[] SIZE_FRACTION = {0.82f, 0.9f, 0.97f};
    private static final long OPEN_ANIMATION_MS = 180;

    private final List<MenuPage> pages;
    private final Hits hits = new Hits();
    private int tab;
    private String query = "";
    private boolean searchFocused;
    private long openedAt;

    public ClientMenuScreen() {
        this(TAB_MODULES);
    }

    public ClientMenuScreen(int tab) {
        super(Component.literal("Axo Client"));
        ModulesPage modules = new ModulesPage();
        this.pages = List.of(
            modules,
            // The Options button on a cosmetic jumps to that module's options page.
            new CosmeticsPage(module -> {
                switchTo(TAB_MODULES);
                modules.openOptions(module);
            }),
            new WaypointsPage(),
            new ThemesPage(),
            new SettingsPage()
        );
        this.tab = Math.max(0, Math.min(pages.size() - 1, tab));
    }

    @Override
    protected void init() {
        openedAt = System.currentTimeMillis();
        pages.get(tab).shown();
    }

    private void switchTo(int index) {
        if (index == tab) {
            pages.get(tab).shown();
            return;
        }
        tab = index;
        query = "";
        searchFocused = false;
        pages.get(tab).shown();
    }

    // ------------------------------------------------------------------
    // Layout
    // ------------------------------------------------------------------

    private int windowW() {
        int size = MenuSettings.size();
        return Math.min(SIZE_CAPS[size][0], Math.round(width * SIZE_FRACTION[size]));
    }

    private int windowH() {
        int size = MenuSettings.size();
        return Math.min(SIZE_CAPS[size][1], Math.round(height * SIZE_FRACTION[size]));
    }

    private int sidebarW(int windowW) {
        return Math.max(108, Math.min(150, windowW / 5));
    }

    /** Slide-in offset while the menu opens, 0 once settled or with animations off. */
    private int openOffset() {
        if (!MenuSettings.animations()) {
            return 0;
        }
        float t = Math.min(1f, (System.currentTimeMillis() - openedAt) / (float) OPEN_ANIMATION_MS);
        float eased = 1f - (1f - t) * (1f - t) * (1f - t);
        return Math.round((1f - eased) * 12);
    }

    // ------------------------------------------------------------------
    // Render
    // ------------------------------------------------------------------

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float delta) {
        if (minecraft != null && minecraft.level == null) {
            // From the title screen there's no world to show: use the menu art.
            MenuBackground.render(g, width, height);
            return;
        }
        if (MenuSettings.blur()) {
            super.renderBackground(g, mouseX, mouseY, delta);
        }
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float delta) {
        Palette p = Themes.palette();
        hits.clear();
        g.fill(0, 0, width, height, (0x70 << 24) | (p.ground() & 0x00FFFFFF));

        int w = windowW();
        int h = windowH();
        int x = (width - w) / 2;
        int y = (height - h) / 2 + openOffset();

        // Soft drop shadow, then the window itself.
        for (int i = 4; i >= 1; i--) {
            Draw.round(g, x - i, y - i + 3, w + i * 2, h + i * 2, 10 + i, (0x10 + (4 - i) * 0x08) << 24);
        }
        Draw.card(g, x, y, w, h, 9, p.panel(), p.border());

        int sideW = sidebarW(w);
        renderSidebar(g, p, x, y, sideW, h, mouseX, mouseY);

        int cx = x + sideW + 14;
        int cw = w - sideW - 28;
        int headerH = renderHeader(g, p, cx, y + 12, cw, mouseX, mouseY);
        int pageY = y + 12 + headerH + 8;
        pages.get(tab).render(g, p, hits, cx, pageY, cw, y + h - 10 - pageY, mouseX, mouseY, query);

        super.render(g, mouseX, mouseY, delta);
    }

    private void renderSidebar(GuiGraphics g, Palette p, int x, int y, int w, int h, int mx, int my) {
        // Side panel with its right-hand border; the window's own rounding is
        // restored by redrawing the left corners in the side colour.
        Draw.round(g, x + 1, y + 1, w, h - 2, 8, p.side());
        g.fill(x + w - 8, y + 1, x + w, y + h - 1, p.side());
        g.fill(x + w, y + 1, x + w + 1, y + h - 1, p.border());

        // Logo tile and wordmark.
        int lx = x + 10;
        int ly = y + 12;
        Draw.round(g, lx, ly, 18, 18, 5, p.accentSoft());
        Draw.logo(g, lx + 9, ly + 9, p.accent());
        Component word = Component.literal("Axo ").withStyle(ChatFormatting.BOLD)
            .append(Component.literal("Client").withStyle(s -> s.withItalic(true).withBold(false).withColor(p.accent() & 0xFFFFFF)));
        g.drawString(Draw.font(), word, lx + 24, ly + 1, p.text(), false);
        Draw.label(g, "Version " + version(), lx + 24, ly + 11, p.muted());

        int navY = ly + 30;
        Draw.label(g, "Menu", x + 12, navY, p.muted());
        navY += 12;
        int rowH = 20;
        for (int i = 0; i < pages.size(); i++) {
            MenuPage page = pages.get(i);
            boolean on = i == tab;
            int ry = navY + i * (rowH + 2);
            boolean hover = Hits.over(mx, my, x + 6, ry, w - 12, rowH);
            if (on || hover) {
                Draw.round(g, x + 6, ry, w - 12, rowH, 5, on ? p.accentSoft() : (p.light() ? 0x10000000 : 0x10FFFFFF));
            }
            int iconColour = on ? p.accent() : p.sub();
            int ix = x + 12;
            int iy = ry + 4;
            switch (i) {
                case TAB_MODULES -> Draw.iconModules(g, ix, iy, iconColour);
                case TAB_COSMETICS -> Draw.iconCosmetics(g, ix, iy, iconColour);
                case TAB_WAYPOINTS -> Draw.iconWaypoints(g, ix, iy, iconColour);
                case TAB_THEMES -> Draw.iconThemes(g, ix, iy, iconColour, p.side());
                default -> Draw.iconSettings(g, ix, iy, iconColour);
            }
            Draw.text(g, page.navLabel(), x + 30, ry + 6, on ? p.text() : p.sub());
            String count = page.navCount();
            if (!count.isEmpty()) {
                Draw.text(g, count, x + w - 10 - Draw.width(count), ry + 6, p.muted());
            }
            int index = i;
            hits.add(x + 6, ry, w - 12, rowH, () -> switchTo(index));
        }

        // Player card and the close hint at the bottom.
        int cardH = 30;
        int cardY = y + h - cardH - 22;
        Draw.card(g, x + 8, cardY, w - 16, cardH, 6, p.card(), p.border());
        String name = minecraft != null ? minecraft.getUser().getName() : "Player";
        Draw.round(g, x + 13, cardY + 6, 18, 18, 4, p.accentSoft());
        Draw.boldCentered(g, name.isEmpty() ? "?" : name.substring(0, 1).toUpperCase(java.util.Locale.ROOT),
            x + 22, cardY + 11, p.accent());
        Draw.text(g, Draw.trim(name, w - 52), x + 36, cardY + 6, p.text());
        Draw.text(g, Draw.trim(statusLine(), w - 52), x + 36, cardY + 17, p.muted());

        String keyName = Keybinds.keyName(MenuSettings.menuKey()).toUpperCase(java.util.Locale.ROOT);
        String hint = "Press " + keyName + " to close";
        Draw.centered(g, Draw.trim(hint, w - 8), x + w / 2, y + h - 14, p.muted());
    }

    private String statusLine() {
        if (minecraft == null || minecraft.level == null) {
            return "Main menu";
        }
        ServerData server = minecraft.getCurrentServer();
        return (server != null ? "Online" : "Singleplayer") + " · 1.21.11";
    }

    private static String version() {
        return FabricLoader.getInstance().getModContainer(AxoClient.MOD_ID)
            .map(c -> c.getMetadata().getVersion().getFriendlyString())
            .orElse("dev");
    }

    /** Title, summary line and search box. Returns the header's height. */
    private int renderHeader(GuiGraphics g, Palette p, int x, int y, int w, int mx, int my) {
        MenuPage page = pages.get(tab);
        Component title = Component.literal(page.titleAccent())
            .withStyle(s -> s.withItalic(true).withColor(p.accent() & 0xFFFFFF))
            .append(Component.literal(" " + page.titleRest()).withStyle(s -> s.withItalic(false).withBold(true).withColor(p.text() & 0xFFFFFF)));
        g.drawString(Draw.font(), title, x, y + 2, p.text(), false);
        Draw.text(g, Draw.trim(page.subtitle(), w - 140), x, y + 15, p.sub());

        String placeholder = page.searchPlaceholder();
        if (placeholder != null) {
            int sw = Math.min(130, w / 3);
            int sx = x + w - sw;
            int sy = y;
            Draw.card(g, sx, sy, sw, 20, 5, p.raised(), searchFocused ? p.accent() : p.border());
            // A small magnifier: ring plus handle.
            Draw.round(g, sx + 7, sy + 6, 7, 7, 3, p.sub());
            Draw.round(g, sx + 8, sy + 7, 5, 5, 2, p.raised());
            g.fill(sx + 13, sy + 12, sx + 15, sy + 14, p.sub());
            boolean empty = query.isEmpty();
            String shown = empty && !searchFocused ? placeholder : query;
            Draw.text(g, Draw.trim(shown, sw - 26), sx + 20, sy + 6, empty && !searchFocused ? p.muted() : p.text());
            if (searchFocused && (System.currentTimeMillis() / 500) % 2 == 0) {
                int caret = sx + 20 + Draw.width(Draw.trim(query, sw - 26));
                g.fill(caret + 1, sy + 5, caret + 2, sy + 15, p.text());
            }
            hits.add(sx, sy, sw, 20, () -> searchFocused = true);
        }
        return 26;
    }

    // ------------------------------------------------------------------
    // Input
    // ------------------------------------------------------------------

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.buttonInfo().button() != GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            return super.mouseClicked(event, doubleClick);
        }
        boolean wasFocused = searchFocused;
        searchFocused = false;
        if (hits.click(event.x(), event.y())) {
            return true;
        }
        searchFocused = false;
        return wasFocused || super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        pages.get(tab).drag(event.x(), event.y());
        return true;
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        pages.get(tab).release();
        return super.mouseReleased(event);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        return pages.get(tab).scroll(mouseX, mouseY, scrollY) || super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        int key = event.key();
        MenuPage page = pages.get(tab);
        if (page.capturingKeys()) {
            page.key(key);
            return true;
        }
        if (searchFocused) {
            if (key == GLFW.GLFW_KEY_BACKSPACE) {
                if (!query.isEmpty()) {
                    query = query.substring(0, query.length() - 1);
                }
                return true;
            }
            if (key == GLFW.GLFW_KEY_ESCAPE || key == GLFW.GLFW_KEY_ENTER) {
                searchFocused = false;
                return true;
            }
            // Other keys type through charTyped; don't let the menu key close the menu mid-word.
            return true;
        }
        if (key == MenuSettings.menuKey()) {
            onClose();
            return true;
        }
        if (page.key(key)) {
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean charTyped(CharacterEvent event) {
        if (event.codepoint() < ' ') {
            return super.charTyped(event);
        }
        if (searchFocused) {
            if (query.length() < 40) {
                query += event.codepointAsString();
            }
            return true;
        }
        if (pages.get(tab).typed(event.codepointAsString())) {
            return true;
        }
        return super.charTyped(event);
    }

    @Override
    public boolean isPauseScreen() {
        return MenuSettings.pauseGame();
    }
}
