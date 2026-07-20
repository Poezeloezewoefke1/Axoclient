package dev.axoclient.ui;

import dev.axoclient.core.AxoConfig;
import dev.axoclient.core.AxoModule;
import dev.axoclient.core.ModuleManager;
import dev.axoclient.hud.HudAnchor;
import dev.axoclient.hud.HudModule;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * HUD placement editor (roadmap P5-09, button-based v1): cycle each HUD
 * module through the nine anchors and nudge its pixel offsets. Changes
 * write to config and apply live via HudModule.reloadPosition(). Drag
 * editing can replace this later without touching the modules.
 */
public final class HudLayoutScreen extends Screen {
    private static final int AXO_BLUE = 0xFF38BDF8;
    private static final int NUDGE = 4;

    public HudLayoutScreen() {
        super(Component.literal("HUD Layout"));
    }

    @Override
    protected void init() {
        AxoConfig config = ModuleManager.get().config();
        int y = 44;
        for (AxoModule module : ModuleManager.get().all()) {
            if (!(module instanceof HudModule hud)) {
                continue;
            }
            int x = this.width / 2 - 156;

            this.addRenderableWidget(Button.builder(anchorLabel(config, hud), button -> {
                HudAnchor next = nextAnchor(currentAnchor(config, hud));
                config.setModuleString(hud.id(), "hud_anchor", next.name());
                hud.reloadPosition();
                button.setMessage(anchorLabel(config, hud));
            }).bounds(x, y, 200, 20).build());

            this.addRenderableWidget(nudgeButton(config, hud, "hud_x", -NUDGE, "X-", x + 204, y));
            this.addRenderableWidget(nudgeButton(config, hud, "hud_x", NUDGE, "X+", x + 232, y));
            this.addRenderableWidget(nudgeButton(config, hud, "hud_y", -NUDGE, "Y-", x + 260, y));
            this.addRenderableWidget(nudgeButton(config, hud, "hud_y", NUDGE, "Y+", x + 288, y));
            y += 24;
        }
        this.addRenderableWidget(
            Button.builder(Component.literal("Done"), button -> this.onClose())
                .bounds(this.width / 2 - 100, y + 8, 200, 20)
                .build()
        );
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        super.render(graphics, mouseX, mouseY, delta);
        graphics.drawCenteredString(this.font, this.title, this.width / 2, 18, AXO_BLUE);
    }

    private Button nudgeButton(AxoConfig config, HudModule hud, String key, int delta, String label, int x, int y) {
        return Button.builder(Component.literal(label), button -> {
            config.setModuleInt(hud.id(), key, config.getModuleInt(hud.id(), key, defaultOffset(hud, key)) + delta);
            hud.reloadPosition();
        }).bounds(x, y, 24, 20).build();
    }

    private static int defaultOffset(HudModule hud, String key) {
        // No config value yet: nudge starts from 0 relative to the anchor.
        return 0;
    }

    private static HudAnchor currentAnchor(AxoConfig config, HudModule hud) {
        return HudAnchor.fromId(
            config.getModuleString(hud.id(), "hud_anchor", HudAnchor.TOP_LEFT.name()),
            HudAnchor.TOP_LEFT
        );
    }

    private static HudAnchor nextAnchor(HudAnchor current) {
        HudAnchor[] all = HudAnchor.values();
        return all[(current.ordinal() + 1) % all.length];
    }

    private static Component anchorLabel(AxoConfig config, HudModule hud) {
        return Component.literal(hud.displayName() + ": " + currentAnchor(config, hud).name());
    }
}
