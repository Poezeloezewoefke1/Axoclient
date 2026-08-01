package dev.axoclient.modules.qol;

import dev.axoclient.core.AxoModule;
import dev.axoclient.core.ModuleCategory;
import dev.axoclient.core.ModuleManager;
import dev.axoclient.core.ModuleSetting;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/**
 * Adds an exact durability line to damageable items, because the vanilla bar
 * only tells you roughly how worn something is and only once it is damaged
 * at all.
 *
 * The line is coloured by how much is left, so a glance is enough: green
 * healthy, yellow worth watching, red about to break.
 *
 * Rendering is driven by {@link dev.axoclient.util.Tooltips}, which owns the
 * single Fabric tooltip callback shared by every tooltip module.
 */
public final class DurabilityTooltipModule extends AxoModule {

    public DurabilityTooltipModule() {
        super("durability_tooltip", "Durability Tooltips", ModuleCategory.QOL, true);
    }

    @Override
    public List<ModuleSetting> settings() {
        return List.of(
            ModuleSetting.choice(id(), "style", "Show as", List.of("1234 / 1561", "79%", "1234 / 1561 (79%)"), 2)
        );
    }

    /** Appends the durability line for {@code stack}, or nothing if it has none. */
    public void appendTo(ItemStack stack, List<Component> lines) {
        if (!stack.isDamageableItem()) {
            return;
        }
        int max = stack.getMaxDamage();
        int left = max - stack.getDamageValue();
        if (max <= 0) {
            return;
        }
        int percent = (int) Math.round(left * 100.0 / max);

        String text = switch (ModuleManager.get().config().getModuleInt(id(), "style", 2)) {
            case 0 -> left + " / " + max;
            case 1 -> percent + "%";
            default -> left + " / " + max + " (" + percent + "%)";
        };
        lines.add(Component.literal("Durability: ")
            .withStyle(ChatFormatting.GRAY)
            .append(Component.literal(text).withStyle(colourFor(percent))));
    }

    private static ChatFormatting colourFor(int percent) {
        if (percent <= 10) {
            return ChatFormatting.RED;
        }
        if (percent <= 33) {
            return ChatFormatting.GOLD;
        }
        if (percent <= 66) {
            return ChatFormatting.YELLOW;
        }
        return ChatFormatting.GREEN;
    }
}
