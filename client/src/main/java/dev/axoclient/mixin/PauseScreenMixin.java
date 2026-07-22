package dev.axoclient.mixin;

import dev.axoclient.gui.AxoMenuScreen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Adds an "Axo Client" button to the in-game pause menu that opens the hub.
 * Only the full pause menu builds the button grid, so guard on width being
 * laid out; injecting at init TAIL after the vanilla buttons is enough.
 */
@Mixin(PauseScreen.class)
public abstract class PauseScreenMixin extends Screen {
    protected PauseScreenMixin(Component title) {
        super(title);
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void axoclient$addButton(CallbackInfo ci) {
        this.addRenderableWidget(
            Button.builder(Component.literal("Axo Client"),
                    b -> this.minecraft.setScreen(new AxoMenuScreen(this)))
                .bounds(this.width / 2 - 102, this.height - 40, 204, 20)
                .build());
    }
}
