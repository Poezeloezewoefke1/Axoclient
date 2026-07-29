package dev.axoclient.modules.qol;

import dev.axoclient.chat.ChatTweaks;
import dev.axoclient.core.AxoModule;
import dev.axoclient.core.ModuleCategory;

/**
 * Hides a chat line when it is an exact repeat of the one before it and
 * arrives within about a second — the usual shape of server spam.
 *
 * Deliberately strict: only identical text inside a short window is dropped.
 * Anything fuzzier risks eating a message you actually wanted.
 */
public final class ChatAntiSpamModule extends AxoModule {

    public ChatAntiSpamModule() {
        super(ChatTweaks.ANTI_SPAM_ID, "Chat Anti-Spam", ModuleCategory.QOL, false);
    }
}
