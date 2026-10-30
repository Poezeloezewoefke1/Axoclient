package dev.axoclient.gui.menu;

import dev.axoclient.core.ModuleManager;
import dev.axoclient.gui.menu.Ui.Type;
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
 * The Waypoints page: every waypoint saved for the world you're in. Click a
 * name to rename it; the eye hides or shows it (hidden ones stay saved but the
 * HUD arrow and minimap skip them); copy puts it on the clipboard; the bin
 * deletes it.
 */
final class WaypointsPage implements MenuPage {
    private static final int ROW_H = 62;
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
        return all.size() + " waypoints · " + all.stream().filter(Waypoint::visible).count() + " visible";
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
            Ui.midCentered(g, Type.R13, "The Waypoints module is off.", x + w / 2, y + 60, p.sub());
            int bw = Ui.width(Type.B12, "Turn it on") + 32;
            Ui.round(g, x + w / 2 - bw / 2, y + 80, bw, 36, 9, p.accent());
            Ui.midCentered(g, Type.B12, "Turn it on", x + w / 2, y + 98, p.onAccent());
            hits.add(x + w / 2 - bw / 2, y + 80, bw, 36, () -> ModuleManager.get().setEnabled(module, true));
            return;
        }
        if (player == null || WorldInfo.worldId() == null) {
            Ui.midCentered(g, Type.R13, "Join a world to see its waypoints.", x + w / 2, y + 60, p.sub());
            return;
        }

        // "Add waypoint at my position": 36px, padding 0 16, radius 9, plus icon 14 then 8px gap.
        String add = "Add waypoint at my position";
        int aw = 16 + 14 + 8 + Ui.spacedWidth(Type.B12, add, 0.04f) + 16;
        Ui.round(g, x, y, aw, 36, 9, p.accent());
        Ui.icon(g, MenuIcon.PLUS, x + 16, y + 11, 14, p.onAccent());
        Ui.spaced(g, Type.B12, add, x + 16 + 14 + 8, y + 18, p.onAccent(), 0.04f);
        hits.add(x, y, aw, 36, () -> {
            Waypoint point = module.markHere();
            if (point != null) {
                renaming = point;
                draft = point.name();
                scroll = Integer.MAX_VALUE;
            }
        });
        String here = String.format(Locale.ROOT, "Current: X %d · Y %d · Z %d",
            (int) Math.floor(player.getX()), (int) Math.floor(player.getY()), (int) Math.floor(player.getZ()));
        Ui.midRight(g, Type.R12, here, x + w, y + 18, p.sub());

        String needle = query.toLowerCase(Locale.ROOT).trim();
        List<Waypoint> rows = new ArrayList<>();
        for (Waypoint point : module.pointsInCurrentWorld()) {
            if (needle.isEmpty() || point.name().toLowerCase(Locale.ROOT).contains(needle)) {
                rows.add(point);
            }
        }
        int top = y + 36 + 14;
        int viewH = y + h - top;
        if (rows.isEmpty()) {
            Ui.midCentered(g, Type.R13, needle.isEmpty() ? "No waypoints in this world yet." : "No waypoints match your search.",
                x + w / 2, top + 60, p.sub());
            return;
        }
        int contentH = rows.size() * (ROW_H + 8) - 8 + 24;
        maxScroll = Math.max(0, contentH - viewH);
        scroll = Math.max(0, Math.min(scroll, maxScroll));
        String dim = WorldInfo.dimensionId();

        g.enableScissor(x, top, x + w, top + viewH);
        hits.clip(x, top, w, viewH);
        for (int i = 0; i < rows.size(); i++) {
            int ry = top - scroll + i * (ROW_H + 8);
            row(g, p, hits, module, rows.get(i), COLOURS[i % COLOURS.length], x, ry, w - 4, dim, player);
        }
        hits.unclip();
        g.disableScissor();
        ModulesPage.scrollbar(g, x + w, top, viewH, contentH, scroll);
    }

    private void row(GuiGraphics g, Palette p, Hits hits, WaypointsModule module, Waypoint point, int colour,
                     int x, int y, int w, String dim, LocalPlayer player) {
        boolean visible = point.visible();
        int mid = y + ROW_H / 2;
        // opacity 0.55 for hidden rows: dim every colour toward the card.
        int text = visible ? p.text() : blend(p.card(), p.text(), 0.55f);
        int sub = visible ? p.sub() : blend(p.card(), p.sub(), 0.55f);
        Ui.box(g, x, y, w, ROW_H, 12, p.card(), p.border());

        // 12px square rotated 45°, radius 4: a softened diamond, 17px across.
        int dc = visible ? colour : blend(p.card(), colour, 0.55f);
        int dx = x + 16 + 6;
        for (int k = -8; k <= 8; k++) {
            int half = Math.max(0, 8 - Math.abs(k) - (Math.abs(k) > 6 ? 1 : 0));
            if (half > 0) {
                g.fill(dx - half, mid + k, dx + half, mid + k + 1, dc);
            }
        }

        // Right side, right to left: bin, eye, copy, distance (70px), dimension badge.
        int right = x + w - 16;
        int bx = right - 34;
        Ui.icon(g, MenuIcon.TRASH, bx + 9, mid - 8, 16, sub);
        hits.add(bx, mid - 17, 34, 34, () -> {
            module.remove(point);
            Notifications.push(point.name() + " deleted", Notification.Type.INFO);
        });
        int ex = bx - 14 - 34;
        Ui.icon(g, visible ? MenuIcon.EYE : MenuIcon.EYE_OFF, ex + 9, mid - 8, 16, visible ? p.accent() : p.muted());
        hits.add(ex, mid - 17, 34, 34, () -> module.setVisible(point, !point.visible()));
        int cx = ex - 14 - 34;
        Ui.icon(g, MenuIcon.COPY, cx + 9, mid - 8, 16, sub);
        hits.add(cx, mid - 17, 34, 34, () -> {
            String clip = point.name() + ": " + point.x() + " " + point.y() + " " + point.z() + " (" + dimName(point.dim()) + ")";
            Minecraft.getInstance().keyboardHandler.setClipboard(clip);
            Notifications.push("Copied " + point.name() + " to the clipboard", Notification.Type.SUCCESS);
        });
        int distRight = cx - 14;
        String distance = point.dim().equals(dim)
            ? Math.round(point.horizontalDistance(player.getX(), player.getZ())) + "m"
            : "—";
        Ui.midRight(g, Type.R12, distance, distRight, mid, sub);
        int badgeRight = distRight - 70 - 14;
        String badge = dimName(point.dim()).toUpperCase(Locale.ROOT);
        int hue = dimHue(point.dim());
        int bw = Ui.spacedWidth(Type.S10, badge, 0.08f) + 18;
        Ui.round(g, badgeRight - bw, mid - 10, bw, 20, 6, (0x29 << 24) | (hue & 0x00FFFFFF));
        Ui.spaced(g, Type.S10, badge, badgeRight - bw + 9, mid, p.light() ? dimDark(point.dim()) : dimLight(point.dim()), 0.08f);

        // Name (click to rename) and coordinates.
        int textX = x + 16 + 12 + 14;
        int textW = badgeRight - bw - 14 - textX;
        if (point.equals(renaming)) {
            boolean caret = (System.currentTimeMillis() / 500) % 2 == 0;
            Ui.text(g, Type.S14, draft + (caret ? "|" : ""), textX, mid - 18, p.accent());
        } else {
            Ui.text(g, Type.S14, Ui.trim(Type.S14, point.name(), textW), textX, mid - 18, text);
        }
        Ui.text(g, Type.R11, "X " + point.x() + " · Y " + point.y() + " · Z " + point.z(), textX, mid + 2, sub);
        hits.add(textX, y, Math.max(60, textW), ROW_H, () -> {
            renaming = point;
            draft = point.name();
        });
    }

    private static String dimName(String dim) {
        return switch (dim) {
            case WorldInfo.OVERWORLD -> "Overworld";
            case WorldInfo.NETHER -> "Nether";
            case WorldInfo.END -> "End";
            default -> "Other";
        };
    }

    /** The design's AXO_DIMS: hue for the badge fill, light/dark for its text. */
    private static int dimHue(String dim) {
        return switch (dim) {
            case WorldInfo.NETHER -> 0xFFF0714B;
            case WorldInfo.END -> 0xFFB59CF6;
            default -> 0xFF4FBF7A;
        };
    }

    private static int dimLight(String dim) {
        return switch (dim) {
            case WorldInfo.NETHER -> 0xFFF59A7C;
            case WorldInfo.END -> 0xFFC8B6FA;
            default -> 0xFF7FD9A0;
        };
    }

    private static int dimDark(String dim) {
        return switch (dim) {
            case WorldInfo.NETHER -> 0xFFB4441F;
            case WorldInfo.END -> 0xFF6B4FC4;
            default -> 0xFF2E7D4F;
        };
    }

    private static int blend(int a, int b, float t) {
        int r = Math.round(((a >> 16) & 0xFF) * (1 - t) + ((b >> 16) & 0xFF) * t);
        int gr = Math.round(((a >> 8) & 0xFF) * (1 - t) + ((b >> 8) & 0xFF) * t);
        int bl = Math.round((a & 0xFF) * (1 - t) + (b & 0xFF) * t);
        return 0xFF000000 | (r << 16) | (gr << 8) | bl;
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
        scroll = Math.max(0, Math.min(maxScroll, scroll + (int) Math.round(-amount * 40)));
        return true;
    }
}
