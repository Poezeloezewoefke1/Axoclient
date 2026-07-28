package dev.axoclient.modules.hud;

import dev.axoclient.core.AxoModule;
import dev.axoclient.core.HudRenderable;
import dev.axoclient.core.ModuleCategory;
import dev.axoclient.gui.theme.Themes;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

/**
 * A short log of what you just picked up, so a busy floor doesn't hide the
 * one drop you cared about.
 *
 * Works by diffing your inventory totals between ticks rather than hooking
 * the pickup event: no mixin, and it naturally covers items that arrive by
 * any route — crafting, chests, trades — not just ones off the ground.
 */
public final class PickupLogModule extends AxoModule implements HudRenderable {
    private static final int MAX_LINES = 5;
    /** Ticks a line stays visible (20 = 1 second). */
    private static final int LINE_TICKS = 100;

    private record Entry(String text, int expiresAtTick) {}

    /** Keyed by display name rather than Item: it is what we print anyway. */
    private final Map<String, Integer> counts = new HashMap<>();
    private final Deque<Entry> lines = new ArrayDeque<>();
    private boolean primed;
    private int tick;

    public PickupLogModule() {
        super("pickup_log", "Pickup Log", ModuleCategory.HUD, false);
    }

    @Override
    protected void onEnable() {
        // Start from a clean baseline, or enabling would report the whole
        // inventory as freshly picked up.
        counts.clear();
        lines.clear();
        primed = false;
    }

    @Override
    public void onTick() {
        tick++;
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            primed = false;
            return;
        }
        Map<String, Integer> current = tally(player.getInventory());
        if (primed) {
            for (Map.Entry<String, Integer> entry : current.entrySet()) {
                int gained = entry.getValue() - counts.getOrDefault(entry.getKey(), 0);
                if (gained > 0) {
                    push("+" + gained + " " + entry.getKey());
                }
            }
        }
        counts.clear();
        counts.putAll(current);
        primed = true;

        while (!lines.isEmpty() && lines.peekFirst().expiresAtTick() <= tick) {
            lines.removeFirst();
        }
    }

    private void push(String text) {
        if (lines.size() >= MAX_LINES) {
            lines.removeFirst();
        }
        lines.addLast(new Entry(text, tick + LINE_TICKS));
    }

    private static Map<String, Integer> tally(Inventory inventory) {
        Map<String, Integer> totals = new HashMap<>();
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (!stack.isEmpty()) {
                totals.merge(stack.getHoverName().getString(), stack.getCount(), Integer::sum);
            }
        }
        return totals;
    }

    @Override
    public void renderHud(GuiGraphics graphics) {
        if (lines.isEmpty()) {
            return;
        }
        Font font = Minecraft.getInstance().font;
        int accent = Themes.current().accent;
        int step = font.lineHeight + 2;
        int y = graphics.guiHeight() / 2 - (lines.size() * step) / 2;
        for (Entry entry : lines) {
            graphics.drawString(font, entry.text(), 6, y, accent, true);
            y += step;
        }
    }
}
