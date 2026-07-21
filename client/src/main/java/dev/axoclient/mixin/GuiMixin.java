package dev.axoclient.mixin;

import dev.axoclient.core.ModuleManager;
import dev.axoclient.gui.notify.Notifications;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Draws Axo HUD modules after the vanilla HUD each frame. This is the only
 * render hook the scaffold needs; once Fabric API is added (P1-02) this can
 * migrate to its HUD events if the injection point ever breaks.
 * Verify the render(...) signature against 1.21.11 mappings in task P1-01.
 */
@Mixin(Gui.class)
public abstract class GuiMixin {
    @Inject(method = "render", at = @At("TAIL"))
    private void axoclient$renderHud(GuiGraphics graphics, DeltaTracker deltaTracker, CallbackInfo ci) {
        ModuleManager.get().renderHud(graphics);
        Notifications.render(graphics, graphics.guiWidth());
    }
}
