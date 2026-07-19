package dev.axoclient.modules.hud;

import dev.axoclient.hud.HudAnchor;
import dev.axoclient.hud.HudModule;
import dev.axoclient.hud.HudPosition;
import java.util.ArrayDeque;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;

/**
 * Clicks-per-second display (roadmap P1-07). Click edges are sampled per
 * tick from the attack/use bindings; a one-second sliding window decays
 * naturally as timestamps age out.
 */
public final class CpsCounterModule extends HudModule {
    private static final long WINDOW_MS = 1000;

    private final ArrayDeque<Long> leftClicks = new ArrayDeque<>();
    private final ArrayDeque<Long> rightClicks = new ArrayDeque<>();
    private boolean leftWasDown;
    private boolean rightWasDown;

    public CpsCounterModule() {
        super("cps_counter", "CPS Counter", new HudPosition(HudAnchor.TOP_LEFT, 4, 28));
    }

    @Override
    public void onTick() {
        long now = System.currentTimeMillis();
        Options options = Minecraft.getInstance().options;

        boolean left = options.keyAttack.isDown();
        if (left && !leftWasDown) {
            leftClicks.addLast(now);
        }
        leftWasDown = left;

        boolean right = options.keyUse.isDown();
        if (right && !rightWasDown) {
            rightClicks.addLast(now);
        }
        rightWasDown = right;

        prune(leftClicks, now);
        prune(rightClicks, now);
    }

    private static void prune(ArrayDeque<Long> clicks, long now) {
        while (!clicks.isEmpty() && now - clicks.peekFirst() > WINDOW_MS) {
            clicks.pollFirst();
        }
    }

    @Override
    protected String hudText() {
        return leftClicks.size() + " | " + rightClicks.size() + " cps";
    }
}
