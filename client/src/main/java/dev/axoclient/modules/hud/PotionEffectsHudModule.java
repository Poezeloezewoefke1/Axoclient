package dev.axoclient.modules.hud;

import dev.axoclient.core.AxoModule;
import dev.axoclient.core.HudRenderable;
import dev.axoclient.core.ModuleCategory;
import dev.axoclient.gui.theme.Themes;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.effect.MobEffectInstance;

/**
 * Active potion effects with level and remaining time, stacked up from the
 * bottom-left. Renders itself rather than extending {@code HudModule}
 * because it draws a variable number of lines (same approach as the module
 * list). Off by default.
 */
public final class PotionEffectsHudModule extends AxoModule implements HudRenderable {
    /** Amplifier 0 is level I and shown bare; higher levels get a numeral. */
    private static final String[] LEVELS = {"", " II", " III", " IV", " V"};

    public PotionEffectsHudModule() {
        super("potion_hud", "Potion Effects", ModuleCategory.HUD, false);
    }

    @Override
    public void renderHud(GuiGraphics graphics) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }
        List<String> lines = new ArrayList<>();
        for (MobEffectInstance effect : player.getActiveEffects()) {
            lines.add(label(effect));
        }
        if (lines.isEmpty()) {
            return;
        }
        lines.sort(String::compareTo);

        Font font = Minecraft.getInstance().font;
        int accent = Themes.current().accent;
        int step = font.lineHeight + 2;
        int y = graphics.guiHeight() - 4 - lines.size() * step;
        for (String line : lines) {
            int width = font.width(line);
            graphics.fill(1, y, width + 6, y + font.lineHeight + 1, 0x66000000);
            graphics.fill(1, y, 2, y + font.lineHeight + 1, accent);
            graphics.drawString(font, line, 4, y + 1, accent, true);
            y += step;
        }
    }

    private static String label(MobEffectInstance effect) {
        int amplifier = effect.getAmplifier();
        String level = amplifier >= 0 && amplifier < LEVELS.length
            ? LEVELS[amplifier]
            : " " + (amplifier + 1);
        return effect.getEffect().value().getDisplayName().getString() + level + " " + time(effect);
    }

    private static String time(MobEffectInstance effect) {
        int duration = effect.getDuration();
        if (duration < 0) {
            return "--"; // infinite-duration effects use a negative duration
        }
        int seconds = duration / 20;
        return String.format(Locale.ROOT, "%d:%02d", seconds / 60, seconds % 60);
    }
}
