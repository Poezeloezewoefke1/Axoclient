package dev.axoclient.modules.hud;

import dev.axoclient.core.ModuleManager;
import dev.axoclient.hud.HudAnchor;
import dev.axoclient.hud.HudModule;
import dev.axoclient.hud.HudPosition;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;

/**
 * What your character is doing — sprinting, sneaking, swimming, flying,
 * gliding — and whether Toggle Sprint is holding the sprint for you.
 * Hidden while standing still.
 */
public final class MovementStateModule extends HudModule {

    public MovementStateModule() {
        super("movement_state", "Movement State", new HudPosition(HudAnchor.BOTTOM_LEFT, 4, -26), false);
    }

    @Override
    protected String hudText() {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return null;
        }
        if (player.isFallFlying()) {
            return "Gliding";
        }
        if (player.getAbilities().flying) {
            return "Flying";
        }
        if (player.isSwimming()) {
            return "Swimming";
        }
        if (player.isShiftKeyDown()) {
            return "Sneaking";
        }
        if (player.isSprinting()) {
            boolean toggled = ModuleManager.get().byId("toggle_sprint")
                .map(ModuleManager.get()::isEnabled)
                .orElse(false);
            return toggled ? "Sprinting (toggled)" : "Sprinting";
        }
        double dx = player.getDeltaMovement().x;
        double dz = player.getDeltaMovement().z;
        return dx * dx + dz * dz > 1.0E-4 ? "Walking" : null;
    }
}
