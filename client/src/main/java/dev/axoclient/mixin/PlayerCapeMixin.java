package dev.axoclient.mixin;

import dev.axoclient.cosmetic.Capes;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Supplies a custom cape texture in the local player's skin when a cape module
 * is active. Only rewrites the cape field of the resolved skin, leaving body /
 * model / elytra untouched, and only for the client's own player so it stays a
 * purely local cosmetic.
 */
@Mixin(AbstractClientPlayer.class)
public abstract class PlayerCapeMixin {

    @Inject(method = "getSkin", at = @At("RETURN"), cancellable = true)
    private void axoclient$cape(CallbackInfoReturnable<PlayerSkin> cir) {
        ResourceLocation cape = Capes.active();
        if (cape == null || (Object) this != Minecraft.getInstance().player) {
            return;
        }
        PlayerSkin skin = cir.getReturnValue();
        if (skin == null) {
            return;
        }
        cir.setReturnValue(new PlayerSkin(
            skin.texture(),
            skin.textureUrl(),
            cape,
            skin.elytraTexture(),
            skin.model(),
            skin.secure()
        ));
    }
}
