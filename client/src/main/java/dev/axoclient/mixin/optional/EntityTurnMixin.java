package dev.axoclient.mixin.optional;

import dev.axoclient.camera.Freelook;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * While freelook is engaged, banks the mouse movement into
 * {@link Freelook} instead of turning the player.
 *
 * Targets Entity#turn rather than LocalPlayer because LocalPlayer does not
 * override it, hence the identity guard: this must only ever affect the
 * client's own player, never a mob or another player.
 *
 * Freelook.engaged() is false unless the camera hook has proven itself, so a
 * half-applied pair cannot leave the view frozen.
 *
 * Declared in axoclient.optional.mixins.json — non-fatal by design.
 */
@Mixin(Entity.class)
public abstract class EntityTurnMixin {

    @Inject(method = "turn(DD)V", at = @At("HEAD"), cancellable = true)
    private void axoclient$freelook(double deltaYaw, double deltaPitch, CallbackInfo ci) {
        if (!Freelook.engaged() || (Object) this != Minecraft.getInstance().player) {
            return;
        }
        Freelook.applyMouse(deltaYaw, deltaPitch);
        ci.cancel();
    }
}
