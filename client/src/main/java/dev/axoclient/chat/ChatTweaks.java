package dev.axoclient.chat;

import dev.axoclient.core.ModuleManager;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.sounds.SoundEvents;

/**
 * The logic behind the chat mixin, kept out of the mixin itself.
 *
 * Mixin classes are awkward to reason about and impossible to unit-test, so
 * {@link dev.axoclient.mixin.optional.ChatMixin} stays a two-line shim and
 * everything real lives here.
 */
public final class ChatTweaks {
    public static final String TIMESTAMPS_ID = "chat_timestamps";
    public static final String ANTI_SPAM_ID = "chat_anti_spam";
    public static final String MENTIONS_ID = "chat_mentions";

    private static final DateTimeFormatter CLOCK = DateTimeFormatter.ofPattern("HH:mm");
    /** Duplicate window. Long enough to catch spam, short enough that a repeated "gg" still shows. */
    private static final long DUPLICATE_WINDOW_MS = 1200;

    private static String lastMessage = "";
    private static long lastMessageAt;

    private ChatTweaks() {
    }

    private static boolean enabled(String moduleId) {
        return ModuleManager.get().byId(moduleId).map(m -> ModuleManager.get().isEnabled(m)).orElse(false);
    }

    /**
     * Marks a line that mentions you (when Chat Mentions is on), then prefixes
     * a dim [HH:mm] (when timestamps are on). Either can be off independently.
     */
    public static Component decorate(Component message) {
        if (message == null) {
            return null;
        }
        Component out = markMention(message);
        if (!enabled(TIMESTAMPS_ID)) {
            return out;
        }
        MutableComponent stamp = Component.literal("[" + LocalTime.now().format(CLOCK) + "] ")
            .setStyle(Style.EMPTY.withColor(TextColor.fromRgb(0x8A8A8A)));
        return stamp.append(out);
    }

    /** A gold "» " in front of lines that say your name, plus a ping unless muted. See {@link Mentions}. */
    private static Component markMention(Component message) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null || !enabled(MENTIONS_ID)
            || !Mentions.isMention(message.getString(), player.getName().getString())) {
            return message;
        }
        if (ModuleManager.get().config().getModuleInt(MENTIONS_ID, "sound", 0) == 0) {
            Minecraft.getInstance().getSoundManager().play(
                SimpleSoundInstance.forUI(SoundEvents.EXPERIENCE_ORB_PICKUP, 1.6F)
            );
        }
        return Component.literal("» ")
            .setStyle(Style.EMPTY.withColor(TextColor.fromRgb(0xFBBF24)))
            .append(message);
    }

    /**
     * True when this message should be dropped as spam.
     *
     * Only exact repeats inside a short window are suppressed — nothing that
     * merely looks similar — so this can never eat a message you needed.
     */
    public static boolean shouldSuppress(Component message) {
        if (message == null || !enabled(ANTI_SPAM_ID)) {
            return false;
        }
        String text = message.getString();
        long now = System.currentTimeMillis();
        boolean duplicate = text.equals(lastMessage) && (now - lastMessageAt) < DUPLICATE_WINDOW_MS;
        lastMessage = text;
        lastMessageAt = now;
        return duplicate;
    }
}
