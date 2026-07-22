package dev.axoclient;

import dev.axoclient.core.AxoConfig;
import dev.axoclient.core.ModuleManager;
import dev.axoclient.modules.hud.CoordinatesModule;
import dev.axoclient.modules.hud.CpsCounterModule;
import dev.axoclient.modules.hud.FpsHudModule;
import dev.axoclient.modules.hud.ModuleListHudModule;
import dev.axoclient.modules.hud.TimeHudModule;
import dev.axoclient.modules.pvp.KeystrokesModule;
import dev.axoclient.modules.qol.FullbrightModule;
import dev.axoclient.modules.qol.ZoomModule;
import dev.axoclient.gui.ClickGuiScreen;
import dev.axoclient.input.Keybinds;
import dev.axoclient.update.UpdateChecker;
import dev.axoclient.util.Keys;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Client entrypoint (declared in fabric.mod.json). Boot order matters:
 * config first, then module registration so persisted enabled-states apply.
 */
public final class AxoClient implements ClientModInitializer {
    public static final String MOD_ID = "axoclient";
    public static final Logger LOGGER = LoggerFactory.getLogger("AxoClient");

    private static boolean settingsKeyWasDown;

    @Override
    public void onInitializeClient() {
        AxoConfig config = AxoConfig.load();
        ModuleManager modules = ModuleManager.init(config);

        modules.register(new FpsHudModule());
        modules.register(new CoordinatesModule());
        modules.register(new CpsCounterModule());
        modules.register(new TimeHudModule());
        modules.register(new ModuleListHudModule());
        modules.register(new KeystrokesModule());
        modules.register(new FullbrightModule());
        modules.register(new ZoomModule());
        // New modules register here and nowhere else (see docs/architecture.md).

        // In-game update check: notifies if the manifest advertises a newer build.
        UpdateChecker.runAsync(config);

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            modules.tickAll();
            Keybinds.tick();

            // Right Shift opens the ClickGUI.
            boolean down = Keys.isDown(GLFW.GLFW_KEY_RIGHT_SHIFT);
            if (down && !settingsKeyWasDown && client.screen == null) {
                client.setScreen(new ClickGuiScreen());
            }
            settingsKeyWasDown = down;
        });

        LOGGER.info("Axo Client initialized with {} module(s)", modules.all().size());
    }
}
