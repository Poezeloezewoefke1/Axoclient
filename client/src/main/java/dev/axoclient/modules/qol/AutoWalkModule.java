package dev.axoclient.modules.qol;

import dev.axoclient.core.AxoModule;
import dev.axoclient.core.ModuleCategory;
import net.minecraft.client.Minecraft;

/**
 * Holds walk-forward for you, for long trips across the map or down a
 * tunnel. Bind it a key in the menu and tap to start and stop. Like Toggle
 * Sprint it only drives the vanilla binding, so the server sees a player
 * holding W. Pauses while a screen is open so menus never fight it.
 */
public final class AutoWalkModule extends AxoModule {

    public AutoWalkModule() {
        super("auto_walk", "Auto Walk", ModuleCategory.QOL, false);
    }

    @Override
    public void onTick() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.screen != null) {
            return;
        }
        minecraft.options.keyUp.setDown(true);
    }

    @Override
    protected void onDisable() {
        Minecraft.getInstance().options.keyUp.setDown(false);
    }
}
