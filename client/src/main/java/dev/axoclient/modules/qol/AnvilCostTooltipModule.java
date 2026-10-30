package dev.axoclient.modules.qol;

import dev.axoclient.core.AxoModule;
import dev.axoclient.core.ModuleCategory;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/**
 * Shows an item's anvil work penalty: the extra levels every further anvil
 * use costs, which roughly doubles each time the item goes through one. Once
 * a single operation would cost 40 levels the anvil refuses with "Too
 * Expensive!", so this tells you how many repairs or combines you have left
 * before an item is locked in.
 *
 * Driven by {@link dev.axoclient.util.Tooltips}. Items never worked on show
 * nothing.
 */
public final class AnvilCostTooltipModule extends AxoModule {
    /** Penalty at which almost any further operation crosses the 40-level cap. */
    private static final int NEAR_LIMIT = 31;

    public AnvilCostTooltipModule() {
        super("anvil_cost_tooltip", "Anvil Cost Tooltips", ModuleCategory.QOL, true);
    }

    public void appendTo(ItemStack stack, List<Component> lines) {
        Integer penalty = stack.get(DataComponents.REPAIR_COST);
        if (penalty == null || penalty <= 0) {
            return;
        }
        lines.add(Component.literal("Anvil penalty: ")
            .withStyle(ChatFormatting.GRAY)
            .append(Component.literal("+" + penalty + (penalty == 1 ? " level" : " levels"))
                .withStyle(penalty >= NEAR_LIMIT ? ChatFormatting.RED : ChatFormatting.AQUA)));
    }
}
