package dev.axoclient.gui.menu;

import dev.axoclient.core.ModuleManager;
import dev.axoclient.gui.notify.Notification;
import dev.axoclient.gui.notify.Notifications;
import dev.axoclient.gui.theme.Palette;
import dev.axoclient.modules.qol.WaypointsModule;
import dev.axoclient.util.WorldInfo;
import dev.axoclient.waypoint.Waypoint;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import org.lwjgl.glfw.GLFW;

/**
 * The Waypoints page: every waypoint saved for the world you're in, with its
 * coordinates, dimension and distance. Click a name to rename it; the eye hides
 * or shows it (hidden ones stay saved but the HUD arrow and minimap skip them);
 * copy puts it on the clipboard; the bin deletes it.
 */
final class WaypointsPage implements MenuPage {
    private static final int ROW_H = 30;
    /** Diamond colours, cycled per row, from the design. */
    private static final int[] COLOURS = {0xFF8B7CF6, 0xFF4FBF7A, 0xFFF08A4B, 0xFF3CC3D6, 0xFFEE6E9F};

    private int scroll;
    private int maxScroll;
    private Waypoint renaming;
    private String draft = "";

    private static WaypointsModule module() {
        return ModuleManager.get().byId("waypoints").orElse(null) instanceof WaypointsModule w ? w : null;
    }

    private static boolean active() {
        WaypointsModule module = module();
        return module != null && ModuleManager.get().isEnabled(module);
    }

    private static List<Waypoint> all() {
        WaypointsModule module = module();
        return module == null || !active() ? List.of() : module.pointsInCurrentWorld();
    }

    @Override
    public String navLabel() {
        return "Waypoints";
    }

    @Override
    public String navCount() {
        return Integer.toString(all().size());
    }

    @Override
    public String titleAccent() {
        return "Saved";
    }

    @Override
    public String titleRest() {
        return "Waypoints";
    }

    @Override
    public String subtitle() {
        List<Waypoint> all = all();
        long visible = all.stream().filter(Waypoint::visible).count();
        return all.size() + " waypoints · " + visible + " visible";
    }

    @Override
    public String searchPlaceholder() {
        return "Search waypoints";
    }

    @Override
    public void shown() {
        renaming = null;
    }

    @Override
    public void render(GuiGraphics g, Palette p, Hits hits, int x, int y, int w, int h, int mx, int my, String query) {
        WaypointsModule module = module();
        LocalPlayer player = Minecraft.getInstance().player;
        if (module == null) {
            return;
        }
        if (!active()) {
            Draw.centered(g, "The Waypoints module is off.", x + w / 2, y + 30, p.sub());
            String label = "Turn it on";
            int bw = Draw.width(label) + 20;
            Draw.round(g, x + w / 2 - bw / 2, y + 44, bw, 18, 5, p.accent());
            Draw.boldCentered(g, label, x + w / 2, y + 49, p.onAccent());
            hits.add(x + w / 2 - bw / 2, y + 44, bw, 18, () -> ModuleManager.get().setEnabled(module, true));
            return;
        }
        if (player == null || WorldInfo.worldId() == null) {
            Draw.centered(g, "Join a world to see its waypoints.", x + w / 2, y + 30, p.sub());
            return;
        }

        // Add button and the current position.
        String add = "+  Add waypoint at my position";
        int aw = Draw.boldWidth(add) + 20;
        Draw.round(g, x, y, aw, 18, 5, p.accent());
        Draw.bold(g, add, x + 10, y + 5, p.onAccent());
        hits.add(x, y, aw, 18, () -> {
            Waypoint point = module.markHere();
            if (point != null) {
                renaming = point;
                draft = point.name();
                scroll = Integer.MAX_VALUE;
            }
        });
        String here = String.format(Locale.ROOT, "Current: X %d · Y %d · Z %d",
            (int) Math.floor(player.getX()), (int) Math.floor(player.getY()), (int) Math.floor(player.getZ()));
        Draw.text(g, here, x + w - Draw.width(here), y + 5, p.sub());

        String needle = query.toLowerCase(Locale.ROOT).trim();
        List<Waypoint> rows = new ArrayList<>();
        for (Waypoint point : module.pointsInCurrentWorld()) {
            if (needle.isEmpty() || point.name().toLowerCase(Locale.ROOT).contains(needle)) {
                rows.add(point);
            }
        }

        int top = y + 26;
        int viewH = h - 26;
        if (rows.isEmpty()) {
            Draw.centered(g, needle.isEmpty() ? "No waypoints in this world yet." : "No waypoints match your search.",
                x + w / 2, top + 30, p.sub());
            return;
        }
        int contentH = rows.size() * (ROW_H + 5) - 5;
        maxScroll = Math.max(0, contentH - viewH);
        scroll = Math.max(0, Math.min(scroll, maxScroll));
        String dim = WorldInfo.dimensionId();

        g.enableScissor(x, top, x + w, top + viewH);
        hits.clip(x, top, w, viewH);
        for (int i = 0; i < rows.size(); i++) {
            Waypoint point = rows.get(i);
            int ry = top - scroll + i * (ROW_H + 5);
            row(g, p, hits, module, point, COLOURS[i % COLOURS.length], x, ry, w, dim, player);
        }
        hits.unclip();
        g.disableScissor();
    }

