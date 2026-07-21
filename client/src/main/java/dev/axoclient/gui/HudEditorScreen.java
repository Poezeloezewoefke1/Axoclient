package dev.axoclient.gui;

import dev.axoclient.core.AxoModule;
import dev.axoclient.core.ModuleManager;
import dev.axoclient.gui.render.GuiRender;
import dev.axoclient.gui.theme.GuiTheme;
import dev.axoclient.gui.theme.Themes;
import dev.axoclient.hud.HudModule;
import dev.axoclient.hud.HudPosition;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

/**
 * Drag-to-place HUD editor (roadmap P5-09). Every {@link HudModule} gets a
 * handle that can be dragged anywhere on screen; on release the absolute
 * position is snapped to the nearest of the nine anchors and persisted, so
 * layouts survive resolution changes. Arrow keys nudge the selected handle
 * (Shift = 10px) and R resets it to its default. Disabled modules show
 * dimmed so they can be positioned before being turned on.
 */
public final class HudEditorScreen extends Screen {
    private static final int PAD = 3;

    private final List<Handle> handles = new ArrayList<>();
    private Handle dragging;
    private Handle selected;
    private int grabDX;
    private int grabDY;

    public HudEditorScreen() {
        super(Component.literal("HUD Editor"));
    }

    private static final class Handle {
        final HudModule module;
        int x;
        int y;

        Handle(HudModule module, int x, int y) {
            this.module = module;
            this.x = x;
            this.y = y;
        }
    }

    @Override
    protected void init() {
        handles.clear();
        Font font = font();
        for (AxoModule m : ModuleManager.get().all()) {
            if (!(m instanceof HudModule hud)) {
                continue;
            }
            HudPosition p = hud.currentPosition();
            int w = font.width(hud.editorLabel());
            int x = p.anchor().x(this.width, w, p.offsetX());
            int y = p.anchor().y(this.height, font.lineHeight, p.offsetY());
            handles.add(new Handle(hud, x, y));
        }
    }

    private static Font font() {
        return Minecraft.getInstance().font;
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float delta) {
        GuiTheme theme = Themes.current();
        g.fill(0, 0, this.width, this.height, 0xC00A0A0F);
        drawGuides(g, theme);

        Font font = font();
        for (Handle h : handles) {
            String label = h.module.editorLabel();
            int w = font.width(label);
            int hgt = font.lineHeight;
            boolean on = ModuleManager.get().isEnabled(h.module);
            boolean hover = GuiRender.inside(mouseX, mouseY, h.x - PAD, h.y - PAD, w + PAD * 2, hgt + PAD * 2);
            boolean active = h == selected || h == dragging;

            int fill = active ? theme.hover : (0x66000000);
            GuiRender.rect(g, h.x - PAD, h.y - PAD, w + PAD * 2, hgt + PAD * 2, fill);
            GuiRender.outline(
                g,
                h.x - PAD,
                h.y - PAD,
                w + PAD * 2,
                hgt + PAD * 2,
                active || hover ? theme.accent : theme.outline
            );
            GuiRender.text(g, label, h.x, h.y, on ? theme.text : theme.textDim);
        }

        // Header + hints.
        GuiRender.centered(g, "HUD Editor", this.width / 2, 10, theme.accent);
        GuiRender.centered(
            g,
            "Drag to move  •  snaps to edges  •  arrows nudge (Shift ×10)  •  R reset  •  Esc done",
            this.width / 2,
            this.height - 14,
            theme.textDim
        );
        if (selected != null) {
            HudPosition p = snap(selected);
            String badge = p.anchor().name() + "  " + p.offsetX() + ", " + p.offsetY();
            GuiRender.centered(g, badge, this.width / 2, 22, theme.text);
        }
    }

    /** Faint anchor dots and centre cross so the snap targets are visible. */
    private void drawGuides(GuiGraphics g, GuiTheme theme) {
        int guide = (theme.accent & 0x00FFFFFF) | 0x33000000;
        g.fill(this.width / 2, 0, this.width / 2 + 1, this.height, guide);
        g.fill(0, this.height / 2, this.width, this.height / 2 + 1, guide);
        int[] fracX = { 0, this.width / 2, this.width };
        int[] fracY = { 0, this.height / 2, this.height };
        for (int fx : fracX) {
            for (int fy : fracY) {
                int cx = Math.max(1, Math.min(this.width - 2, fx));
                int cy = Math.max(1, Math.min(this.height - 2, fy));
                g.fill(cx - 1, cy - 1, cx + 2, cy + 2, theme.accent);
            }
        }
    }

    private HudPosition snap(Handle h) {
        Font font = font();
        int w = font.width(h.module.editorLabel());
        return HudPosition.fromAbsolute(h.x, h.y, w, font.lineHeight, this.width, this.height);
    }

    private void commit(Handle h) {
        h.module.moveTo(snap(h));
    }

    private int clampX(int x, int w) {
        return Math.max(0, Math.min(this.width - w, x));
    }

    private int clampY(int y, int hgt) {
        return Math.max(0, Math.min(this.height - hgt, y));
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        int mx = (int) event.x();
        int my = (int) event.y();
        Font font = font();
        // Reverse order so the topmost (last drawn) handle wins overlaps.
        for (int i = handles.size() - 1; i >= 0; i--) {
            Handle h = handles.get(i);
            int w = font.width(h.module.editorLabel());
            if (GuiRender.inside(mx, my, h.x - PAD, h.y - PAD, w + PAD * 2, font.lineHeight + PAD * 2)) {
                dragging = h;
                selected = h;
                grabDX = mx - h.x;
                grabDY = my - h.y;
                return true;
            }
        }
        selected = null;
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        if (dragging != null) {
            Font font = font();
            int w = font.width(dragging.module.editorLabel());
            dragging.x = clampX((int) event.x() - grabDX, w);
            dragging.y = clampY((int) event.y() - grabDY, font.lineHeight);
            return true;
        }
        return super.mouseDragged(event, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (dragging != null) {
            commit(dragging);
            dragging = null;
            return true;
        }
        return super.mouseReleased(event);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        int key = event.key();
        if (selected != null) {
            int step = (event.modifiers() & GLFW.GLFW_MOD_SHIFT) != 0 ? 10 : 1;
            Font font = font();
            int w = font.width(selected.module.editorLabel());
            switch (key) {
                case GLFW.GLFW_KEY_LEFT -> selected.x = clampX(selected.x - step, w);
                case GLFW.GLFW_KEY_RIGHT -> selected.x = clampX(selected.x + step, w);
                case GLFW.GLFW_KEY_UP -> selected.y = clampY(selected.y - step, font.lineHeight);
                case GLFW.GLFW_KEY_DOWN -> selected.y = clampY(selected.y + step, font.lineHeight);
                case GLFW.GLFW_KEY_R -> {
                    selected.module.resetPosition();
                    HudPosition p = selected.module.currentPosition();
                    selected.x = p.anchor().x(this.width, w, p.offsetX());
                    selected.y = p.anchor().y(this.height, font.lineHeight, p.offsetY());
                }
                default -> {
                    return super.keyPressed(event);
                }
            }
            commit(selected);
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
