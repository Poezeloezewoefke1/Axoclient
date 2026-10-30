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
 * Draws the artwork at HEAD of render, before the buttons. If this stops
 * matching, you get the plain vanilla menu.
 *
 * The panorama that would paint over it is suppressed separately by
 * {@link ScreenPanoramaMixin}: on 1.21.11 renderPanorama is declared on
 * Screen, not TitleScreen, so a hook here never matched (the build log said
 * so on every build). If that one stops matching, the panorama comes back and
 * the artwork is hidden behind it.
 *
 * Either way the menu still works, which is why this lives in
 * axoclient.optional.mixins.json rather than next to the required mixins.
 */
@Mixin(TitleScreen.class)
public abstract class TitleBackgroundMixin {

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
