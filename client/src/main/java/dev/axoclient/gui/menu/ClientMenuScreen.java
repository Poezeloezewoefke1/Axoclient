package dev.axoclient.gui.menu;

import dev.axoclient.AxoClient;
import dev.axoclient.gui.render.MenuBackground;
import dev.axoclient.gui.theme.Palette;
import dev.axoclient.gui.theme.Themes;
import dev.axoclient.input.Keybinds;
import java.util.List;
import java.util.Locale;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

/**
 * The Axo client menu, drawn to the "Axo Client GUI" design.
 *
 * Everything is laid out on the design's own 1280x720 canvas — the 1120x620
 * window at (80, 50), the 230px sidebar, 44px nav rows and so on, straight
 * from the design file — and the canvas is scaled to fit the game window.
 * So the menu has the design's proportions at every resolution and GUI
 * scale, instead of being squeezed into Minecraft's GUI pixels.
 */
public final class ClientMenuScreen extends Screen {
    public static final int TAB_MODULES = 0;
    public static final int TAB_COSMETICS = 1;
    public static final int TAB_WAYPOINTS = 2;
    public static final int TAB_THEMES = 3;
    public static final int TAB_SETTINGS = 4;

    static final int CANVAS_W = 1280;
    static final int CANVAS_H = 720;
    /** The window and its parts, in design pixels. */
    static final int WIN_X = 80;
    static final int WIN_Y = 50;
    static final int WIN_W = 1120;
    static final int WIN_H = 620;
    static final int SIDE_W = 230;
    /** Page content area: main's padding is 24 / 26 / 0, under a 58px header and a 16px gap. */
    static final int CONTENT_X = WIN_X + SIDE_W + 1 + 26;
    static final int CONTENT_W = WIN_W - SIDE_W - 1 - 52;
    static final int CONTENT_Y = WIN_Y + 24 + 58 + 16;
    static final int CONTENT_BOTTOM = WIN_Y + WIN_H - 1;

    private static final long OPEN_ANIMATION_MS = 200;
    private static final MenuIcon[] NAV_ICONS = {
        MenuIcon.NAV_MODULES, MenuIcon.NAV_COSMETICS, MenuIcon.NAV_WAYPOINTS, MenuIcon.NAV_THEMES, MenuIcon.NAV_SETTINGS
    };

    private final List<MenuPage> pages;
    private final Hits hits = new Hits();
    private int tab;
    private String query = "";
    private boolean searchFocused;
    private long openedAt;

