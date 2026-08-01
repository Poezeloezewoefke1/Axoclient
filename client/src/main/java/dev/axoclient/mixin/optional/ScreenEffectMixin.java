package dev.axoclient.mixin.optional;

import dev.axoclient.modules.qol.ClearLiquidsModule;
import net.minecraft.client.renderer.ScreenEffectRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Suppresses the full-screen liquid overlays for
 * {@link ClearLiquidsModule}.
 *
 * The whole screen-effect pass is cancelled only when every overlay the
 * module covers is set to hidden. That is deliberately conservative: this
 * one entry point draws water, lava, powder snow and the block-on-head
 * overlay together, so cancelling it while the player still wants, say, the
 * lava overlay would silently take away more than they asked for. With a
 * mixed selection we let vanilla draw normally rather than guess.
 *
 * Matched by name with no descriptor, since this renderer's signature has
 * changed repeatedly across versions. Declared in
 * axoclient.optional.mixins.json — a miss costs this feature and nothing else.
 */
@Mixin(ScreenEffectRenderer.class)
public abstract class ScreenEffectMixin {

    @Inject(method = "renderScreenEffect", at = @At("HEAD"), cancellable = true)
    private void axoclient$clearLiquids(CallbackInfo ci) {
        if (ClearLiquidsModule.hidesEverything()) {
            ci.cancel();
        }
    }
}
