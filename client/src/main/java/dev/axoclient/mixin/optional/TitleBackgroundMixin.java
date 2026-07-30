package dev.axoclient.mixin.optional;

import dev.axoclient.gui.render.MenuBackground;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.TitleScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Replaces the rotating vanilla panorama on the main menu with the Axo
 * artwork.
 *
 * Two independent injections, and the config's defaultRequire of 0 means
 * either may fail on its own:
 *
 *   panorama  — suppressed so it doesn't paint over the artwork. If this stops
 *               matching, the panorama comes back and the artwork is simply
 *               hidden behind it.
 *   render    — draws the artwork at HEAD, before the buttons. If this stops
 *               matching, you get the plain vanilla menu.
 *
 * Either way the menu still works, which is why this lives in
 * axoclient.optional.mixins.json rather than next to the required mixins.
 */
@Mixin(TitleScreen.class)
public abstract class TitleBackgroundMixin {

    @Inject(method = "renderPanorama", at = @At("HEAD"), cancellable = true, require = 0)
    private void axoclient$noPanorama(CallbackInfo ci) {
        ci.cancel();
    }

    @Inject(method = "render", at = @At("HEAD"), require = 0)
    private void axoclient$background(
        GuiGraphics graphics,
        int mouseX,
        int mouseY,
        float delta,
        CallbackInfo ci
    ) {
        MenuBackground.render(graphics, graphics.guiWidth(), graphics.guiHeight());
    }
}
