package dev.axoclient.mixin.optional;

import dev.axoclient.camera.Freelook;
import net.minecraft.client.Camera;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Points the camera at the freelook rotation instead of the player's.
 *
 * Runs at the tail of setup(), after vanilla has positioned the camera from
 * the player's own yaw/pitch, and overrides just the rotation. setRotation is
 * used rather than writing the fields directly because it also rebuilds the
 * rotation quaternion and the forward/up/left vectors — setting xRot/yRot
 * alone would leave those stale and the view would not actually move.
 *
 * setup() is matched by name with no captured arguments on purpose: its
 * parameter list has changed across Minecraft versions, and not naming it
 * means one less thing to break on a port.
 *
 * Declared in axoclient.optional.mixins.json, so a failure here costs the
 * freelook feature and nothing else.
 */
@Mixin(Camera.class)
public abstract class CameraMixin {

    @Invoker("setRotation")
    abstract void axoclient$invokeSetRotation(float yaw, float pitch);

    @Inject(method = "setup", at = @At("TAIL"))
    private void axoclient$freelook(CallbackInfo ci) {
        // Reaching this line at all is the proof the hook applied; the turn
        // mixin will not swallow mouse input until it has been set.
        Freelook.markCameraHookAlive();
        if (Freelook.engaged()) {
            axoclient$invokeSetRotation(Freelook.yaw(), Freelook.pitch());
        }
    }
}
