package dev.axoclient.modules.qol;

import dev.axoclient.AxoClient;
import dev.axoclient.core.AxoModule;
import dev.axoclient.core.ModuleCategory;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;

/**
 * Keeps the messages you've sent, so pressing Up in chat still finds them
 * after a server switch or a restart.
 *
 * Vanilla wipes the sent-message history whenever you leave a world. Nothing
 * about fixing that needs a mixin — {@code ChatComponent.getRecentChat()} and
 * {@code addRecentChat(String)} are both public — so this is a plain polling
 * module: it notices the history growing, saves it, and restores it the next
 * time chat comes up empty.
 */
public final class ChatHistoryModule extends AxoModule {
    private static final int MAX_ENTRIES = 100;
    /** Every 5s. Saving is cheap but there's no reason to hit disk per tick. */
    private static final int SAVE_INTERVAL_TICKS = 100;

    private final List<String> history = new ArrayList<>();
    private boolean loaded;
    private int tick;

    public ChatHistoryModule() {
        super("chat_history", "Chat History", ModuleCategory.QOL, false);
    }

    private static Path historyFile() {
        return Minecraft.getInstance().gameDirectory.toPath()
            .resolve("axoclient")
            .resolve("chat-history.txt");
    }

    @Override
    protected void onEnable() {
        if (!loaded) {
            history.clear();
            history.addAll(read());
            loaded = true;
        }
    }

    @Override
    public void onTick() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.gui == null || minecraft.player == null) {
            return;
        }
        List<String> recent = minecraft.gui.getChat().getRecentChat();

        // Vanilla cleared it (world change) but we still have entries: put ours back.
        if (recent.isEmpty() && !history.isEmpty()) {
            // getRecentChat is newest-first, and addRecentChat prepends, so
            // replaying oldest-first restores the original order.
            for (int i = history.size() - 1; i >= 0; i--) {
                minecraft.gui.getChat().addRecentChat(history.get(i));
            }
            return;
        }

        // Otherwise track what vanilla has and persist it periodically.
        if (recent.size() > history.size()) {
            history.clear();
            history.addAll(recent.subList(0, Math.min(recent.size(), MAX_ENTRIES)));
        }
        if (++tick % SAVE_INTERVAL_TICKS == 0 && !history.isEmpty()) {
            write();
        }
    }

    @Override
    protected void onDisable() {
        if (!history.isEmpty()) {
            write();
        }
    }

    private static List<String> read() {
        Path file = historyFile();
        try {
            if (!Files.isRegularFile(file)) {
                return List.of();
            }
            List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
            return lines.size() > MAX_ENTRIES ? lines.subList(0, MAX_ENTRIES) : lines;
        } catch (IOException e) {
            AxoClient.LOGGER.warn("Could not read chat history", e);
            return List.of();
        }
    }

    /** Atomic write — a crash mid-save must not leave a truncated file. */
    private void write() {
        Path file = historyFile();
        try {
            Files.createDirectories(file.getParent());
            Path temp = file.resolveSibling("chat-history.txt.tmp");
            Files.write(temp, history, StandardCharsets.UTF_8);
            Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            AxoClient.LOGGER.warn("Could not save chat history", e);
        }
    }
}
