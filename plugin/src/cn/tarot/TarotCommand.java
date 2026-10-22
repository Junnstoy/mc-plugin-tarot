package cn.tarot;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Predicate;

/** Parses commands before any inventory or persistent-state changes occur. */
public final class TarotCommand {
    private TarotCommand() {}

    public enum Kind {
        HELP, STATUS, MIGRATE, ALBUM, COLLECTION, AURA, SELFTEST, GIVE, RESET_CD, RESET_BUFFER
    }

    /** For GIVE card, card 0 means all 22 kinds and count applies to each kind. */
    public record Request(Kind kind, String player, String item, int card, int level, int count) {}

    private static final List<String> ITEMS = List.of("card", "pack", "box", "case", "album");
    private static final String CARD_USAGE =
        "用法：/tarot give <玩家> card <卡牌编号/完整名称/all> <等级1-5> <数量1-64>";
    private static final String GIVE_USAGE =
        "用法：/tarot give <玩家> card <卡牌编号/完整名称/all> <等级1-5> <数量1-64>；"
        + "或 /tarot give <玩家> <pack|box|case|album> <数量>";
    private static final String RESET_USAGE = "用法：/tarot reset <cd|buffer> <玩家>";

    /** cardNames contains the 22 names in card-ID order, starting at ID 1. */
    public static Request parse(String[] args, List<String> cardNames) {
        checkNames(cardNames);
        if (args == null) throw new IllegalArgumentException("指令参数不能为空。");
        for (String arg : args) {
            if (arg == null || arg.isBlank() || arg.codePoints().anyMatch(Character::isWhitespace))
                throw new IllegalArgumentException("指令参数不能为空或包含空格；使用 /tarot help 查看用法。");
        }
        if (args.length == 0) return simple(Kind.HELP);
        String command = lower(args[0]);
        return switch (command) {
            case "give" -> parseGive(args, cardNames);
            case "reset" -> parseReset(args);
            case "card" -> throw new IllegalArgumentException(
                "card 已移入 give。" + CARD_USAGE);
            case "clear" -> throw new IllegalArgumentException(
                "clear 已改为 /tarot reset cd <玩家>（重置换卡冷却）和 "
                + "/tarot reset buffer <玩家>（移除塔罗牌效果）。");
            case "help", "status", "migrate", "album", "collection", "aura", "selftest" -> {
                if (args.length != 1)
                    throw new IllegalArgumentException("用法：/tarot " + command + "；此指令不接受额外参数。");
                yield simple(Kind.valueOf(command.toUpperCase(Locale.ROOT)));
            }
            default -> throw new IllegalArgumentException("未知指令：" + args[0] + "。使用 /tarot help 查看用法。");
        };
    }

    private static Request parseGive(String[] args, List<String> cardNames) {
        if (args.length < 3) throw new IllegalArgumentException(GIVE_USAGE);
        String item = lower(args[2]);
        if (!ITEMS.contains(item))
            throw new IllegalArgumentException("物品类型必须为 card、pack、box、case 或 album。" + GIVE_USAGE);
        if (item.equals("card")) {
            if (args.length != 6) throw new IllegalArgumentException(CARD_USAGE);
            int card = cardId(args[3], cardNames);
            int level = number(args[4], 1, 5, "卡牌等级");
            int count = number(args[5], 1, 64, "数量");
            return new Request(Kind.GIVE, args[1], item, card, level, count);
        }
        if (args.length != 4)
            throw new IllegalArgumentException("用法：/tarot give <玩家> " + item
                + (item.equals("album") ? " 1" : " <数量1-64>"));
        int count = number(args[3], 1, 64, "数量");
        if (item.equals("album") && count != 1)
            throw new IllegalArgumentException("收集册为随身唯一物品，每次只能发放 1 本，多余收集册会自动移除。");
        return new Request(Kind.GIVE, args[1], item, 0, 0, count);
    }

