package dev.axoclient.mixin;

import dev.axoclient.gui.AxoMenuScreen;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Brands the main (boot) menu: an "Axo Client" button that opens the hub, and
 * a small watermark in the corner. Extends Screen so the inherited widget and
 * layout helpers are visible to the injected methods.
 */
@Mixin(TitleScreen.class)
public abstract class TitleScreenMixin extends Screen {
    protected TitleScreenMixin(Component title) {
        super(title);
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void axoclient$addButton(CallbackInfo ci) {
        this.addRenderableWidget(
            Button.builder(Component.literal("Axo Client"),
                    b -> this.minecraft.setScreen(new AxoMenuScreen(this)))
                .bounds(this.width / 2 - 100, this.height - 52, 200, 20)
                .build());
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void axoclient$watermark(GuiGraphics graphics, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        graphics.drawString(this.font, "Axo Client", 4, this.height - 20, 0xFF38BDF8, true);
    }
}
