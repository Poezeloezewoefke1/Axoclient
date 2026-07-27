package dev.axoclient;

import dev.axoclient.core.AxoConfig;
import dev.axoclient.core.ModuleManager;
import dev.axoclient.modules.hud.CoordinatesModule;
import dev.axoclient.modules.hud.CpsCounterModule;
import dev.axoclient.modules.hud.FpsHudModule;
import dev.axoclient.modules.cosmetic.CapeModule;
import dev.axoclient.modules.cosmetic.HeartTrailModule;
import dev.axoclient.modules.cosmetic.ParticleCosmeticModule;
import dev.axoclient.modules.cosmetic.ParticleTrailModule;
import dev.axoclient.modules.hud.ArmorHudModule;
import dev.axoclient.modules.hud.ClockHudModule;
import dev.axoclient.modules.hud.CompassHudModule;
import dev.axoclient.modules.hud.DayCounterHudModule;
import dev.axoclient.modules.hud.DirectionHudModule;
import dev.axoclient.modules.hud.ExperienceHudModule;
import dev.axoclient.modules.hud.HealthHudModule;
import dev.axoclient.modules.hud.HungerHudModule;
import dev.axoclient.modules.hud.MemoryHudModule;
import dev.axoclient.modules.hud.ModuleListHudModule;
import dev.axoclient.modules.hud.DeathCoordsModule;
import dev.axoclient.modules.hud.DurabilityWarningModule;
import dev.axoclient.modules.hud.PingHudModule;
import dev.axoclient.modules.hud.PingSpikeModule;
import dev.axoclient.modules.hud.PotionEffectsHudModule;
import dev.axoclient.modules.hud.ServerInfoHudModule;
import dev.axoclient.modules.hud.SessionUptimeHudModule;
import dev.axoclient.modules.hud.SpeedHudModule;
import dev.axoclient.modules.hud.TimeHudModule;
import dev.axoclient.modules.hud.TpsHudModule;
import dev.axoclient.modules.hud.XpProgressHudModule;
import dev.axoclient.modules.pvp.AttackCooldownModule;
import dev.axoclient.modules.pvp.KeystrokesModule;
import dev.axoclient.modules.pvp.TargetHudModule;
import dev.axoclient.modules.qol.FullbrightModule;
import dev.axoclient.modules.qol.ToggleSprintModule;
import dev.axoclient.modules.qol.UnfocusedFpsModule;
import dev.axoclient.modules.qol.ZoomModule;
import dev.axoclient.gui.ClickGuiScreen;
import dev.axoclient.input.Keybinds;
import dev.axoclient.update.UpdateChecker;
import dev.axoclient.util.Keys;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.core.particles.ParticleTypes;
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
        modules.register(new MemoryHudModule());
        modules.register(new SpeedHudModule());
        modules.register(new PingHudModule());
        modules.register(new DayCounterHudModule());
        modules.register(new SessionUptimeHudModule());
        modules.register(new HealthHudModule());
        modules.register(new HungerHudModule());
        modules.register(new ExperienceHudModule());
        modules.register(new DirectionHudModule());
        modules.register(new ClockHudModule());
        modules.register(new ArmorHudModule());
        modules.register(new PotionEffectsHudModule());
        modules.register(new XpProgressHudModule());
        modules.register(new ServerInfoHudModule());
        modules.register(new PingSpikeModule());
        modules.register(new DurabilityWarningModule());
        modules.register(new DeathCoordsModule());
        modules.register(new TpsHudModule());
        modules.register(new CompassHudModule());
        modules.register(new ParticleTrailModule());
        modules.register(new HeartTrailModule());
        // Particle cosmetics (client-side, local-only) — all share one class.
        modules.register(new ParticleCosmeticModule("flame_trail", "Flame Trail", ParticleTypes.FLAME, 1, 0.1));
        modules.register(new ParticleCosmeticModule("soul_trail", "Soul Trail", ParticleTypes.SOUL_FIRE_FLAME, 1, 0.1));
        modules.register(new ParticleCosmeticModule("smoke_trail", "Smoke Trail", ParticleTypes.SMOKE, 1, 0.1));
        modules.register(new ParticleCosmeticModule("cloud_trail", "Cloud Trail", ParticleTypes.CLOUD, 2, 0.1));
        modules.register(new ParticleCosmeticModule("crit_aura", "Crit Aura", ParticleTypes.CRIT, 1, 1.0));
        modules.register(new ParticleCosmeticModule("portal_trail", "Portal Trail", ParticleTypes.PORTAL, 1, 0.5));
        modules.register(new ParticleCosmeticModule("witch_trail", "Witch Trail", ParticleTypes.WITCH, 1, 0.3));
        modules.register(new ParticleCosmeticModule("happy_aura", "Happy Aura", ParticleTypes.HAPPY_VILLAGER, 2, 1.0));
        modules.register(new ParticleCosmeticModule("note_aura", "Note Aura", ParticleTypes.NOTE, 3, 1.2));
        modules.register(new ParticleCosmeticModule("splash_trail", "Splash Trail", ParticleTypes.SPLASH, 1, 0.1));
        modules.register(new ParticleCosmeticModule("lava_trail", "Lava Trail", ParticleTypes.LAVA, 2, 0.1));
        modules.register(new ParticleCosmeticModule("angry_aura", "Angry Aura", ParticleTypes.ANGRY_VILLAGER, 4, 1.5));
        // Custom capes (client-side, local-only) — bundled textures.
        modules.register(new CapeModule("cape_blue", "Blue Cape", "textures/capes/blue.png"));
        modules.register(new CapeModule("cape_red", "Red Cape", "textures/capes/red.png"));
        modules.register(new CapeModule("cape_purple", "Purple Cape", "textures/capes/purple.png"));
        modules.register(new CapeModule("cape_black", "Black Cape", "textures/capes/black.png"));
        modules.register(new KeystrokesModule());
        modules.register(new TargetHudModule());
        modules.register(new AttackCooldownModule());
        modules.register(new FullbrightModule());
        modules.register(new ZoomModule());
        modules.register(new ToggleSprintModule());
        modules.register(new UnfocusedFpsModule());
        // New modules register here and nowhere else (see docs/architecture.md).

        // In-game update check: notifies if the manifest advertises a newer build.
        UpdateChecker.runAsync(config);

        // Apply persisted "enabled" modules only once the client is fully up —
        // options and the render system exist by now, so onEnable() is safe.
        ClientLifecycleEvents.CLIENT_STARTED.register(client -> modules.enableInitial());

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