    private void row(
        GuiGraphics g, Palette p, Hits hits, WaypointsModule module, Waypoint point, int colour,
        int x, int y, int w, String dim, LocalPlayer player
    ) {
        boolean visible = point.visible();
        Draw.card(g, x, y, w, ROW_H, 6, p.card(), p.border());
        // A diamond marker, dimmed for hidden waypoints.
        int dc = visible ? colour : ((0x70 << 24) | (colour & 0x00FFFFFF));
        for (int k = -4; k <= 4; k++) {
            int half = 4 - Math.abs(k);
            g.fill(x + 12 - half, y + 15 + k, x + 13 + half, y + 16 + k, dc);
        }

        int textColour = visible ? p.text() : p.muted();
        if (point.equals(renaming)) {
            boolean caret = (System.currentTimeMillis() / 500) % 2 == 0;
            Draw.text(g, draft + (caret ? "_" : ""), x + 24, y + 5, p.accent());
        } else {
            Draw.text(g, Draw.trim(point.name(), w - 230), x + 24, y + 5, textColour);
        }
        String coords = "X " + point.x() + " · Y " + point.y() + " · Z " + point.z();
        Draw.text(g, Draw.trim(coords, w - 230), x + 24, y + 16, p.sub());
        hits.add(x + 20, y, Math.max(40, w - 230), ROW_H, () -> {
            renaming = point;
            draft = point.name();
        });

        // Right side, right to left: delete, copy, eye, distance, dimension badge.
        int rx = x + w - 6;
        rx -= 16;
        iconBin(g, rx + 3, y + 9, p.sub());
        hits.add(rx, y + 5, 16, 20, () -> {
            module.remove(point);
            Notifications.push(point.name() + " deleted", Notification.Type.INFO);
        });
        rx -= 18;
        iconCopy(g, rx + 3, y + 9, p.sub(), p.card());
        hits.add(rx, y + 5, 16, 20, () -> {
            String text = point.name() + ": " + point.x() + " " + point.y() + " " + point.z() + " (" + dimName(point.dim()) + ")";
            Minecraft.getInstance().keyboardHandler.setClipboard(text);
            Notifications.push("Copied " + point.name() + " to the clipboard", Notification.Type.SUCCESS);
        });
        rx -= 18;
        iconEye(g, rx + 2, y + 11, visible ? p.accent() : p.muted(), !visible, p.card());
        hits.add(rx, y + 5, 16, 20, () -> module.setVisible(point, !point.visible()));

        String distance = point.dim().equals(dim)
            ? Waypoint.formatDistance(point.horizontalDistance(player.getX(), player.getZ()))
            : "—";
        rx -= 8;
        Draw.text(g, distance, rx - Draw.width(distance), y + 11, p.sub());
        rx -= 46;

        String badge = dimName(point.dim()).toUpperCase(Locale.ROOT);
        int hue = dimHue(point.dim());
        int bw = Draw.width(badge) + 10;
        Draw.round(g, rx - bw, y + 8, bw, 14, 4, (0x29 << 24) | (hue & 0x00FFFFFF));
        Draw.centered(g, badge, rx - bw / 2, y + 11, p.light() ? darker(hue) : lighter(hue));
    }

