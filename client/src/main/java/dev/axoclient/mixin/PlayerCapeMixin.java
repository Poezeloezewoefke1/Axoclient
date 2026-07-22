package dev.axoclient.mixin;

import dev.axoclient.cosmetic.Capes;
import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.core.ClientAsset;
import net.minecraft.world.entity.player.PlayerSkin;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Patches a custom cape into the local player's skin when a cape module is
 * active. Uses PlayerSkin#with(Patch) to override only the cape slot, leaving
 * body / model / elytra intact, and only for the client's own player so it
 * stays a purely local cosmetic.
 */
@Mixin(AbstractClientPlayer.class)
public abstract class PlayerCapeMixin {

    @Inject(method = "getSkin", at = @At("RETURN"), cancellable = true)
    private void axoclient$cape(CallbackInfoReturnable<PlayerSkin> cir) {
        ClientAsset.ResourceTexture cape = Capes.active();
        if (cape == null || (Object) this != Minecraft.getInstance().player) {
            return;
        }
        PlayerSkin skin = cir.getReturnValue();
        if (skin == null) {
            return;
        }
        PlayerSkin.Patch patch = new PlayerSkin.Patch(
            Optional.empty(),
            Optional.of(cape),
            Optional.empty(),
            Optional.empty()
        );
        cir.setReturnValue(skin.with(patch));
    }
}
