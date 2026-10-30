package dev.axoclient.mixin.optional;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Suppresses the vanilla panorama on the title screen only, so the Axo
 * artwork drawn by {@link TitleBackgroundMixin} isn't painted over.
 *
 * TitleBackgroundMixin also tries this on TitleScreen itself, but on 1.21.11
 * TitleScreen doesn't declare renderPanorama — it inherits it from Screen —
 * and the CI build log reports that target as unresolvable. A mixin can only
 * hook a method in the class that declares it, so this hooks Screen and
 * checks the instance. Everything else that uses the panorama (other menus)
 * is untouched.
 *
 * Optional config: if Screen#renderPanorama changes too, this stops
 * applying and the menu falls back to the vanilla panorama.
 */
@Mixin(Screen.class)
public abstract class ScreenPanoramaMixin {

    @Inject(method = "renderPanorama", at = @At("HEAD"), cancellable = true, require = 0)
    private void axoclient$noTitlePanorama(CallbackInfo ci) {
        if ((Object) this instanceof TitleScreen) {
            ci.cancel();
        }
    }
}
