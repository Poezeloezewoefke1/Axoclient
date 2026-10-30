package dev.axoclient;

import dev.axoclient.core.AxoConfig;
import dev.axoclient.core.ModuleManager;
import dev.axoclient.modules.cosmetic.CapesModule;
import dev.axoclient.modules.cosmetic.CustomCapeModule;
import dev.axoclient.modules.cosmetic.HaloModule;
import dev.axoclient.modules.cosmetic.TrailsModule;
import dev.axoclient.modules.cosmetic.WingsModule;
import dev.axoclient.modules.hud.ArmorHudModule;
import dev.axoclient.modules.hud.ArmorPointsModule;
import dev.axoclient.modules.hud.BiomeHudModule;
import dev.axoclient.modules.hud.BlockInfoModule;
import dev.axoclient.modules.hud.ChunkInfoModule;
import dev.axoclient.modules.hud.ClockHudModule;
import dev.axoclient.modules.hud.CompassHudModule;
import dev.axoclient.modules.hud.CoordinatesModule;
import dev.axoclient.modules.hud.CpsCounterModule;
import dev.axoclient.modules.hud.CrosshairModule;
import dev.axoclient.modules.hud.DayCounterHudModule;
import dev.axoclient.modules.hud.DayCycleModule;
import dev.axoclient.modules.hud.DeathCoordsModule;
import dev.axoclient.modules.hud.DepthGuideModule;
import dev.axoclient.modules.hud.DirectionHudModule;
import dev.axoclient.modules.hud.DurabilityWarningModule;
import dev.axoclient.modules.hud.ElytraInfoModule;
import dev.axoclient.modules.hud.ExperienceHudModule;
import dev.axoclient.modules.hud.FallWarningModule;
import dev.axoclient.modules.hud.FpsGraphModule;
import dev.axoclient.modules.hud.FpsHudModule;
import dev.axoclient.modules.hud.HealthHudModule;
import dev.axoclient.modules.hud.HostileCounterModule;
import dev.axoclient.modules.hud.HudStyleModule;
import dev.axoclient.modules.hud.HungerHudModule;
import dev.axoclient.modules.hud.HungerWarningModule;
import dev.axoclient.modules.hud.InventoryFullModule;
import dev.axoclient.modules.hud.LightLevelModule;
import dev.axoclient.modules.hud.MemoryHudModule;
import dev.axoclient.modules.hud.MinimapModule;
import dev.axoclient.modules.hud.ModuleListHudModule;
import dev.axoclient.modules.hud.MountInfoModule;
import dev.axoclient.modules.hud.MovementStateModule;
import dev.axoclient.modules.hud.NetherCoordsModule;
import dev.axoclient.modules.hud.PickupLogModule;
import dev.axoclient.modules.hud.PingHudModule;
import dev.axoclient.modules.hud.PingSpikeModule;
import dev.axoclient.modules.hud.PotionEffectsHudModule;
import dev.axoclient.modules.hud.SaturationHudModule;
import dev.axoclient.modules.hud.ServerInfoHudModule;
import dev.axoclient.modules.hud.SessionStatsModule;
import dev.axoclient.modules.hud.SessionUptimeHudModule;
import dev.axoclient.modules.hud.SpeedHudModule;
import dev.axoclient.modules.hud.TimeHudModule;
import dev.axoclient.modules.hud.TpsHudModule;
import dev.axoclient.modules.hud.WeatherModule;
import dev.axoclient.modules.hud.XpProgressHudModule;
import dev.axoclient.modules.performance.PerformanceModeModule;
import dev.axoclient.modules.performance.StutterAlertModule;
import dev.axoclient.modules.pvp.AttackCooldownModule;
import dev.axoclient.modules.pvp.BowChargeModule;
import dev.axoclient.modules.pvp.ComboCounterModule;
import dev.axoclient.modules.pvp.DamageNumbersModule;
import dev.axoclient.modules.pvp.HitSoundModule;
import dev.axoclient.modules.pvp.ItemCounterModule;
import dev.axoclient.modules.pvp.KeystrokesModule;
import dev.axoclient.modules.pvp.KillCounterModule;
import dev.axoclient.modules.pvp.LowHealthWarningModule;
import dev.axoclient.modules.pvp.NearbyPlayersModule;
import dev.axoclient.modules.pvp.PearlCooldownModule;
import dev.axoclient.modules.pvp.ReachDisplayModule;
import dev.axoclient.modules.pvp.TargetHudModule;
import dev.axoclient.modules.pvp.TotemWarningModule;
import dev.axoclient.modules.qol.AutoWalkModule;
import dev.axoclient.modules.qol.BreakReminderModule;
import dev.axoclient.modules.qol.ChatAntiSpamModule;
import dev.axoclient.modules.qol.ChatHistoryModule;
import dev.axoclient.modules.qol.ChatMentionsModule;
import dev.axoclient.modules.qol.ChatTimestampsModule;
import dev.axoclient.modules.qol.ClearLiquidsModule;
import dev.axoclient.modules.qol.CopyCoordsModule;
import dev.axoclient.modules.qol.DurabilityTooltipModule;
import dev.axoclient.modules.qol.EffectExpiryModule;
import dev.axoclient.modules.qol.FreelookModule;
import dev.axoclient.modules.qol.FullbrightModule;
import dev.axoclient.modules.qol.ShulkerTooltipModule;
import dev.axoclient.modules.qol.SleepReminderModule;
import dev.axoclient.modules.qol.StopwatchModule;
import dev.axoclient.modules.qol.ToggleSprintModule;
import dev.axoclient.modules.qol.WaypointsModule;
import dev.axoclient.modules.qol.ZoomModule;
import dev.axoclient.gui.ClickGuiScreen;
import dev.axoclient.input.Keybinds;
import dev.axoclient.update.UpdateChecker;
import dev.axoclient.util.Keys;
import dev.axoclient.util.Tooltips;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
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
        modules.register(new HudStyleModule());
        modules.register(new MinimapModule());
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
        modules.register(new BiomeHudModule());
        modules.register(new PickupLogModule());
        modules.register(new CrosshairModule());
        modules.register(new LightLevelModule());
        modules.register(new NetherCoordsModule());
        modules.register(new SessionStatsModule());
        modules.register(new ChunkInfoModule());
        modules.register(new WeatherModule());
        modules.register(new DayCycleModule());
        modules.register(new BlockInfoModule());
        modules.register(new HostileCounterModule());
        modules.register(new DepthGuideModule());
        modules.register(new FallWarningModule());
        modules.register(new InventoryFullModule());
        modules.register(new SaturationHudModule());
        modules.register(new FpsGraphModule());
        modules.register(new PerformanceModeModule());
        modules.register(new StutterAlertModule());
        modules.register(new ElytraInfoModule());
        modules.register(new MountInfoModule());
        modules.register(new MovementStateModule());
        modules.register(new HungerWarningModule());
        modules.register(new ArmorPointsModule());
        // Cosmetics: one toggle each, with the variant picked in settings.
        modules.register(new TrailsModule());
        modules.register(new HaloModule());
        modules.register(new WingsModule());
        modules.register(new CapesModule());
        modules.register(new CustomCapeModule());
        modules.register(new KeystrokesModule());
        modules.register(new TargetHudModule());
        modules.register(new AttackCooldownModule());
        modules.register(new DamageNumbersModule());
        modules.register(new ComboCounterModule());
        modules.register(new ReachDisplayModule());
        modules.register(new ItemCounterModule());
        modules.register(new LowHealthWarningModule());
        modules.register(new BowChargeModule());
        modules.register(new NearbyPlayersModule());
        modules.register(new HitSoundModule());
        modules.register(new PearlCooldownModule());
        modules.register(new TotemWarningModule());
        modules.register(new KillCounterModule());
        modules.register(new FullbrightModule());
        modules.register(new ZoomModule());
        modules.register(new ToggleSprintModule());
        modules.register(new CopyCoordsModule());
        modules.register(new ChatTimestampsModule());
        modules.register(new ChatAntiSpamModule());
        modules.register(new ChatMentionsModule());
        modules.register(new ChatHistoryModule());
        modules.register(new FreelookModule());
        modules.register(new ClearLiquidsModule());
        modules.register(new DurabilityTooltipModule());
        modules.register(new ShulkerTooltipModule());
        modules.register(new WaypointsModule());
        modules.register(new StopwatchModule());
        modules.register(new AutoWalkModule());
        modules.register(new BreakReminderModule());
        modules.register(new EffectExpiryModule());
        modules.register(new SleepReminderModule());
        // New modules register here and nowhere else (see docs/architecture.md).

        // One tooltip callback shared by every tooltip module.
        Tooltips.register();

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
