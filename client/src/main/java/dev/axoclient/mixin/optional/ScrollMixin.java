package dev.axoclient.mixin.optional;

import dev.axoclient.camera.Zoom;
import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Lets the mouse wheel adjust zoom while the zoom key is held, and stops that
 * scroll from also changing hotbar slot.
 *
 * Declared in axoclient.optional.mixins.json: if onScroll changes shape in a
 * future Minecraft version, this stops applying, scrolling behaves like
 * vanilla again, and zoom itself is unaffected.
 */
@Mixin(MouseHandler.class)
public abstract class ScrollMixin {

    @Inject(method = "onScroll", at = @At("HEAD"), cancellable = true)
    private void axoclient$zoomScroll(long window, double xOffset, double yOffset, CallbackInfo ci) {
        if (Zoom.scroll(yOffset)) {
            ci.cancel();
        }
    }
}
