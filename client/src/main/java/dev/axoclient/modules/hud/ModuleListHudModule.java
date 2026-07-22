package dev.axoclient.modules.hud;

import dev.axoclient.core.AxoModule;
import dev.axoclient.core.HudRenderable;
import dev.axoclient.core.ModuleCategory;
import dev.axoclient.core.ModuleManager;
import dev.axoclient.gui.theme.Themes;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

/**
 * The classic "array list": every enabled module stacked in the top-right,
 * longest name first, each tagged with the accent colour. Off by default so
 * it never surprises anyone. Renders itself rather than extending
 * {@link HudModule} because it draws a multi-line list, not a single line.
 */
public final class ModuleListHudModule extends AxoModule implements HudRenderable {

    public ModuleListHudModule() {
        super("module_list", "Module List", ModuleCategory.HUD, false);
    }

    @Override
    public void renderHud(GuiGraphics graphics) {
        Font font = Minecraft.getInstance().font;
        List<String> names = new ArrayList<>();
        for (AxoModule module : ModuleManager.get().all()) {
            if (module != this && ModuleManager.get().isEnabled(module)) {
                names.add(module.displayName());
            }
        }
        if (names.isEmpty()) {
            return;
        }
        names.sort((a, b) -> Integer.compare(font.width(b), font.width(a)));

        int accent = Themes.current().accent;
        int screenWidth = graphics.guiWidth();
        int y = 2;
        for (String name : names) {
            int width = font.width(name);
            int x = screenWidth - width - 3;
            graphics.fill(x - 2, y, screenWidth, y + font.lineHeight + 1, 0x66000000);
            graphics.fill(x - 2, y, x - 1, y + font.lineHeight + 1, accent);
            graphics.drawString(font, name, x, y + 1, accent, true);
            y += font.lineHeight + 2;
        }
    }
}
