package dev.axoclient.mixin.optional;

import dev.axoclient.chat.ChatTweaks;
import net.minecraft.client.GuiMessageTag;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MessageSignature;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Chat timestamps and duplicate suppression.
 *
 * Declared in axoclient.optional.mixins.json (required:false,
 * defaultRequire:0) rather than the main config. Injecting into chat means
 * targeting a method signature Mojang does change, and this is a nice-to-have
 * — if it stops matching on a future Minecraft version, the two chat modules
 * quietly stop working and the game still boots. That trade is the whole
 * reason the optional config exists.
 *
 * Targets the three-argument sink: the one-argument addMessage delegates to
 * it, so hooking here catches both player and system messages.
 */
@Mixin(ChatComponent.class)
public abstract class ChatMixin {

    @ModifyVariable(
        method = "addMessage(Lnet/minecraft/network/chat/Component;"
            + "Lnet/minecraft/network/chat/MessageSignature;"
            + "Lnet/minecraft/client/GuiMessageTag;)V",
        at = @At("HEAD"),
        argsOnly = true
    )
    private Component axoclient$timestamp(Component message) {
        return ChatTweaks.decorate(message);
    }

    @Inject(
        method = "addMessage(Lnet/minecraft/network/chat/Component;"
            + "Lnet/minecraft/network/chat/MessageSignature;"
            + "Lnet/minecraft/client/GuiMessageTag;)V",
        at = @At("HEAD"),
        cancellable = true
    )
    private void axoclient$antiSpam(
        Component message,
        MessageSignature signature,
        GuiMessageTag tag,
        CallbackInfo ci
    ) {
        if (ChatTweaks.shouldSuppress(message)) {
            ci.cancel();
        }
    }
}
