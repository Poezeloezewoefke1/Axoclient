package dev.axoclient.modules.pvp;

import dev.axoclient.core.AxoModule;
import dev.axoclient.core.ModuleCategory;
import dev.axoclient.core.ModuleSetting;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

/**
 * A crisp confirmation sound when your hit actually lands, so you can tell a
 * connected hit from a whiff without watching the health bar. Detects hits
 * the same way the combo counter does — the target's health dropping — and
 * plays the sound only for you.
 *
 * Config (module "hit_sound"):
 *   sound — which sound to play
 */
public final class HitSoundModule extends AxoModule {
    private static final float MIN_DAMAGE = 0.05F;
    private static final List<String> SOUND_NAMES = List.of("Ding", "Arrow", "Crit");

    private int trackedId = -1;
    private float lastHealth;

    public HitSoundModule() {
        super("hit_sound", "Hit Sound", ModuleCategory.PVP, false);
    }

    @Override
    public List<ModuleSetting> settings() {
        return List.of(ModuleSetting.choice(id(), "sound", "Sound", SOUND_NAMES, 0));
    }

    @Override
    public void onTick() {
        Minecraft minecraft = Minecraft.getInstance();
        Entity target = minecraft.crosshairPickEntity;
        if (minecraft.player == null || !(target instanceof LivingEntity living)) {
            trackedId = -1;
            return;
        }
        float health = living.getHealth();
        if (living.getId() == trackedId && lastHealth - health >= MIN_DAMAGE) {
            minecraft.getSoundManager().play(SimpleSoundInstance.forUI(sound(), 1.0F));
        }
        trackedId = living.getId();
        lastHealth = health;
    }

    private SoundEvent sound() {
        return switch (settings().get(0).get()) {
            case 1 -> SoundEvents.ARROW_HIT_PLAYER;
            case 2 -> SoundEvents.PLAYER_ATTACK_CRIT;
            default -> SoundEvents.EXPERIENCE_ORB_PICKUP;
        };
    }
}
