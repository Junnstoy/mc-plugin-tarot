package cn.tarot;

import java.util.List;
import java.util.Locale;
import java.util.function.Predicate;

/** Shared authorization rules for execution, help and tab completion. */
public final class CommandPermissions {
    public static final String ADMIN = "tarot.admin";
    public static final String BYPASS_CD = "tarot.bypass.cd";
    public static final List<String> COMMANDS = List.of(
        "help", "status", "migrate", "album", "collection", "aura", "give", "reset", "selftest");
    private static final List<String> PUBLIC_COMMANDS = List.of(
        "help", "status", "migrate", "album", "collection", "aura");

    private CommandPermissions() {}

    public static String node(TarotCommand.Kind kind) {
        return switch (kind) {
            case RESET_CD -> "tarot.command.reset.cd";
            case RESET_BUFFER -> "tarot.command.reset.buffer";
            default -> "tarot.command." + kind.name().toLowerCase(Locale.ROOT);
        };
    }

    public static boolean allowed(TarotCommand.Kind kind, Predicate<String> permission) {
        if (permission.test(ADMIN) || permission.test(node(kind))) return true;
        return (kind == TarotCommand.Kind.RESET_CD || kind == TarotCommand.Kind.RESET_BUFFER)
            && permission.test("tarot.command.reset");
    }

    /** A reset subcommand grant also exposes the reset entry in help and completion. */
    public static boolean allowed(String command, Predicate<String> permission) {
        if (command == null) return false;
        String normalized = command.toLowerCase(Locale.ROOT);
        if (!COMMANDS.contains(normalized)) return false;
        if (normalized.equals("reset"))
            return allowed(TarotCommand.Kind.RESET_CD, permission)
                || allowed(TarotCommand.Kind.RESET_BUFFER, permission);
        return allowed(TarotCommand.Kind.valueOf(normalized.toUpperCase(Locale.ROOT)), permission);
    }

    /** Compatibility with the older completion API; live callers supply hasPermission. */
    static boolean legacyPermission(String node, boolean admin) {
        return admin && node.equals(ADMIN)
            || PUBLIC_COMMANDS.stream().anyMatch(command -> node.equals("tarot.command." + command));
    }
}
