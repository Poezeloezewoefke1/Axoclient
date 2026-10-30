package dev.axoclient.modules.hud;

import dev.axoclient.core.ModuleSetting;
import dev.axoclient.gui.notify.Notification;
import dev.axoclient.gui.notify.Notifications;
import dev.axoclient.hud.HudAnchor;
import dev.axoclient.hud.HudModule;
import dev.axoclient.hud.HudPosition;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

/**
 * Shouts when a worn or held item is nearly broken, so a good pickaxe or a
 * full set of armour doesn't shatter mid-fight. Silent until something is
 * actually in danger.
 *
 * Besides the HUD line, the moment an item first crosses the line it gets a
 * notification and (unless turned off) a short sound, once per item, so you
 * hear it even when you're not looking at the corner of the screen.
 *
 * Config (module "durability_warning"):
 *   alert — notification and sound on first crossing (On / Off)
 */
public final class DurabilityWarningModule extends HudModule {
    /** Warn from this fraction of durability remaining. */
    private static final double WARN_AT = 0.10;

    private static final EquipmentSlot[] SLOTS = {
        EquipmentSlot.HEAD,
        EquipmentSlot.CHEST,
        EquipmentSlot.LEGS,
        EquipmentSlot.FEET,
        EquipmentSlot.MAINHAND
    };

    /** "slot:item" keys already alerted, so each item alerts once until repaired or swapped. */
    private final Set<String> alerted = new HashSet<>();

    public DurabilityWarningModule() {
        super("durability_warning", "Durability Warning", new HudPosition(HudAnchor.TOP_CENTER, 0, 34), false);
    }

    @Override
    public List<ModuleSetting> settings() {
        return List.of(ModuleSetting.choice(id(), "alert", "Alert", List.of("On", "Off"), 0));
    }

    @Override
    public void onTick() {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null) {
            return;
        }
        Set<String> low = new HashSet<>();
        for (EquipmentSlot slot : SLOTS) {
            ItemStack stack = player.getItemBySlot(slot);
            int percent = percentLeft(stack);
            if (percent < 0 || percent > WARN_AT * 100) {
                continue;
            }
            String name = stack.getHoverName().getString();
            String key = slot.name() + ":" + name;
            low.add(key);
            if (!alerted.contains(key) && settings().get(0).get() == 0) {
                Notifications.push(name + " is about to break (" + percent + "%)", Notification.Type.WARN);
                minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.ANVIL_LAND, 1.6F));
            }
        }
        // Forget items that were repaired or put away, so they can alert again.
        alerted.retainAll(low);
        alerted.addAll(low);
    }

    @Override
    protected String hudText() {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return null;
        }
        int worst = 101;
        String worstName = null;
        for (EquipmentSlot slot : SLOTS) {
            ItemStack stack = player.getItemBySlot(slot);
            int percent = percentLeft(stack);
            if (percent >= 0 && percent <= WARN_AT * 100 && percent < worst) {
                worst = percent;
                worstName = stack.getHoverName().getString();
            }
        }
        return worstName == null ? null : "! " + worstName + " " + worst + "%";
    }

    /** Durability left as a percentage, or -1 for empty or unbreakable stacks. */
    private static int percentLeft(ItemStack stack) {
        int max = stack.getMaxDamage();
        if (stack.isEmpty() || max <= 0) {
            return -1;
        }
        return (int) Math.round((max - stack.getDamageValue()) * 100.0 / max);
    }
}
