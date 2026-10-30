package dev.axoclient.modules.pvp;

import dev.axoclient.core.AxoModule;
import dev.axoclient.core.HudRenderable;
import dev.axoclient.core.ModuleCategory;
import dev.axoclient.core.ModuleSetting;
import dev.axoclient.gui.render.GuiRender;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;

/**
 * A pulsing red edge around the screen when your health drops low, so you
 * notice mid-fight without looking down at the hearts. It pulses faster the
 * closer you are to dying.
 *
 * Drawn as nested 1px outlines fading inwards, because GuiGraphics#fill only
 * does solid boxes and a vignette texture would be one more asset to ship.
 *
 * Config (module "low_health_warning"):
 *   hearts — warn at or below this many hearts
 */
public final class LowHealthWarningModule extends AxoModule implements HudRenderable {
    private static final int DEFAULT_HEARTS = 4;
    /** Depth of the vignette in GUI pixels. */
    private static final int DEPTH = 14;
    private static final int MAX_ALPHA = 0x70;

    public LowHealthWarningModule() {
        super("low_health_warning", "Low Health Warning", ModuleCategory.PVP, false);
    }

    @Override
    public List<ModuleSetting> settings() {
        return List.of(ModuleSetting.plain(id(), "hearts", "Warn at hearts", 1, 10, DEFAULT_HEARTS));
    }

    @Override
    public void renderHud(GuiGraphics graphics) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null || player.isCreative() || player.isSpectator()) {
            return;
        }
        float health = player.getHealth();
        float threshold = settings().get(0).get() * 2.0F;
        if (health <= 0.0F || health > threshold) {
            return;
        }
        // 1.0 at the threshold down to 0.0 at death: drives pulse speed.
        float danger = 1.0F - health / threshold;
        double period = 1200.0 - 700.0 * danger;
        double phase = (System.currentTimeMillis() % (long) period) / period;
        float pulse = 0.55F + 0.45F * (float) Math.sin(phase * Math.PI * 2.0);

        int width = graphics.guiWidth();
        int height = graphics.guiHeight();
        for (int i = 0; i < DEPTH; i++) {
            float fade = 1.0F - (float) i / DEPTH;
            int alpha = Math.round(MAX_ALPHA * pulse * fade * fade);
            if (alpha <= 0) {
                continue;
            }
            GuiRender.outline(graphics, i, i, width - i * 2, height - i * 2, (alpha << 24) | 0xE02020);
        }
    }
}
