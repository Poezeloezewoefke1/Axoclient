package dev.axoclient.modules.pvp;

import dev.axoclient.core.ModuleCategory;
import dev.axoclient.core.ModuleSetting;
import dev.axoclient.hud.HudAnchor;
import dev.axoclient.hud.HudModule;
import dev.axoclient.hud.HudPosition;
import java.util.ArrayDeque;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

/**
 * WASD + mouse-button display (roadmap P1-09). Pure input visualization —
 * firmly on the fair-play side of the feature line (risk R6).
 *
 * Config (module "keystrokes"):
 *   mouse — "LMB/RMB" labels always, or clicks-per-second while clicking
 *   space — add a space-bar row (On / Off)
 */
public final class KeystrokesModule extends HudModule {
    private static final int CELL = 18;
    private static final int GAP = 2;
    private static final int PRESSED_BG = 0xE038BDF8;
    private static final int IDLE_BG = 0x90101018;
    private static final int PRESSED_TEXT = 0xFF0A0A0F;
    private static final int IDLE_TEXT = 0xFFE7E9EE;
    private static final long CPS_WINDOW_MS = 1000;

    private final ArrayDeque<Long> leftClicks = new ArrayDeque<>();
    private final ArrayDeque<Long> rightClicks = new ArrayDeque<>();
    private boolean leftWasDown;
    private boolean rightWasDown;

    public KeystrokesModule() {
        super("keystrokes", "Keystrokes", ModuleCategory.PVP, new HudPosition(HudAnchor.MIDDLE_LEFT, 8, 0));
    }

    @Override
    public List<ModuleSetting> settings() {
        return List.of(
            ModuleSetting.choice(id(), "mouse", "Mouse buttons", List.of("Labels", "CPS while clicking"), 1),
            ModuleSetting.choice(id(), "space", "Space bar", List.of("Off", "On"), 0)
        );
    }

    @Override
    public void onTick() {
        Options options = Minecraft.getInstance().options;
        long now = System.currentTimeMillis();
        boolean left = options.keyAttack.isDown();
        boolean right = options.keyUse.isDown();
        if (left && !leftWasDown) {
            leftClicks.addLast(now);
        }
        if (right && !rightWasDown) {
            rightClicks.addLast(now);
        }
        leftWasDown = left;
        rightWasDown = right;
        while (!leftClicks.isEmpty() && now - leftClicks.peekFirst() > CPS_WINDOW_MS) {
            leftClicks.pollFirst();
        }
        while (!rightClicks.isEmpty() && now - rightClicks.peekFirst() > CPS_WINDOW_MS) {
            rightClicks.pollFirst();
        }
    }

    @Override
    public void renderHud(GuiGraphics graphics) {
        Options options = Minecraft.getInstance().options;
        boolean space = settings().get(1).get() == 1;
        int width = CELL * 3 + GAP * 2;
        int height = CELL * (space ? 4 : 3) + GAP * (space ? 3 : 2);
        int x = position().anchor().x(graphics.guiWidth(), width, position().offsetX());
        int y = position().anchor().y(graphics.guiHeight(), height, position().offsetY());

        drawKey(graphics, x + CELL + GAP, y, CELL, "W", options.keyUp.isDown());
        drawKey(graphics, x, y + CELL + GAP, CELL, "A", options.keyLeft.isDown());
        drawKey(graphics, x + CELL + GAP, y + CELL + GAP, CELL, "S", options.keyDown.isDown());
        drawKey(graphics, x + (CELL + GAP) * 2, y + CELL + GAP, CELL, "D", options.keyRight.isDown());

        int mouseWidth = (width - GAP) / 2;
        int mouseY = y + (CELL + GAP) * 2;
        boolean cps = settings().get(0).get() == 1;
        drawKey(graphics, x, mouseY, mouseWidth, mouseLabel("LMB", leftClicks.size(), cps), options.keyAttack.isDown());
        drawKey(
            graphics, x + mouseWidth + GAP, mouseY, mouseWidth,
            mouseLabel("RMB", rightClicks.size(), cps), options.keyUse.isDown()
        );
        if (space) {
            drawKey(graphics, x, mouseY + CELL + GAP, width, "SPACE", options.keyJump.isDown());
        }
    }

    /** Just the number while clicking: "10 CPS" doesn't fit a half-width key at GUI scale. */
    private static String mouseLabel(String name, int clicks, boolean cps) {
        return cps && clicks > 0 ? Integer.toString(clicks) : name;
    }

    private void drawKey(GuiGraphics graphics, int x, int y, int width, String label, boolean down) {
        graphics.fill(x, y, x + width, y + CELL, down ? PRESSED_BG : IDLE_BG);
        Font font = Minecraft.getInstance().font;
        int textX = x + (width - font.width(label)) / 2;
        int textY = y + (CELL - font.lineHeight) / 2 + 1;
        graphics.drawString(font, label, textX, textY, down ? PRESSED_TEXT : IDLE_TEXT, false);
    }

    @Override
    protected String hudText() {
        return null; // fully custom rendering above
    }
}
