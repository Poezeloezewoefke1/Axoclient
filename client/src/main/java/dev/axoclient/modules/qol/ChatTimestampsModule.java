package dev.axoclient.modules.qol;

import dev.axoclient.chat.ChatTweaks;
import dev.axoclient.core.AxoModule;
import dev.axoclient.core.ModuleCategory;

/**
 * Puts a dim [HH:mm] in front of every chat line, so you can tell how long
 * ago something was said.
 *
 * The work happens in {@link dev.axoclient.mixin.optional.ChatMixin}; this
 * module exists so the feature has a toggle in the ClickGUI like everything
 * else. It only affects messages that arrive while it is on.
 */
public final class ChatTimestampsModule extends AxoModule {

    public ChatTimestampsModule() {
        super(ChatTweaks.TIMESTAMPS_ID, "Chat Timestamps", ModuleCategory.QOL, false);
    }
}
