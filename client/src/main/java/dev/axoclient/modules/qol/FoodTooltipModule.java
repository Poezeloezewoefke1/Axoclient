package dev.axoclient.modules.qol;

import dev.axoclient.core.AxoModule;
import dev.axoclient.core.ModuleCategory;
import java.util.List;
import java.util.Locale;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;

/**
 * Hunger and saturation restored, on every food's tooltip. Saturation is the
 * hidden half that decides how long you stay full, and vanilla never shows
 * it — it's why steak beats a stack of bread.
 *
 * Driven by {@link dev.axoclient.util.Tooltips}.
 */
public final class FoodTooltipModule extends AxoModule {

    public FoodTooltipModule() {
        super("food_tooltip", "Food Tooltips", ModuleCategory.QOL, true);
    }

    public void appendTo(ItemStack stack, List<Component> lines) {
        FoodProperties food = stack.get(DataComponents.FOOD);
        if (food == null) {
            return;
        }
        lines.add(Component.literal("Food: ")
            .withStyle(ChatFormatting.GRAY)
            .append(Component.literal("+" + food.nutrition() + " hunger").withStyle(ChatFormatting.GOLD))
            .append(Component.literal(" · ").withStyle(ChatFormatting.GRAY))
            .append(Component.literal(String.format(Locale.ROOT, "+%.1f saturation", food.saturation()))
                .withStyle(ChatFormatting.YELLOW)));
    }
}
