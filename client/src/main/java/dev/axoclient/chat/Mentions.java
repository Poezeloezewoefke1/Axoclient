package dev.axoclient.chat;

import java.util.regex.Pattern;

/**
 * Decides whether a chat line mentions you. Pure string logic, no Minecraft
 * types, so it is unit-tested directly.
 *
 * Your name has to appear as a whole word ("Steve" doesn't match
 * "Stevenson"), and lines you sent yourself don't count — they start with
 * your own name in nearly every chat format: "&lt;Steve&gt; hi", "Steve: hi",
 * "[VIP] Steve » hi".
 */
public final class Mentions {
    private Mentions() {
    }

    public static boolean isMention(String text, String name) {
        if (text == null || name == null || name.isBlank()) {
            return false;
        }
        Pattern word = Pattern.compile(
            "(?<![A-Za-z0-9_])" + Pattern.quote(name) + "(?![A-Za-z0-9_])",
            Pattern.CASE_INSENSITIVE
        );
        return word.matcher(text).find() && !isOwnMessage(text, name);
    }

    static boolean isOwnMessage(String text, String name) {
        String rest = text.stripLeading();
        // Skip rank or channel tags such as "[VIP]" or "[Global]".
        while (rest.startsWith("[")) {
            int end = rest.indexOf(']');
            if (end < 0) {
                break;
            }
            rest = rest.substring(end + 1).stripLeading();
        }
        if (rest.startsWith("<")) {
            rest = rest.substring(1);
        }
        if (!rest.regionMatches(true, 0, name, 0, name.length())) {
            return false;
        }
        String after = rest.substring(name.length()).stripLeading();
        return after.startsWith(">") || after.startsWith(":") || after.startsWith("»");
    }
}