    // Canvas placement in GUI pixels, refreshed every frame.
    private float scale = 1f;
    private float originX;
    private float originY;

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
        if (index != tab) {
            query = "";
            searchFocused = false;
        }
        tab = index;
        pages.get(tab).shown();
    }

    private float designX(double mouseX) {
        return (float) ((mouseX - originX) / scale);
    }

    private float designY(double mouseY) {
        return (float) ((mouseY - originY) / scale);
    }

    /** Menu size setting: 0.86 / 1.0 / 1.0 of the fit scale, and Large drops the margin around the window. */
    private void layout() {
        float fit = Math.min(width / (float) CANVAS_W, height / (float) CANVAS_H);
        int size = MenuSettings.size();
        if (size == 2) {
            fit = Math.min(width / (float) (WIN_W + 24), height / (float) (WIN_H + 24));
        } else if (size == 0) {
            fit *= 0.86f;
        }
        scale = fit;
        originX = (width - CANVAS_W * scale) / 2f;
        originY = (height - CANVAS_H * scale) / 2f;
        if (MenuSettings.animations()) {
            float t = Math.min(1f, (System.currentTimeMillis() - openedAt) / (float) OPEN_ANIMATION_MS);
            float eased = 1f - (1f - t) * (1f - t) * (1f - t);
            originY += (1f - eased) * 14f * scale;
        }
    }

    // ------------------------------------------------------------------
    // Render
    // ------------------------------------------------------------------

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float delta) {
        if (minecraft != null && minecraft.level == null) {
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
        layout();
        hits.clear();

        // The design's backdrop: the theme's ground colour with a faint 48px
        // grid. Slightly see-through in a world, so the game reads behind it.
        boolean inWorld = minecraft != null && minecraft.level != null;
        int groundAlpha = inWorld ? 0xE0 : 0xC8;
        g.fill(0, 0, width, height, (groundAlpha << 24) | (p.ground() & 0x00FFFFFF));

        g.pose().pushMatrix();
        g.pose().translate(originX, originY);
        g.pose().scale(scale, scale);
        int mx = Math.round(designX(mouseX));
        int my = Math.round(designY(mouseY));

        for (int gx = 0; gx <= CANVAS_W; gx += 48) {
            g.fill(gx, 0, gx + 1, CANVAS_H, 0x12808080);
        }
        for (int gy = 0; gy <= CANVAS_H; gy += 48) {
            g.fill(0, gy, CANVAS_W, gy + 1, 0x12808080);
        }

        // Window: box-shadow 0 30px 80px rgba(0,0,0,.45), radius 18, 1px border.
        Ui.shadow(g, WIN_X, WIN_Y, WIN_W, WIN_H, 18, 40, 30, 0x73);
        Ui.box(g, WIN_X, WIN_Y, WIN_W, WIN_H, 18, p.panel(), p.border());

        renderSidebar(g, p, mx, my);
        renderHeader(g, p);
        pages.get(tab).render(g, p, hits, CONTENT_X, CONTENT_Y, CONTENT_W, CONTENT_BOTTOM - CONTENT_Y, mx, my, query);

        g.pose().popMatrix();
        super.render(g, mouseX, mouseY, delta);
    }

    private void renderSidebar(GuiGraphics g, Palette p, int mx, int my) {
        int x = WIN_X + 1;
        int y = WIN_Y + 1;
        int h = WIN_H - 2;
        // Rounded on the window's side, square where it meets the main area; 1px border-right.
        Ui.round(g, x, y, SIDE_W, h, 17, p.side());
        Ui.rect(g, x + SIDE_W - 18, y, 18, h, p.side());
        Ui.rect(g, x + SIDE_W - 1, y, 1, h, p.border());

        int cx = x + 14;
        int cw = SIDE_W - 1 - 28;

        // Logo: 34px tile, wordmark, release line.
        int lx = cx + 8;
        int ly = y + 22;
        Ui.round(g, lx, ly, 34, 34, 10, p.accentSoft());
        Ui.icon(g, MenuIcon.LOGO, lx + 8, ly + 8, 18, p.accent());
        int tx = lx + 34 + 10;
        int baselineTop = ly - 2;
        Ui.text(g, Ui.Type.S17, "Axo", tx, baselineTop + 2, p.text());
        int axoW = Ui.width(Ui.Type.S17, "Axo ");
        Ui.text(g, Ui.Type.SERIF19, "Client", tx + axoW, baselineTop, p.accent());
        Ui.spaced(g, Ui.Type.R10, "BETA RELEASE " + version(), tx, ly + 31, p.muted(), 0.08f);

        // MENU label, then the five nav buttons (44px, 6px apart).
        int labelY = ly + 34 + 18 + 6;
        Ui.spaced(g, Ui.Type.R10, "MENU", cx + 10, labelY + 4 + 6, p.muted(), 0.1f);
        int navY = labelY + 4 + 12 + 6 + 6;
        for (int i = 0; i < pages.size(); i++) {
            MenuPage page = pages.get(i);
            boolean on = i == tab;
            int ry = navY + i * 50;
            boolean hover = Hits.over(mx, my, cx, ry, cw, 44);
            if (on) {
                Ui.round(g, cx, ry, cw, 44, 10, p.accentSoft());
            } else if (hover) {
                Ui.round(g, cx, ry, cw, 44, 10, 0x29808080);
            }
            Ui.icon(g, NAV_ICONS[i], cx + 12, ry + 13, 18, on ? p.accent() : p.sub());
            Ui.mid(g, Ui.Type.M14, page.navLabel(), cx + 12 + 18 + 12, ry + 22, on ? p.text() : p.sub());
            String count = page.navCount();
            if (!count.isEmpty()) {
                Ui.midRight(g, Ui.Type.R11, count, cx + cw - 12, ry + 22, p.muted());
            }
            int index = i;
            hits.add(cx, ry, cw, 44, () -> switchTo(index));
        }

        // Close hint at the bottom, the player card above it.
        int bottom = y + h - 18;
        String keyName = Keybinds.keyName(MenuSettings.menuKey()).toUpperCase(Locale.ROOT);
        int hintMid = bottom - 7;
        int w1 = Ui.width(Ui.Type.R11, "Press ");
        int w2 = Ui.width(Ui.Type.S11, keyName);
        int w3 = Ui.width(Ui.Type.R11, " to close");
        int hx = cx + (cw - (w1 + w2 + w3)) / 2;
        Ui.mid(g, Ui.Type.R11, "Press ", hx, hintMid, p.muted());
        Ui.mid(g, Ui.Type.S11, keyName, hx + w1, hintMid, p.sub());
        Ui.mid(g, Ui.Type.R11, " to close", hx + w1 + w2, hintMid, p.muted());

        int cardH = 58;
        int cardY = bottom - 14 - 8 - 6 - cardH;
        Ui.box(g, cx, cardY, cw, cardH, 12, p.card(), p.border());
        String name = minecraft != null ? minecraft.getUser().getName() : "Player";
        Ui.round(g, cx + 13, cardY + 13, 32, 32, 8, p.accentSoft());
        Ui.midCentered(g, Ui.Type.S13, name.isEmpty() ? "?" : name.substring(0, 1).toUpperCase(Locale.ROOT),
            cx + 13 + 16, cardY + 29, p.accent());
        Ui.text(g, Ui.Type.M13, Ui.trim(Ui.Type.M13, name, cw - 70), cx + 13 + 32 + 10, cardY + 13, p.text());
        Ui.text(g, Ui.Type.R11, statusLine(), cx + 13 + 32 + 10, cardY + 31, p.muted());
    }

    private String statusLine() {
        if (minecraft == null || minecraft.level == null) {
            return "Main menu · 1.21.11";
        }
        ServerData server = minecraft.getCurrentServer();
        return (server != null ? "Online" : "Singleplayer") + " · 1.21.11";
    }

    private static String version() {
        return FabricLoader.getInstance().getModContainer(AxoClient.MOD_ID)
            .map(c -> c.getMetadata().getVersion().getFriendlyString())
            .orElse("dev");
    }

    /** h1 (italic serif accent word + semibold rest), the summary line, and the search box. */
    private void renderHeader(GuiGraphics g, Palette p) {
        MenuPage page = pages.get(tab);
        int x = CONTENT_X;
        int top = WIN_Y + 24;
        Ui.text(g, Ui.Type.SERIF30, page.titleAccent(), x, top, p.accent());
        int aw = Ui.width(Ui.Type.SERIF30, page.titleAccent()) + Ui.width(Ui.Type.S26, " ");
        // Same baseline as the serif word: shift by the difference in ascents.
        int restTop = top + Math.round(Ui.Type.SERIF30.ascent() - Ui.Type.S26.ascent());
        Ui.text(g, Ui.Type.S26, page.titleRest(), x + aw, restTop, p.text());
        Ui.text(g, Ui.Type.R12, Ui.trim(Ui.Type.R12, page.subtitle(), CONTENT_W - 260), x, top + 39 + 4, p.sub());

        String placeholder = page.searchPlaceholder();
        if (placeholder == null) {
            return;
        }
        int sw = 240;
        int sh = 38;
        int sx = x + CONTENT_W - sw;
        int sy = top + 58 - sh;
        Ui.box(g, sx, sy, sw, sh, 10, p.raised(), searchFocused ? p.accent() : p.border());
        Ui.icon(g, MenuIcon.SEARCH, sx + 12, sy + 11, 15, p.sub());
        boolean empty = query.isEmpty();
        String shown = empty && !searchFocused ? placeholder : query;
        int textX = sx + 12 + 15 + 8;
        String visible = Ui.trim(Ui.Type.R13, shown, sw - 50);
        Ui.mid(g, Ui.Type.R13, visible, textX, sy + sh / 2, empty && !searchFocused ? p.muted() : p.text());
        if (searchFocused && (System.currentTimeMillis() / 500) % 2 == 0) {
            int caret = textX + Ui.width(Ui.Type.R13, empty ? "" : visible) + 1;
            Ui.rect(g, caret, sy + 11, 1, 16, p.text());
        }
        hits.add(sx, sy, sw, sh, () -> searchFocused = true);
    }

    // ------------------------------------------------------------------
    // Input
    // ------------------------------------------------------------------

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.buttonInfo().button() != GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            return super.mouseClicked(event, doubleClick);
        }
        searchFocused = false;
        if (hits.click(designX(event.x()), designY(event.y()))) {
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        pages.get(tab).drag(designX(event.x()), designY(event.y()));
        return true;
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        pages.get(tab).release();
        return super.mouseReleased(event);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        return pages.get(tab).scroll(designX(mouseX), designY(mouseY), scrollY)
            || super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
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
            }
            // Letters arrive through charTyped; don't let the menu key close the menu mid-word.
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