    private static Request parseReset(String[] args) {
        if (args.length != 3) throw new IllegalArgumentException(RESET_USAGE);
        Kind kind = switch (lower(args[1])) {
            case "cd" -> Kind.RESET_CD;
            case "buffer" -> Kind.RESET_BUFFER;
            default -> throw new IllegalArgumentException("重置类型必须为 cd 或 buffer。" + RESET_USAGE);
        };
        return new Request(kind, args[2], null, 0, 0, 0);
    }

    private static Request simple(Kind kind) { return new Request(kind, null, null, 0, 0, 0); }

    private static int cardId(String value, List<String> names) {
        if (value.equalsIgnoreCase("all")) return 0;
        int named = names.indexOf(value);
        if (named >= 0) return named + 1;
        try {
            return number(value, 1, 22, "卡牌编号");
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("卡牌种类必须为 1-22 的编号、完整中文名称或 all。all 会给每种卡各指定数量。");
        }
    }

    private static int number(String value, int min, int max, String label) {
        int parsed;
        try {
            if (value.isEmpty() || !value.chars().allMatch(ch -> ch >= '0' && ch <= '9'))
                throw new NumberFormatException();
            parsed = Integer.parseInt(value);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(label + "必须为 " + min + "-" + max + " 的整数。");
        }
        if (parsed < min || parsed > max)
            throw new IllegalArgumentException(label + "必须为 " + min + "-" + max + " 的整数。");
        return parsed;
    }

    /** Only offers arguments in their actual positions and filters by the current prefix. */
    public static List<String> complete(String[] args, List<String> cardNames,
                                        List<String> players, boolean admin) {
        return complete(args, cardNames, players, node -> CommandPermissions.legacyPermission(node, admin));
    }

    public static List<String> complete(String[] args, List<String> cardNames,
                                        List<String> players, Predicate<String> permission) {
        checkNames(cardNames);
        List<String> commands = CommandPermissions.COMMANDS.stream()
            .filter(command -> CommandPermissions.allowed(command, permission)).toList();
        if (args == null || args.length == 0) return commands;
        for (String arg : args) if (arg == null) return List.of();
        List<String> candidates = List.of();
        if (args.length == 1) candidates = commands;
        else if (CommandPermissions.allowed(Kind.GIVE, permission) && args[0].equalsIgnoreCase("give")) {
            if (args.length == 2) candidates = players;
            else if (args.length == 3) candidates = ITEMS;
            else if (args[2].equalsIgnoreCase("card")) {
                if (args.length == 4) {
                    List<String> kinds = new ArrayList<>();
                    kinds.add("all");
                    for (int id = 1; id <= 22; id++) kinds.add(Integer.toString(id));
                    kinds.addAll(cardNames);
                    candidates = kinds;
                } else if (args.length == 5) candidates = List.of("1", "2", "3", "4", "5");
                else if (args.length == 6) candidates = List.of("1", "2", "8", "16", "32", "64");
            } else if (args.length == 4 && ITEMS.contains(lower(args[2]))) {
                candidates = args[2].equalsIgnoreCase("album") ? List.of("1")
                    : List.of("1", "8", "16", "32", "48", "64");
            }
        } else if (args[0].equalsIgnoreCase("reset")) {
            List<String> resets = new ArrayList<>();
            if (CommandPermissions.allowed(Kind.RESET_CD, permission)) resets.add("cd");
            if (CommandPermissions.allowed(Kind.RESET_BUFFER, permission)) resets.add("buffer");
            if (args.length == 2) candidates = resets;
            else if (args.length == 3 && resets.contains(lower(args[1]))) candidates = players;
        }
        String prefix = lower(args[args.length - 1]);
        return candidates.stream().filter(s -> lower(s).startsWith(prefix)).distinct().toList();
    }

    private static String lower(String value) { return value.toLowerCase(Locale.ROOT); }

    private static void checkNames(List<String> names) {
        if (names == null || names.size() != 22 || names.stream().anyMatch(s -> s == null || s.isBlank()))
            throw new IllegalArgumentException("卡牌名称表必须包含按编号排列的 22 个名称。");
    }
}
