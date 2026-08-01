package dev.axoclient.modules.qol;

import dev.axoclient.core.AxoModule;
import dev.axoclient.core.ModuleCategory;
import dev.axoclient.core.ModuleManager;
import dev.axoclient.core.ModuleSetting;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;

/**
 * Lists what is inside a shulker box (or any container item) on its tooltip,
 * so you can find the right box without placing every one of them.
 *
 * Contents come from the {@code CONTAINER} data component, which is what
 * container items carry from 1.20.5 onward — the same data vanilla uses for
 * its own abbreviated preview, so this stays correct without reading NBT by
 * hand.
 *
 * Stacks of the same item are merged, so 4 stacks of cobble read as one line
 * of 256 rather than four lines of 64.
 */
public final class ShulkerTooltipModule extends AxoModule {
    private static final int DEFAULT_MAX_LINES = 8;

    public ShulkerTooltipModule() {
        super("shulker_tooltip", "Shulker Tooltips", ModuleCategory.QOL, true);
    }

    @Override
    public List<ModuleSetting> settings() {
        return List.of(
            ModuleSetting.plain(id(), "max_lines", "Max lines", 1, 27, DEFAULT_MAX_LINES),
            ModuleSetting.choice(id(), "merge", "Stacking", List.of("Merge same items", "One line per stack"), 0)
        );
    }

    /** Appends the contents lines for {@code stack}, or nothing if it holds none. */
    public void appendTo(ItemStack stack, List<Component> lines) {
        ItemContainerContents contents = stack.get(DataComponents.CONTAINER);
        if (contents == null) {
            return;
        }
        List<ItemStack> items = new ArrayList<>();
        boolean merge = ModuleManager.get().config().getModuleInt(id(), "merge", 0) == 0;
        for (ItemStack inner : contents.nonEmptyItems()) {
            if (merge) {
                ItemStack existing = items.stream()
                    .filter(s -> ItemStack.isSameItemSameComponents(s, inner))
                    .findFirst()
                    .orElse(null);
                if (existing != null) {
                    existing.setCount(existing.getCount() + inner.getCount());
                    continue;
                }
                items.add(inner.copy());
            } else {
                items.add(inner.copy());
            }
        }
        if (items.isEmpty()) {
            lines.add(Component.literal("Empty").withStyle(ChatFormatting.DARK_GRAY));
            return;
        }

        int max = Math.max(1, ModuleManager.get().config().getModuleInt(id(), "max_lines", DEFAULT_MAX_LINES));
        int shown = Math.min(max, items.size());
        for (int i = 0; i < shown; i++) {
            ItemStack item = items.get(i);
            lines.add(Component.literal(item.getCount() + "x ")
                .withStyle(ChatFormatting.DARK_GRAY)
                .append(item.getHoverName().copy().withStyle(ChatFormatting.GRAY)));
        }
        if (items.size() > shown) {
            lines.add(Component.literal("and " + (items.size() - shown) + " more...")
                .withStyle(ChatFormatting.DARK_GRAY));
        }
    }
}
