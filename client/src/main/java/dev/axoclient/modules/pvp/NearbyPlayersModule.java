package dev.axoclient.modules.pvp;

import dev.axoclient.core.ModuleCategory;
import dev.axoclient.gui.theme.Themes;
import dev.axoclient.hud.HudAnchor;
import dev.axoclient.hud.HudModule;
import dev.axoclient.hud.HudPosition;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Player;

/**
 * The closest players the client can see, nearest first, with distance and
 * health. Only players within your render distance are known to the client,
 * so this shows nothing a glance around wouldn't — it just sorts it.
 *
 * The header line is what the HUD editor drags; the list hangs below it.
 */
public final class NearbyPlayersModule extends HudModule {
    private static final int MAX_LISTED = 5;

    private final List<String> lines = new ArrayList<>();

    public NearbyPlayersModule() {
        super("nearby_players", "Nearby Players", ModuleCategory.PVP, new HudPosition(HudAnchor.MIDDLE_RIGHT, -4, 0), false);
    }

    @Override
    public void onTick() {
        lines.clear();
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer self = minecraft.player;
        if (self == null || minecraft.level == null) {
            return;
        }
        List<Player> others = new ArrayList<>();
        for (Player other : minecraft.level.players()) {
            if (other != self && !other.isSpectator()) {
                others.add(other);
            }
        }
        others.sort(Comparator.comparingDouble(self::distanceTo));
        for (int i = 0; i < Math.min(MAX_LISTED, others.size()); i++) {
            Player other = others.get(i);
            lines.add(String.format(
                Locale.ROOT,
                "%s  %.0fm  %.0f HP",
                other.getName().getString(),
                self.distanceTo(other),
                other.getHealth()
            ));
        }
    }

    @Override
    protected String hudText() {
        if (Minecraft.getInstance().player == null) {
            return null;
        }
        return lines.isEmpty() ? "No players nearby" : "Players nearby";
    }

    @Override
    public void renderHud(GuiGraphics graphics) {
        super.renderHud(graphics);
        String header = hudText();
        if (header == null || lines.isEmpty()) {
            return;
        }
        Font font = Minecraft.getInstance().font;
        int headerX = position().anchor().x(graphics.guiWidth(), font.width(header), position().offsetX());
        int y = position().anchor().y(graphics.guiHeight(), font.lineHeight, position().offsetY());
        // Right-anchored lists grow leftwards so they never run off screen.
        boolean rightAligned = position().anchor().name().endsWith("RIGHT");
        int colour = Themes.current().text;
        for (String line : lines) {
            y += font.lineHeight + 2;
            int x = rightAligned ? headerX + font.width(header) - font.width(line) : headerX;
            graphics.drawString(font, line, x, y, colour, true);
        }
    }
}
