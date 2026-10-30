package dev.axoclient.modules.qol;

import dev.axoclient.chat.ChatTweaks;
import dev.axoclient.core.AxoModule;
import dev.axoclient.core.ModuleCategory;
import dev.axoclient.core.ModuleSetting;
import java.util.List;

/**
 * Highlights chat lines that say your name with a gold marker and a short
 * ping, so a busy server chat can't bury someone talking to you. Your own
 * messages don't count. The work happens in {@link ChatTweaks}, through the
 * same optional chat hook as timestamps — if that hook ever fails to apply,
 * this quietly does nothing and the game still starts.
 *
 * Config (module "chat_mentions"):
 *   sound — play a ping on a mention (On / Off)
 */
public final class ChatMentionsModule extends AxoModule {

    public ChatMentionsModule() {
        super(ChatTweaks.MENTIONS_ID, "Chat Mentions", ModuleCategory.QOL, false);
    }

    @Override
    public List<ModuleSetting> settings() {
        return List.of(ModuleSetting.choice(id(), "sound", "Ping sound", List.of("On", "Off"), 0));
    }
}