    private static String dimName(String dim) {
        return switch (dim) {
            case WorldInfo.OVERWORLD -> "Overworld";
            case WorldInfo.NETHER -> "Nether";
            case WorldInfo.END -> "End";
            default -> "Other";
        };
    }

    private static int dimHue(String dim) {
        return switch (dim) {
            case WorldInfo.NETHER -> 0xFFF0714B;
            case WorldInfo.END -> 0xFFB59CF6;
            default -> 0xFF4FBF7A;
        };
    }

    private static int lighter(int colour) {
        return mix(colour, 0xFFFFFFFF, 0.3f);
    }

    private static int darker(int colour) {
        return mix(colour, 0xFF000000, 0.35f);
    }

    private static int mix(int a, int b, float t) {
        int r = Math.round(((a >> 16) & 0xFF) * (1 - t) + ((b >> 16) & 0xFF) * t);
        int g = Math.round(((a >> 8) & 0xFF) * (1 - t) + ((b >> 8) & 0xFF) * t);
        int bl = Math.round((a & 0xFF) * (1 - t) + (b & 0xFF) * t);
        return 0xFF000000 | (r << 16) | (g << 8) | bl;
    }

    private static void iconBin(GuiGraphics g, int x, int y, int c) {
        g.fill(x, y + 1, x + 10, y + 2, c);
        g.fill(x + 3, y, x + 7, y + 1, c);
        g.fill(x + 1, y + 3, x + 2, y + 11, c);
        g.fill(x + 8, y + 3, x + 9, y + 11, c);
        g.fill(x + 1, y + 10, x + 9, y + 11, c);
        g.fill(x + 4, y + 4, x + 5, y + 9, c);
        g.fill(x + 6, y + 4, x + 7, y + 9, c);
    }

    /** Two overlapping sheets; {@code bg} hollows the front one. */
    private static void iconCopy(GuiGraphics g, int x, int y, int c, int bg) {
        Draw.round(g, x + 3, y, 7, 8, 1, c);
        Draw.round(g, x + 4, y + 1, 5, 6, 1, bg);
        g.fill(x, y + 3, x + 1, y + 11, c);
        g.fill(x, y + 10, x + 7, y + 11, c);
    }

    /** An eye; crossed out when the waypoint is hidden. {@code bg} hollows the pupil ring. */
    private static void iconEye(GuiGraphics g, int x, int y, int c, boolean crossed, int bg) {
        Draw.round(g, x, y, 12, 8, 4, c);
        Draw.round(g, x + 1, y + 1, 10, 6, 3, bg);
        Draw.round(g, x + 4, y + 2, 4, 4, 2, c);
        if (crossed) {
            for (int i = 0; i < 12; i++) {
                g.fill(x + i, y - 2 + i, x + i + 1, y - 1 + i, c);
            }
        }
    }

    // ------------------------------------------------------------------
    // Renaming
    // ------------------------------------------------------------------

    @Override
    public boolean capturingKeys() {
        return renaming != null;
    }

    @Override
    public boolean key(int glfwKey) {
        if (renaming == null) {
            if (glfwKey == GLFW.GLFW_KEY_DOWN || glfwKey == GLFW.GLFW_KEY_UP) {
                return scroll(0, 0, glfwKey == GLFW.GLFW_KEY_DOWN ? -1 : 1);
            }
            return false;
        }
        switch (glfwKey) {
            case GLFW.GLFW_KEY_BACKSPACE -> {
                if (!draft.isEmpty()) {
                    draft = draft.substring(0, draft.length() - 1);
                }
            }
            case GLFW.GLFW_KEY_ENTER, GLFW.GLFW_KEY_KP_ENTER -> commit();
            case GLFW.GLFW_KEY_ESCAPE -> renaming = null;
            default -> {
                // letters arrive through typed()
            }
        }
        return true;
    }

    @Override
    public boolean typed(String text) {
        if (renaming == null) {
            return false;
        }
        if (draft.length() < 32) {
            draft += text;
        }
        return true;
    }

    private void commit() {
        WaypointsModule module = module();
        if (renaming != null && module != null) {
            module.rename(renaming, draft);
        }
        renaming = null;
    }

    @Override
    public boolean scroll(double mx, double my, double amount) {
        scroll = Math.max(0, Math.min(maxScroll, scroll + (int) Math.round(-amount * 24)));
        return true;
    }
}
