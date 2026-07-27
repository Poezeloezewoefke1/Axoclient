package dev.axoclient.modules.qol;

import dev.axoclient.core.AxoModule;
import dev.axoclient.core.ModuleCategory;
import net.minecraft.client.Minecraft;

/**
 * Keeps sprint held while you walk forward, so you never have to hold the
 * sprint key. Only drives the vanilla sprint binding — the server sees an
 * ordinary sprinting player, which is why this is allowed everywhere.
 */
public final class ToggleSprintModule extends AxoModule {

    public ToggleSprintModule() {
        super("toggle_sprint", "Toggle Sprint", ModuleCategory.QOL, false);
    }

    @Override
    public void onTick() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.screen != null) {
            return;
        }
        if (minecraft.options.keyUp.isDown()) {
            minecraft.options.keySprint.setDown(true);
        }
    }

    @Override
    protected void onDisable() {
        // Release our hold so normal sprinting takes over again.
        Minecraft.getInstance().options.keySprint.setDown(false);
    }
}
