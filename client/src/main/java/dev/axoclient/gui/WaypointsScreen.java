package dev.axoclient.gui;

import dev.axoclient.gui.render.GuiRender;
import dev.axoclient.gui.theme.GuiTheme;
import dev.axoclient.gui.theme.Themes;
import dev.axoclient.modules.qol.WaypointsModule;
import dev.axoclient.util.WorldInfo;
import dev.axoclient.waypoint.Waypoint;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

/**
 * Every waypoint for the world you're in: click a name to rename it, Point to
 * make the HUD arrow follow it, Delete to drop it, Add here to save where you
 * stand. Waypoints in your current dimension are listed first, since they're
 * the only ones the HUD can point to.
 *
 * Drawn with the same flat helpers as the mod menu so it reads as part of it.
 */
public final class WaypointsScreen extends Screen {
    private static final int PANEL_W = 360;
    private static final int ROW_H = 24;
    private static final int HEADER_H = 34;
    private static final int FOOTER_H = 20;
    private static final int MAX_NAME = 32;

    private final WaypointsModule module;
    private int scroll;
    private Waypoint editing;
    private String draft = "";

    public WaypointsScreen(WaypointsModule module) {
        super(Component.literal("Waypoints"));
        this.module = module;
    }

    // ------------------------------------------------------------------
    // Layout
    // ------------------------------------------------------------------

    private int panelX() {
        return (width - PANEL_W) / 2;
    }

    private int panelY() {
        return 24;
    }

    private int panelH() {
        return height - 48;
    }

    private int visibleRows() {
        return Math.max(1, (panelH() - HEADER_H - FOOTER_H) / ROW_H);
    }

    private int rowY(int visibleIndex) {
        return panelY() + HEADER_H + visibleIndex * ROW_H;
    }

    /** {x, y, w, h} of the Add here button. */
    private int[] addButton() {
        int w = GuiRender.textWidth("+ Add here") + 16;
        return new int[] {panelX() + PANEL_W - w - 10, panelY() + 8, w, 18};
    }

    private int[] deleteButton(int y) {
        return new int[] {panelX() + PANEL_W - 10 - 46, y + 3, 46, 18};
    }

    private int[] pointButton(int y) {
        return new int[] {panelX() + PANEL_W - 10 - 46 - 6 - 42, y + 3, 42, 18};
    }

    private int[] nameArea(int y) {
        return new int[] {panelX() + 14, y + 2, 170, 11};
    }

    /** Current dimension first, then the rest, each in saved order. */
    private List<Waypoint> rows() {
        String dim = WorldInfo.dimensionId();
        List<Waypoint> here = new ArrayList<>();
        List<Waypoint> elsewhere = new ArrayList<>();
        for (Waypoint point : module.pointsInCurrentWorld()) {
            (point.dim().equals(dim) ? here : elsewhere).add(point);
        }
        here.addAll(elsewhere);
        return here;
    }

