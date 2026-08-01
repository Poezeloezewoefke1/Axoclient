package dev.axoclient.modules.cosmetic;

import dev.axoclient.core.AxoModule;
import dev.axoclient.core.ModuleCategory;
import dev.axoclient.core.ModuleManager;
import dev.axoclient.core.ModuleSetting;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;

/**
 * One trail toggle, with the effect chosen in its settings.
 *
 * This replaces twenty-odd near-identical particle modules. They differed
 * only in which particle they emitted, so they belonged behind one Style
 * knob rather than twenty menu rows you could contradictorily enable at once.
 *
 * Local-only: only you see it. Off by default.
 */
public final class TrailsModule extends AxoModule {

    /** One selectable effect. Height is metres above the player's feet. */
    private record Style(String name, ParticleOptions particle, int everyTicks, double height, double spread) {
    }

    private static final List<Style> STYLES = List.of(
        new Style("Flame", ParticleTypes.FLAME, 1, 0.1, 0.4),
        new Style("Soul Fire", ParticleTypes.SOUL_FIRE_FLAME, 1, 0.1, 0.4),
        new Style("Smoke", ParticleTypes.SMOKE, 1, 0.1, 0.4),
        new Style("Cloud", ParticleTypes.CLOUD, 2, 0.1, 0.4),
        new Style("Crit Aura", ParticleTypes.CRIT, 1, 1.0, 0.4),
        new Style("Portal", ParticleTypes.PORTAL, 1, 0.5, 0.4),
        new Style("Witch", ParticleTypes.WITCH, 1, 0.3, 0.4),
        new Style("Happy Aura", ParticleTypes.HAPPY_VILLAGER, 2, 1.0, 0.4),
        new Style("Notes", ParticleTypes.NOTE, 3, 1.2, 0.4),
        new Style("Splash", ParticleTypes.SPLASH, 1, 0.1, 0.4),
        new Style("Lava", ParticleTypes.LAVA, 2, 0.1, 0.4),
        new Style("Angry Aura", ParticleTypes.ANGRY_VILLAGER, 4, 1.5, 0.4),
        new Style("Enchant", ParticleTypes.ENCHANT, 1, 1.2, 0.4),
        new Style("End Rod", ParticleTypes.END_ROD, 1, 0.1, 0.4),
        new Style("Snow", ParticleTypes.SNOWFLAKE, 1, 0.1, 0.4),
        new Style("Soul Aura", ParticleTypes.SOUL, 2, 1.0, 0.4),
        new Style("Sparks", ParticleTypes.ELECTRIC_SPARK, 1, 0.1, 0.4),
        new Style("Glow Aura", ParticleTypes.GLOW, 2, 1.0, 0.4),
        new Style("Cherry", ParticleTypes.CHERRY_LEAVES, 1, 1.4, 0.4),
        new Style("Fireworks", ParticleTypes.FIREWORK, 2, 0.5, 0.4),
        new Style("Hearts", ParticleTypes.HEART, 4, 1.6, 0.3),
        // Themed dust follows the ClickGUI accent, so it recolours with the theme.
        new Style("Themed Dust", null, 1, 0.1, 0.4)
    );

    private static final List<String> NAMES = STYLES.stream().map(Style::name).toList();

    public TrailsModule() {
        super("trails", "Trails", ModuleCategory.COSMETIC, false);
    }

    @Override
    public List<ModuleSetting> settings() {
        return List.of(
            ModuleSetting.choice(id(), "style", "Trail", NAMES, 0),
            ModuleSetting.plain(id(), "density_ticks", "Every N ticks", 1, 20, 1)
        );
    }

    @Override
    public void onTick() {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || minecraft.level == null) {
            return;
        }
        Style style = STYLES.get(styleIndex());

        // Density defaults to the style's own rate, so switching to a heavy
        // effect doesn't inherit a rate tuned for a light one.
        int every = Math.max(1, ModuleManager.get().config().getModuleInt(
            id(), "density_ticks", style.everyTicks()
        ));
        if (player.tickCount % every != 0) {
            return;
        }

        ParticleOptions particle = style.particle() != null
            ? style.particle()
            : Trails.themedDust(id(), 1.0f);

        double ox = (Math.random() - 0.5) * style.spread();
        double oz = (Math.random() - 0.5) * style.spread();
        minecraft.level.addParticle(
            particle,
            player.getX() + ox,
            player.getY() + style.height(),
            player.getZ() + oz,
            0.0,
            0.0,
            0.0
        );
    }

    private int styleIndex() {
        int stored = ModuleManager.get().config().getModuleInt(id(), "style", 0);
        return Math.max(0, Math.min(STYLES.size() - 1, stored));
    }
}
