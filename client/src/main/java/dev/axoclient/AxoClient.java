package dev.axoclient;

import dev.axoclient.core.AxoConfig;
import dev.axoclient.core.ModuleManager;
import dev.axoclient.modules.hud.CoordinatesModule;
import dev.axoclient.modules.hud.FpsHudModule;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Client entrypoint (declared in fabric.mod.json). Boot order matters:
 * config first, then module registration so persisted enabled-states apply.
 */
public final class AxoClient implements ClientModInitializer {
    public static final String MOD_ID = "axoclient";
    public static final Logger LOGGER = LoggerFactory.getLogger("AxoClient");

    @Override
    public void onInitializeClient() {
        AxoConfig config = AxoConfig.load();
        ModuleManager modules = ModuleManager.init(config);

        modules.register(new FpsHudModule());
        modules.register(new CoordinatesModule());
        // New modules register here and nowhere else (see docs/architecture.md).

        ClientTickEvents.END_CLIENT_TICK.register(client -> modules.tickAll());

        LOGGER.info("Axo Client initialized with {} module(s)", modules.all().size());
    }
}
