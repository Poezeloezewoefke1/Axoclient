package dev.axoclient.hud;

import dev.axoclient.core.AxoModule;
import dev.axoclient.core.HudRenderable;
import dev.axoclient.core.ModuleCategory;
import dev.axoclient.core.ModuleManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

/**
 * Base class for single-line HUD text modules. Handles anchored
 * positioning (P1-06); subclasses only supply the text. The position is
 * re-read from config on every enable, so config edits apply on toggle
 * or restart (live drag-editing is P5-09).
 */
public abstract class HudModule extends AxoModule implements HudRenderable {
    protected static final int AXO_BLUE = 0xFF38BDF8;

    private final HudPosition defaultPosition;
    private HudPosition position;

    protected HudModule(String id, String displayName, HudPosition defaultPosition) {
        this(id, displayName, ModuleCategory.HUD, defaultPosition, true);
    }

    protected HudModule(String id, String displayName, HudPosition defaultPosition, boolean enabledByDefault) {
        this(id, displayName, ModuleCategory.HUD, defaultPosition, enabledByDefault);
    }

    protected HudModule(String id, String displayName, ModuleCategory category, HudPosition defaultPosition) {
        this(id, displayName, category, defaultPosition, true);
    }

    protected HudModule(
        String id,
        String displayName,
        ModuleCategory category,
        HudPosition defaultPosition,
        boolean enabledByDefault
    ) {
        super(id, displayName, category, enabledByDefault);
        this.defaultPosition = defaultPosition;
        this.position = defaultPosition;
    }

    /** The line to draw this frame, or null to draw nothing. */
    protected abstract String hudText();

    protected final HudPosition position() {
        return position;
    }

    /** Current placement — read by the HUD editor to draw the drag handle. */
    public final HudPosition currentPosition() {
        return position;
    }

    /** Non-null preview label for the HUD editor (falls back to the module name). */
    public String editorLabel() {
        String text = hudText();
        return text != null ? text : displayName();
    }

    /** Applies a new placement live and persists it (used by the HUD editor). */
    public final void moveTo(HudPosition next) {
        this.position = next;
        next.save(ModuleManager.get().config(), id());
    }

    /** Restores the built-in default placement (HUD editor "reset"). */
    public final void resetPosition() {
        moveTo(defaultPosition);
    }

    /** Re-reads hud_anchor/hud_x/hud_y from config (used by the HUD layout screen). */
    public final void reloadPosition() {
        position = HudPosition.load(ModuleManager.get().config(), id(), defaultPosition);
    }

    @Override
    protected void onEnable() {
        reloadPosition();
    }

    @Override
    public void renderHud(GuiGraphics graphics) {
        String text = hudText();
        if (text == null) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        int x = position.anchor().x(graphics.guiWidth(), minecraft.font.width(text), position.offsetX());
        int y = position.anchor().y(graphics.guiHeight(), minecraft.font.lineHeight, position.offsetY());
        graphics.drawString(minecraft.font, text, x, y, AXO_BLUE, true);
    }
}
