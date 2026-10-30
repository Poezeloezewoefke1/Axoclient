package dev.axoclient.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/**
 * Bookkeeping checks over the module sources, so a new module can't quietly
 * miss its registration or its menu description, and two modules can't share
 * an id or a name.
 *
 * Reads source files instead of instantiating modules: several modules touch
 * Minecraft registries in static initialisers, which need a bootstrapped game.
 * Gradle runs tests from the client/ directory.
 */
class ModuleRegistryTest {
    private static final Path SOURCES = Path.of("src/main/java/dev/axoclient");
    /** super("<id>", "<name>" — the first two arguments of every module constructor. */
    private static final Pattern SUPER = Pattern.compile("super\\(\\s*(\"[a-z_]+\"|[A-Za-z_.]+_ID|ID)\\s*,\\s*\"([^\"]+)\"");

    private record Declared(String className, String id, String name) {}

    private static List<Declared> declaredModules() throws IOException {
        String chatTweaks = Files.readString(SOURCES.resolve("chat/ChatTweaks.java"));
        List<Declared> out = new ArrayList<>();
        try (Stream<Path> files = Files.walk(SOURCES.resolve("modules"))) {
            for (Path file : files.filter(p -> p.toString().endsWith("Module.java")).toList()) {
                String source = Files.readString(file);
                Matcher m = SUPER.matcher(source);
                if (!m.find()) {
                    continue;
                }
                String id = resolveId(m.group(1), source, chatTweaks);
                String className = file.getFileName().toString().replace(".java", "");
                out.add(new Declared(className, id, m.group(2)));
            }
        }
        return out;
    }

    /** Literal ids as-is; constant ids looked up in the module itself or ChatTweaks. */
    private static String resolveId(String token, String source, String chatTweaks) {
        if (token.startsWith("\"")) {
            return token.substring(1, token.length() - 1);
        }
        String constant = token.substring(token.lastIndexOf('.') + 1);
        Pattern definition = Pattern.compile(constant + "\\s*=\\s*\"([a-z_]+)\"");
        for (String haystack : List.of(source, chatTweaks)) {
            Matcher m = definition.matcher(haystack);
            if (m.find()) {
                return m.group(1);
            }
        }
        throw new AssertionError("Could not resolve module id constant " + token);
    }

    @Test
    void findsTheModules() throws IOException {
        assertTrue(declaredModules().size() > 50, "source scan found suspiciously few modules");
    }

    @Test
    void everyModuleIsRegistered() throws IOException {
        String entrypoint = Files.readString(SOURCES.resolve("AxoClient.java"));
        for (Declared module : declaredModules()) {
            assertTrue(
                entrypoint.contains("new " + module.className() + "()"),
                module.className() + " is never registered in AxoClient.onInitializeClient()"
            );
        }
    }

    @Test
    void everyModuleHasADescription() throws IOException {
        for (Declared module : declaredModules()) {
            assertFalse(
                ModuleDescriptions.of(module.id()).isEmpty(),
                module.id() + " has no entry in ModuleDescriptions"
            );
        }
    }

    @Test
    void idsAndNamesAreUnique() throws IOException {
        Set<String> ids = new HashSet<>();
        Set<String> names = new HashSet<>();
        for (Declared module : declaredModules()) {
            assertTrue(ids.add(module.id()), "duplicate module id " + module.id());
            assertTrue(names.add(module.name()), "two modules are both called \"" + module.name() + "\"");
        }
        assertEquals(ids.size(), names.size());
    }
}
