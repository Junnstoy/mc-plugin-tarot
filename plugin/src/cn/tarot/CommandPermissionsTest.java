package cn.tarot;

import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

public final class CommandPermissionsTest {
    private static int checks;
    private static void check(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError(message);
    }

    private static Predicate<String> grants(String... nodes) {
        Set<String> assigned = Set.of(nodes);
        return assigned::contains;
    }

    public static void main(String[] ignored) {
        for (TarotCommand.Kind kind : TarotCommand.Kind.values()) {
            check(!CommandPermissions.allowed(kind, grants()), "no grant: " + kind);
            check(CommandPermissions.allowed(kind, grants(CommandPermissions.ADMIN)), "admin: " + kind);
            check(CommandPermissions.allowed(kind, grants(CommandPermissions.node(kind))), "individual: " + kind);
            check(!CommandPermissions.allowed(kind, grants(CommandPermissions.BYPASS_CD)), "bypass is not admin: " + kind);
            for (TarotCommand.Kind other : TarotCommand.Kind.values()) {
                if (kind != other)
                    check(!CommandPermissions.allowed(other, grants(CommandPermissions.node(kind))),
                        kind + " should not authorize " + other);
            }
        }
        check(CommandPermissions.allowed(TarotCommand.Kind.RESET_CD, grants("tarot.command.reset")), "parent grants cd");
        check(CommandPermissions.allowed(TarotCommand.Kind.RESET_BUFFER, grants("tarot.command.reset")), "parent grants buffer");
        check(!CommandPermissions.allowed(TarotCommand.Kind.GIVE, grants("tarot.command.reset")), "reset does not grant give");
        check(CommandPermissions.allowed("reset", grants("tarot.command.reset.cd")), "cd exposes reset entry");
        check(CommandPermissions.allowed("reset", grants("tarot.command.reset.buffer")), "buffer exposes reset entry");
        check(!CommandPermissions.allowed("reset", grants("tarot.command.status")), "status cannot reset");
        check(!CommandPermissions.allowed("unknown", grants(CommandPermissions.ADMIN)), "unknown command remains invalid");
        check(!CommandPermissions.allowed((String)null, grants(CommandPermissions.ADMIN)), "null remains invalid");
        for (String command : CommandPermissions.COMMANDS) {
            check(CommandPermissions.allowed(command.toUpperCase(java.util.Locale.ROOT),
                grants(CommandPermissions.ADMIN)), "case insensitive: " + command);
        }
        for (String publicCommand : List.of("help", "status", "migrate", "album", "collection", "aura")) {
            check(CommandPermissions.allowed(publicCommand,
                node -> CommandPermissions.legacyPermission(node, false)), "legacy default: " + publicCommand);
        }
        check(!CommandPermissions.legacyPermission(CommandPermissions.BYPASS_CD, true), "admin does not imply bypass");
        check(!CommandPermissions.legacyPermission(CommandPermissions.BYPASS_CD, false), "public does not imply bypass");
        System.out.println("COMMAND PERMISSIONS TEST PASS: " + checks
            + " checks; command-specific grants, reset children, admin override and separate cooldown bypass");
    }
}