    // ------------------------------------------------------------------
    // Render
    // ------------------------------------------------------------------

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float delta) {
        GuiTheme theme = Themes.current();
        GuiRender.rect(g, 0, 0, width, height, theme.dark ? 0xC8000000 : 0x80000000);
        int px = panelX();
        int py = panelY();
        GuiRender.panel(g, px, py, PANEL_W, panelH(), theme);
        GuiRender.text(g, "Waypoints", px + 12, py + 13, theme.text);

        int[] add = addButton();
        boolean addHover = GuiRender.inside(mouseX, mouseY, add[0], add[1], add[2], add[3]);
        GuiRender.rect(g, add[0], add[1], add[2], add[3], addHover ? theme.accent : theme.moduleOn);
        GuiRender.roundCorners(g, add[0], add[1], add[2], add[3]);
        GuiRender.centered(g, "+ Add here", add[0] + add[2] / 2, add[1] + 5, 0xFFFFFFFF);

        List<Waypoint> rows = rows();
        scroll = Math.max(0, Math.min(scroll, Math.max(0, rows.size() - visibleRows())));
        if (rows.isEmpty()) {
            GuiRender.centered(g, "No waypoints in this world yet.", px + PANEL_W / 2, py + HEADER_H + 20, theme.textDim);
            GuiRender.centered(g, "Use Add here, or your mark key in game.", px + PANEL_W / 2, py + HEADER_H + 34, theme.textDim);
        }
        Waypoint target = module.target();
        String dim = WorldInfo.dimensionId();
        LocalPlayer player = Minecraft.getInstance().player;
        for (int i = 0; i < visibleRows() && scroll + i < rows.size(); i++) {
            Waypoint point = rows.get(scroll + i);
            int y = rowY(i);
            boolean sameDim = point.dim().equals(dim);
            boolean rowHover = GuiRender.inside(mouseX, mouseY, px + 6, y, PANEL_W - 12, ROW_H - 2);
            GuiRender.rect(g, px + 6, y, PANEL_W - 12, ROW_H - 2, rowHover ? theme.hover : theme.moduleOff);
            if (point.equals(target)) {
                GuiRender.rect(g, px + 6, y, 2, ROW_H - 2, theme.accent);
            }

            if (point.equals(editing)) {
                boolean caret = (System.currentTimeMillis() / 500) % 2 == 0;
                GuiRender.text(g, draft + (caret ? "_" : ""), px + 14, y + 3, theme.accent);
            } else {
                GuiRender.text(g, point.name(), px + 14, y + 3, sameDim ? theme.text : theme.textDim);
            }
            String detail = point.x() + ", " + point.y() + ", " + point.z() + "  " + dimLabel(point.dim());
            if (sameDim && player != null) {
                detail += "  " + Waypoint.formatDistance(point.horizontalDistance(player.getX(), player.getZ()));
            }
            GuiRender.text(g, detail, px + 14, y + 13, theme.textDim);

            if (sameDim) {
                int[] pb = pointButton(y);
                boolean on = point.equals(target);
                boolean hover = GuiRender.inside(mouseX, mouseY, pb[0], pb[1], pb[2], pb[3]);
                GuiRender.rect(g, pb[0], pb[1], pb[2], pb[3], on ? theme.accent : hover ? theme.hover : theme.panel);
                GuiRender.centered(g, on ? "Active" : "Point", pb[0] + pb[2] / 2, pb[1] + 5, on ? 0xFFFFFFFF : theme.text);
            }
            int[] db = deleteButton(y);
            boolean delHover = GuiRender.inside(mouseX, mouseY, db[0], db[1], db[2], db[3]);
            GuiRender.rect(g, db[0], db[1], db[2], db[3], delHover ? 0xFFDC2626 : theme.panel);
            GuiRender.centered(g, "Delete", db[0] + db[2] / 2, db[1] + 5, delHover ? 0xFFFFFFFF : theme.textDim);
        }

        String footer = rows.size() > visibleRows()
            ? "Click a name to rename  ·  Up/Down to scroll  ·  Esc to close"
            : "Click a name to rename  ·  Esc to close";
        GuiRender.centered(g, footer, px + PANEL_W / 2, py + panelH() - FOOTER_H + 4, theme.textDim);
        super.render(g, mouseX, mouseY, delta);
    }

    private static String dimLabel(String dim) {
        return switch (dim) {
            case WorldInfo.OVERWORLD -> "Overworld";
            case WorldInfo.NETHER -> "Nether";
            case WorldInfo.END -> "End";
            default -> "Other";
        };
    }

    // ------------------------------------------------------------------
    // Input
    // ------------------------------------------------------------------

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        double mx = event.x();
        double my = event.y();
        if (event.buttonInfo().button() != GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            return super.mouseClicked(event, doubleClick);
        }
        commitEdit();

        int[] add = addButton();
        if (GuiRender.inside(mx, my, add[0], add[1], add[2], add[3])) {
            Waypoint point = module.markHere();
            if (point != null) {
                // Jump straight into naming it, which is usually the next thing you want.
                editing = point;
                draft = point.name();
                scroll = Integer.MAX_VALUE;
            }
            return true;
        }

        List<Waypoint> rows = rows();
        String dim = WorldInfo.dimensionId();
        for (int i = 0; i < visibleRows() && scroll + i < rows.size(); i++) {
            Waypoint point = rows.get(scroll + i);
            int y = rowY(i);
            int[] db = deleteButton(y);
            if (GuiRender.inside(mx, my, db[0], db[1], db[2], db[3])) {
                module.remove(point);
                return true;
            }
            int[] pb = pointButton(y);
            if (point.dim().equals(dim) && GuiRender.inside(mx, my, pb[0], pb[1], pb[2], pb[3])) {
                module.pointTo(point);
                return true;
            }
            int[] na = nameArea(y);
            if (GuiRender.inside(mx, my, na[0], na[1], na[2], na[3])) {
                editing = point;
                draft = point.name();
                return true;
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        int key = event.key();
        if (editing != null) {
            switch (key) {
                case GLFW.GLFW_KEY_BACKSPACE -> {
                    if (!draft.isEmpty()) {
                        draft = draft.substring(0, draft.length() - 1);
                    }
                }
                case GLFW.GLFW_KEY_ENTER, GLFW.GLFW_KEY_KP_ENTER -> commitEdit();
                case GLFW.GLFW_KEY_ESCAPE -> editing = null;
                default -> {
                    // other keys are typed through charTyped
                }
            }
            return true;
        }
        if (key == GLFW.GLFW_KEY_DOWN) {
            scroll++;
            return true;
        }
        if (key == GLFW.GLFW_KEY_UP) {
            scroll = Math.max(0, scroll - 1);
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean charTyped(CharacterEvent event) {
        if (editing != null && event.codepoint() >= ' ' && draft.length() < MAX_NAME) {
            draft += event.codepointAsString();
            return true;
        }
        return super.charTyped(event);
    }

    private void commitEdit() {
        if (editing != null) {
            module.rename(editing, draft);
            editing = null;
        }
    }

    @Override
    public void onClose() {
        commitEdit();
        super.onClose();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
