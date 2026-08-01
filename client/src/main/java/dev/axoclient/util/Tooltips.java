package dev.axoclient.util;

import dev.axoclient.core.ModuleManager;
import dev.axoclient.modules.qol.DurabilityTooltipModule;
import dev.axoclient.modules.qol.ShulkerTooltipModule;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;

/**
 * Owns the single Fabric tooltip callback that every tooltip module appends
 * through.
 *
 * One registration rather than one per module: the callback fires for every
 * item hover, so the enabled check belongs inside it, and registering the
 * same event several times just multiplies that work.
 */
public final class Tooltips {

    private Tooltips() {
    }

    public static void register() {
        ItemTooltipCallback.EVENT.register((stack, context, type, lines) -> {
            ModuleManager modules = ModuleManager.get();
            modules.byId("durability_tooltip")
                .filter(modules::isEnabled)
                .ifPresent(m -> ((DurabilityTooltipModule) m).appendTo(stack, lines));
            modules.byId("shulker_tooltip")
                .filter(modules::isEnabled)
                .ifPresent(m -> ((ShulkerTooltipModule) m).appendTo(stack, lines));
        });
    }
}
