package dev.axoclient.gui;

import dev.axoclient.gui.render.GuiRender;
import dev.axoclient.gui.theme.GuiTheme;
import dev.axoclient.gui.theme.Themes;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * The Axo hub screen — reachable from the title (boot) menu and the pause
 * menu. Central launch pad for the ClickGUI, the HUD editor, and the in-game
 * settings, so every Axo feature is one click from any menu. Returns to the
 * screen it was opened from on close.
 */
public final class AxoMenuScreen extends Screen {
    private static final int BTN_W = 220;
    private static final int BTN_H = 20;
    private static final int GAP = 24;

    private final Screen parent;

    public AxoMenuScreen(Screen parent) {
        super(Component.literal("Axo Client"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int cx = this.width / 2 - BTN_W / 2;
        int y = this.height / 4 + 12;

        addRenderableWidget(Button.builder(Component.literal("Modules  (ClickGUI)"),
            b -> this.minecraft.setScreen(new ClickGuiScreen()))
            .bounds(cx, y, BTN_W, BTN_H).build());

        addRenderableWidget(Button.builder(Component.literal("HUD Editor"),
            b -> this.minecraft.setScreen(new HudEditorScreen()))
            .bounds(cx, y + GAP, BTN_W, BTN_H).build());

        addRenderableWidget(Button.builder(Component.literal("Settings"),
            b -> this.minecraft.setScreen(new AxoSettingsScreen(this)))
            .bounds(cx, y + GAP * 2, BTN_W, BTN_H).build());

        addRenderableWidget(Button.builder(Component.literal("Done"),
            b -> this.onClose())
            .bounds(cx, y + GAP * 4, BTN_W, BTN_H).build());
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float delta) {
        GuiTheme theme = Themes.current();
        g.fill(0, 0, this.width, this.height, 0xC00A0A12);
        super.render(g, mouseX, mouseY, delta);
        GuiRender.centered(g, "AXO CLIENT", this.width / 2, this.height / 4 - 18, theme.accent);
        GuiRender.centered(g, "Right Shift opens the ClickGUI in-game",
            this.width / 2, this.height / 4 - 4, theme.textDim);
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(parent);
    }
}
