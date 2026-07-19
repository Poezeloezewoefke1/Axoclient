package dev.axoclient.ui;

import dev.axoclient.core.AxoModule;
import dev.axoclient.core.ModuleManager;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * In-game module toggles (roadmap P1-12). Deliberately plain vanilla
 * widgets for now — a styled screen is a later polish task.
 */
public final class AxoSettingsScreen extends Screen {
    private static final int AXO_BLUE = 0xFF38BDF8;

    public AxoSettingsScreen() {
        super(Component.literal("Axo Client"));
    }

    @Override
    protected void init() {
        int y = 48;
        for (AxoModule module : ModuleManager.get().all()) {
            this.addRenderableWidget(
                Button.builder(label(module), button -> {
                    ModuleManager.get().toggle(module);
                    button.setMessage(label(module));
                }).bounds(this.width / 2 - 100, y, 200, 20).build()
            );
            y += 24;
        }
        this.addRenderableWidget(
            Button.builder(Component.literal("Done"), button -> this.onClose())
                .bounds(this.width / 2 - 100, y + 8, 200, 20)
                .build()
        );
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        super.render(graphics, mouseX, mouseY, delta);
        graphics.drawCenteredString(this.font, this.title, this.width / 2, 20, AXO_BLUE);
    }

    private static Component label(AxoModule module) {
        String state = ModuleManager.get().isEnabled(module) ? "ON" : "OFF";
        return Component.literal(module.displayName() + ": " + state);
    }
}
