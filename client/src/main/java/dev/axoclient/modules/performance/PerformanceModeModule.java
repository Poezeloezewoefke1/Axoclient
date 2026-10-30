package dev.axoclient.modules.performance;

import dev.axoclient.AxoClient;
import dev.axoclient.core.AxoConfig;
import dev.axoclient.core.AxoModule;
import dev.axoclient.core.ModuleCategory;
import dev.axoclient.core.ModuleManager;
import net.minecraft.client.CloudStatus;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.server.level.ParticleStatus;

/**
 * One switch for the video settings that cost the most frames for the least
 * visual difference: clouds off, minimal particles, no entity shadows, no
 * biome blending, and entities drawn a little less far away. Turning it off
 * puts every one of your own settings back exactly.
 *
 * Your originals are written to axoclient.json before anything is changed,
 * and only cleared once they've been restored. If the game crashes while
 * this is on — and Minecraft has saved the lowered values to options.txt in
 * the meantime — the next session still knows what to restore.
 */
public final class PerformanceModeModule extends AxoModule {
    private static final double ENTITY_DISTANCE = 0.75;

    public PerformanceModeModule() {
        super("performance_mode", "Performance Mode", ModuleCategory.PERFORMANCE, false);
    }

    @Override
    protected void onEnable() {
        Options options = Minecraft.getInstance().options;
        AxoConfig config = ModuleManager.get().config();
        if (!config.getModuleBool(id(), "saved", false)) {
            config.setModuleString(id(), "clouds", options.cloudStatus().get().name());
            config.setModuleString(id(), "particles", options.particles().get().name());
            config.setModuleBool(id(), "shadows", options.entityShadows().get());
            config.setModuleInt(id(), "biome_blend", options.biomeBlendRadius().get());
            config.setModuleInt(id(), "entity_distance", (int) Math.round(options.entityDistanceScaling().get() * 100));
            config.setModuleBool(id(), "saved", true);
        }
        options.cloudStatus().set(CloudStatus.OFF);
        options.particles().set(ParticleStatus.MINIMAL);
        options.entityShadows().set(false);
        options.biomeBlendRadius().set(0);
        options.entityDistanceScaling().set(Math.min(ENTITY_DISTANCE, options.entityDistanceScaling().get()));
    }

    @Override
    protected void onDisable() {
        Options options = Minecraft.getInstance().options;
        AxoConfig config = ModuleManager.get().config();
        if (!config.getModuleBool(id(), "saved", false)) {
            return;
        }
        try {
            options.cloudStatus().set(CloudStatus.valueOf(config.getModuleString(id(), "clouds", "FANCY")));
            options.particles().set(ParticleStatus.valueOf(config.getModuleString(id(), "particles", "ALL")));
        } catch (IllegalArgumentException e) {
            AxoClient.LOGGER.warn("Performance Mode: unrecognised saved setting, leaving clouds/particles as they are", e);
        }
        options.entityShadows().set(config.getModuleBool(id(), "shadows", true));
        options.biomeBlendRadius().set(config.getModuleInt(id(), "biome_blend", 2));
        options.entityDistanceScaling().set(config.getModuleInt(id(), "entity_distance", 100) / 100.0);
        config.setModuleBool(id(), "saved", false);
    }
}
