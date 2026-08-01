package dev.axoclient.mixin.optional;

import dev.axoclient.camera.Zoom;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Returns an eased field of view while zooming, so the ramp moves every frame
 * as a double instead of once per tick as a rounded integer.
 *
 * Injected at RETURN and only ever <em>adjusts</em> the value vanilla already
 * computed, so effects that also change FOV (speed, sprinting, nausea) keep
 * working and compose with the zoom rather than being replaced by it.
 *
 * The {@code useFovSetting} guard matters: getFov is also called for the
 * held-item projection, and zooming that would make the hand swim in and out
 * of the screen.
 *
 * Declared in axoclient.optional.mixins.json — if the renderer changes shape,
 * zoom falls back to the option-writing path in ZoomModule instead of the
 * game failing to boot.
 */
@Mixin(GameRenderer.class)
public abstract class FovMixin {

    @Inject(method = "getFov", at = @At("RETURN"), cancellable = true)
    private void axoclient$zoom(
        Camera camera, float partialTick, boolean useFovSetting, CallbackInfoReturnable<Double> cir
    ) {
        // Reaching this line is the proof the hook applied, which is what
        // lets ZoomModule skip its fallback.
        Zoom.markHookAlive();
        if (!useFovSetting || !Zoom.affectingFov()) {
            return;
        }
        double base = cir.getReturnValueD();
        double zoomed = Zoom.apply(base);
        if (zoomed != base) {
            cir.setReturnValue(zoomed);
        }
    }
}
