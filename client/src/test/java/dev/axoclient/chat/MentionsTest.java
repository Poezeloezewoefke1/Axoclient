package dev.axoclient.chat;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class MentionsTest {

    @Test
    void someoneElseSayingYourName() {
        assertTrue(Mentions.isMention("<Alex> hey Steve, come here", "Steve"));
        assertTrue(Mentions.isMention("<Alex> steve!", "Steve"));
        assertTrue(Mentions.isMention("[VIP] Alex: gg steve", "Steve"));
    }

    @Test
    void yourOwnMessagesDoNotCount() {
        assertFalse(Mentions.isMention("<Steve> hello", "Steve"));
        assertFalse(Mentions.isMention("Steve: hello", "Steve"));
        assertFalse(Mentions.isMention("[VIP] [Global] Steve » hello", "Steve"));
    }

    @Test
    void wholeWordOnly() {
        assertFalse(Mentions.isMention("Stevenson joined the game", "Steve"));
        assertFalse(Mentions.isMention("<Alex> xsteve", "Steve"));
        assertTrue(Mentions.isMention("Steve joined the game", "Steve"));
    }

    @Test
    void namesWithRegexCharactersAreLiteral() {
        assertTrue(Mentions.isMention("<Alex> hi a.b", "a.b"));
        assertFalse(Mentions.isMention("<Alex> hi axb", "a.b"));
    }

    @Test
    void blankInputs() {
        assertFalse(Mentions.isMention(null, "Steve"));
        assertFalse(Mentions.isMention("hi", ""));
        assertFalse(Mentions.isMention("hi", null));
    }
}
